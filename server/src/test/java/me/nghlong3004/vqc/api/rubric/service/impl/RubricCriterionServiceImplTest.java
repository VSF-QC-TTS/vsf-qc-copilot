package me.nghlong3004.vqc.api.rubric.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.rubric.entity.Rubric;
import me.nghlong3004.vqc.api.rubric.entity.RubricCriterion;
import me.nghlong3004.vqc.api.rubric.entity.RubricVersion;
import me.nghlong3004.vqc.api.rubric.enums.RubricStatus;
import me.nghlong3004.vqc.api.rubric.enums.RubricVersionStatus;
import me.nghlong3004.vqc.api.rubric.mapper.RubricMapper;
import me.nghlong3004.vqc.api.rubric.repository.RubricCriterionRepository;
import me.nghlong3004.vqc.api.rubric.repository.RubricVersionRepository;
import me.nghlong3004.vqc.api.rubric.request.CreateRubricCriterionRequest;
import me.nghlong3004.vqc.api.rubric.request.UpdateRubricCriterionRequest;
import me.nghlong3004.vqc.api.rubric.response.RubricCriterionResponse;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class RubricCriterionServiceImplTest {

  @Test
  void createCriterionPersistsDraftCriterionWithDefaults() {
    User creator = user();
    RubricVersion version = version(creator, RubricVersionStatus.DRAFT);
    AtomicReference<RubricCriterion> savedCriterion = new AtomicReference<>();
    RubricCriterionServiceImpl service =
        service(
            criterionRepository(
                savedCriterion,
                false,
                false,
                Optional.empty(),
                new AtomicReference<>(),
                new AtomicReference<>()),
            versionRepository(Optional.of(version)),
            userRepository(Optional.of(creator)));

    RubricCriterionResponse response =
        service.createCriterion(
            version.getPublicId(),
            new CreateRubricCriterionRequest(
                "  Correctness  ",
                "  Checks factual match.  ",
                new BigDecimal("0.4000"),
                "  Facts match.  ",
                "  Facts are wrong.  ",
                "  Compare with ground truth.  ",
                "correctness",
                null,
                null),
            "qc.demo@example.com");

    assertThat(savedCriterion.get().getRubricVersion()).isSameAs(version);
    assertThat(savedCriterion.get().getName()).isEqualTo("Correctness");
    assertThat(savedCriterion.get().getDescription()).isEqualTo("Checks factual match.");
    assertThat(savedCriterion.get().getWeight()).isEqualByComparingTo("0.4000");
    assertThat(savedCriterion.get().getPassCondition()).isEqualTo("Facts match.");
    assertThat(savedCriterion.get().getFailCondition()).isEqualTo("Facts are wrong.");
    assertThat(savedCriterion.get().getJudgeInstruction()).isEqualTo("Compare with ground truth.");
    assertThat(savedCriterion.get().getMetricKey()).isEqualTo("correctness");
    assertThat(savedCriterion.get().getCritical()).isFalse();
    assertThat(savedCriterion.get().getSortOrder()).isZero();
    assertThat(response.metricKey()).isEqualTo("correctness");
  }

  @Test
  void createCriterionRejectsDuplicateMetricKey() {
    User creator = user();
    RubricVersion version = version(creator, RubricVersionStatus.DRAFT);
    RubricCriterionServiceImpl service =
        service(
            criterionRepository(
                new AtomicReference<>(),
                true,
                false,
                Optional.empty(),
                new AtomicReference<>(),
                new AtomicReference<>()),
            versionRepository(Optional.of(version)),
            userRepository(Optional.of(creator)));

    assertThatThrownBy(
            () ->
                service.createCriterion(
                    version.getPublicId(),
                    new CreateRubricCriterionRequest(
                        "Correctness",
                        null,
                        new BigDecimal("0.4000"),
                        null,
                        null,
                        "Judge correctness.",
                        "correctness",
                        true,
                        1),
                    "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(error -> ((ResourceException) error).getResponse().code())
        .isEqualTo("RUBRIC_CRITERION_METRIC_KEY_CONFLICT");
  }

  @Test
  void createCriterionRejectsPublishedVersion() {
    User creator = user();
    RubricVersion version = version(creator, RubricVersionStatus.PUBLISHED);
    RubricCriterionServiceImpl service =
        service(
            ignoredCriterionRepository(),
            versionRepository(Optional.of(version)),
            userRepository(Optional.of(creator)));

    assertThatThrownBy(
            () ->
                service.createCriterion(
                    version.getPublicId(),
                    new CreateRubricCriterionRequest(
                        "Correctness",
                        null,
                        new BigDecimal("0.4000"),
                        null,
                        null,
                        "Judge correctness.",
                        "correctness",
                        true,
                        1),
                    "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(error -> ((ResourceException) error).getResponse().code())
        .isEqualTo("RUBRIC_VERSION_IMMUTABLE");
  }

  @Test
  void updateCriterionChangesEditableFields() {
    User creator = user();
    RubricVersion version = version(creator, RubricVersionStatus.DRAFT);
    RubricCriterion criterion = criterion(version);
    AtomicReference<RubricCriterion> savedCriterion = new AtomicReference<>();
    RubricCriterionServiceImpl service =
        service(
            criterionRepository(
                savedCriterion,
                false,
                false,
                Optional.of(criterion),
                new AtomicReference<>(),
                new AtomicReference<>()),
            ignoredVersionRepository(),
            userRepository(Optional.of(creator)));

    service.updateCriterion(
        criterion.getPublicId(),
        new UpdateRubricCriterionRequest(
            "  Safety  ",
            "Updated",
            new BigDecimal("0.6000"),
            null,
            null,
            "  Judge safety.  ",
            "safety",
            false,
            2),
        "qc.demo@example.com");

    assertThat(savedCriterion.get()).isSameAs(criterion);
    assertThat(criterion.getName()).isEqualTo("Safety");
    assertThat(criterion.getDescription()).isEqualTo("Updated");
    assertThat(criterion.getWeight()).isEqualByComparingTo("0.6000");
    assertThat(criterion.getJudgeInstruction()).isEqualTo("Judge safety.");
    assertThat(criterion.getMetricKey()).isEqualTo("safety");
    assertThat(criterion.getCritical()).isFalse();
    assertThat(criterion.getSortOrder()).isEqualTo(2);
  }

  @Test
  void updateCriterionRejectsMetricKeyConflict() {
    User creator = user();
    RubricVersion version = version(creator, RubricVersionStatus.DRAFT);
    RubricCriterion criterion = criterion(version);
    RubricCriterionServiceImpl service =
        service(
            criterionRepository(
                new AtomicReference<>(),
                false,
                true,
                Optional.of(criterion),
                new AtomicReference<>(),
                new AtomicReference<>()),
            ignoredVersionRepository(),
            userRepository(Optional.of(creator)));

    assertThatThrownBy(
            () ->
                service.updateCriterion(
                    criterion.getPublicId(),
                    new UpdateRubricCriterionRequest(null, null, null, null, null, null, "safety", null, null),
                    "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(error -> ((ResourceException) error).getResponse().code())
        .isEqualTo("RUBRIC_CRITERION_METRIC_KEY_CONFLICT");
  }

  @Test
  void deleteCriterionDeletesDraftCriterion() {
    User creator = user();
    RubricVersion version = version(creator, RubricVersionStatus.DRAFT);
    RubricCriterion criterion = criterion(version);
    AtomicReference<RubricCriterion> deletedCriterion = new AtomicReference<>();
    RubricCriterionServiceImpl service =
        service(
            criterionRepository(
                new AtomicReference<>(),
                false,
                false,
                Optional.of(criterion),
                deletedCriterion,
                new AtomicReference<>()),
            ignoredVersionRepository(),
            userRepository(Optional.of(creator)));

    service.deleteCriterion(criterion.getPublicId(), "qc.demo@example.com");

    assertThat(deletedCriterion.get()).isSameAs(criterion);
  }

  private RubricCriterionServiceImpl service(
      RubricCriterionRepository criterionRepository,
      RubricVersionRepository versionRepository,
      UserRepository userRepository) {
    return new RubricCriterionServiceImpl(
        criterionRepository, versionRepository, userRepository, new RubricMapper());
  }

  private RubricCriterionRepository criterionRepository(
      AtomicReference<RubricCriterion> savedCriterion,
      boolean createMetricKeyExists,
      boolean updateMetricKeyExists,
      Optional<RubricCriterion> foundCriterion,
      AtomicReference<RubricCriterion> deletedCriterion,
      AtomicReference<RubricVersion> listedVersion) {
    return (RubricCriterionRepository)
        Proxy.newProxyInstance(
            RubricCriterionRepository.class.getClassLoader(),
            new Class<?>[] {RubricCriterionRepository.class},
            (proxy, method, args) -> {
              if ("save".equals(method.getName())) {
                savedCriterion.set((RubricCriterion) args[0]);
                return args[0];
              }
              if ("existsByRubricVersionAndMetricKey".equals(method.getName())) {
                return createMetricKeyExists;
              }
              if ("existsByRubricVersionAndMetricKeyAndPublicIdNot".equals(method.getName())) {
                return updateMetricKeyExists;
              }
              if ("findByPublicIdAndRubricVersionRubricCreatedBy".equals(method.getName())) {
                return foundCriterion;
              }
              if ("findByRubricVersion".equals(method.getName())) {
                listedVersion.set((RubricVersion) args[0]);
                return new PageImpl<>(foundCriterion.stream().toList(), (PageRequest) args[1], foundCriterion.isPresent() ? 1 : 0);
              }
              if ("delete".equals(method.getName())) {
                deletedCriterion.set((RubricCriterion) args[0]);
                return null;
              }
              if ("toString".equals(method.getName())) {
                return "RubricCriterionRepositoryTestDouble";
              }
              return null;
            });
  }

  private RubricCriterionRepository ignoredCriterionRepository() {
    return criterionRepository(
        new AtomicReference<>(),
        false,
        false,
        Optional.empty(),
        new AtomicReference<>(),
        new AtomicReference<>());
  }

  private RubricVersionRepository versionRepository(Optional<RubricVersion> foundVersion) {
    return (RubricVersionRepository)
        Proxy.newProxyInstance(
            RubricVersionRepository.class.getClassLoader(),
            new Class<?>[] {RubricVersionRepository.class},
            (proxy, method, args) -> {
              if ("findByPublicIdAndRubricCreatedBy".equals(method.getName())) {
                return foundVersion;
              }
              if ("toString".equals(method.getName())) {
                return "RubricVersionRepositoryTestDouble";
              }
              return null;
            });
  }

  private RubricVersionRepository ignoredVersionRepository() {
    return versionRepository(Optional.empty());
  }

  private UserRepository userRepository(Optional<User> foundUser) {
    return (UserRepository)
        Proxy.newProxyInstance(
            UserRepository.class.getClassLoader(),
            new Class<?>[] {UserRepository.class},
            (proxy, method, args) -> {
              if ("findByUsername".equals(method.getName())) {
                return foundUser;
              }
              if ("toString".equals(method.getName())) {
                return "UserRepositoryTestDouble";
              }
              return null;
            });
  }

  private User user() {
    User user = new User();
    user.setId(1L);
    user.setPublicId(UUID.fromString("7b7b7d42-5f42-4c5a-9281-8d1d36f6f59d"));
    user.setUsername("qc.demo@example.com");
    return user;
  }

  private RubricVersion version(User creator, RubricVersionStatus status) {
    Project project = new Project();
    project.setPublicId(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"));
    Rubric rubric = new Rubric();
    rubric.setPublicId(UUID.fromString("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"));
    rubric.setProject(project);
    rubric.setStatus(RubricStatus.ACTIVE);
    rubric.setCreatedBy(creator);
    RubricVersion version = new RubricVersion();
    version.setPublicId(UUID.fromString("5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a"));
    version.setRubric(rubric);
    version.setVersion(1);
    version.setStatus(status);
    version.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:45:00Z"));
    return version;
  }

  private RubricCriterion criterion(RubricVersion version) {
    RubricCriterion criterion = new RubricCriterion();
    criterion.setPublicId(UUID.fromString("d10d218f-0e3c-4771-bf80-9815751f6460"));
    criterion.setRubricVersion(version);
    criterion.setName("Correctness");
    criterion.setWeight(new BigDecimal("0.4000"));
    criterion.setJudgeInstruction("Compare with ground truth.");
    criterion.setMetricKey("correctness");
    criterion.setCritical(true);
    criterion.setSortOrder(1);
    criterion.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:50:00Z"));
    criterion.setUpdatedAt(OffsetDateTime.parse("2026-06-08T10:50:00Z"));
    return criterion;
  }
}
