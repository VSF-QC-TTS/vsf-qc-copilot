package me.nghlong3004.vqc.api.rubric.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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
import me.nghlong3004.vqc.api.rubric.repository.RubricRepository;
import me.nghlong3004.vqc.api.rubric.repository.RubricVersionRepository;
import me.nghlong3004.vqc.api.rubric.request.UpdateRubricVersionRequest;
import me.nghlong3004.vqc.api.rubric.response.RubricVersionPageResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricVersionResponse;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class RubricVersionServiceImplTest {

  @Test
  void createVersionAutoIncrementsFromLatestVersion() {
    User creator = user();
    Rubric rubric = rubric(creator);
    RubricVersion latest = version(rubric, 2, RubricVersionStatus.PUBLISHED);
    AtomicReference<RubricVersion> savedVersion = new AtomicReference<>();
    RubricVersionServiceImpl service =
        service(
            versionRepository(
                savedVersion,
                Optional.of(latest),
                null,
                new AtomicReference<>(),
                Optional.empty(),
                Optional.empty()),
            rubricRepository(new AtomicReference<>(), Optional.of(rubric)),
            criterionRepository(List.of(), new AtomicReference<>(), 0),
            userRepository(Optional.of(creator)));

    RubricVersionResponse response =
        service.createVersion(rubric.getPublicId(), "qc.demo@example.com");

    assertThat(savedVersion.get().getRubric()).isSameAs(rubric);
    assertThat(savedVersion.get().getCreatedBy()).isSameAs(creator);
    assertThat(savedVersion.get().getVersion()).isEqualTo(3);
    assertThat(savedVersion.get().getStatus()).isEqualTo(RubricVersionStatus.DRAFT);
    assertThat(response.version()).isEqualTo(3);
  }

  @Test
  void createVersionRejectsArchivedRubric() {
    User creator = user();
    Rubric rubric = rubric(creator);
    rubric.setStatus(RubricStatus.ARCHIVED);
    RubricVersionServiceImpl service =
        service(
            ignoredVersionRepository(),
            rubricRepository(new AtomicReference<>(), Optional.of(rubric)),
            criterionRepository(List.of(), new AtomicReference<>(), 0),
            userRepository(Optional.of(creator)));

    assertThatThrownBy(() -> service.createVersion(rubric.getPublicId(), "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(error -> ((ResourceException) error).getResponse().code())
        .isEqualTo("RUBRIC_ARCHIVED");
  }

  @Test
  void listVersionsReturnsFilteredPageWithCriterionCounts() {
    User creator = user();
    Rubric rubric = rubric(creator);
    RubricVersion version = version(rubric, 2, RubricVersionStatus.DRAFT);
    AtomicReference<VersionQuery> query = new AtomicReference<>();
    RubricVersionServiceImpl service =
        service(
            versionRepository(
                new AtomicReference<>(),
                Optional.empty(),
                new PageImpl<>(List.of(version), PageRequest.of(0, 20), 1),
                query,
                Optional.empty(),
                Optional.empty()),
            rubricRepository(new AtomicReference<>(), Optional.of(rubric)),
            criterionRepository(List.of(), new AtomicReference<>(), 3),
            userRepository(Optional.of(creator)));

    RubricVersionPageResponse response =
        service.listVersions(
            rubric.getPublicId(), RubricVersionStatus.DRAFT, PageRequest.of(0, 20), "qc.demo@example.com");

    assertThat(query.get().rubric()).isSameAs(rubric);
    assertThat(query.get().status()).isEqualTo(RubricVersionStatus.DRAFT);
    assertThat(response.items().getFirst().totalCriteria()).isEqualTo(3);
  }

  @Test
  void updateVersionPublishesDraftWhenCriteriaWeightsSumOne() {
    User creator = user();
    Rubric rubric = rubric(creator);
    RubricVersion version = version(rubric, 1, RubricVersionStatus.DRAFT);
    AtomicReference<RubricVersion> savedVersion = new AtomicReference<>();
    AtomicReference<Rubric> savedRubric = new AtomicReference<>();
    RubricVersionServiceImpl service =
        service(
            versionRepository(
                savedVersion,
                Optional.empty(),
                null,
                new AtomicReference<>(),
                Optional.of(version),
                Optional.empty()),
            rubricRepository(savedRubric, Optional.empty()),
            criterionRepository(List.of(criterion(version, "correctness", "0.4000"), criterion(version, "safety", "0.6000")), new AtomicReference<>(), 2),
            userRepository(Optional.of(creator)));

    RubricVersionResponse response =
        service.updateVersion(
            version.getPublicId(),
            new UpdateRubricVersionRequest(RubricVersionStatus.PUBLISHED),
            "qc.demo@example.com");

    assertThat(savedVersion.get()).isSameAs(version);
    assertThat(savedRubric.get()).isSameAs(rubric);
    assertThat(version.getStatus()).isEqualTo(RubricVersionStatus.PUBLISHED);
    assertThat(version.getPublishedAt()).isNotNull();
    assertThat(rubric.getCurrentVersion()).isEqualTo(1);
    assertThat(response.status()).isEqualTo(RubricVersionStatus.PUBLISHED);
  }

  @Test
  void updateVersionRejectsPublishWhenWeightsDoNotSumOne() {
    User creator = user();
    Rubric rubric = rubric(creator);
    RubricVersion version = version(rubric, 1, RubricVersionStatus.DRAFT);
    RubricVersionServiceImpl service =
        service(
            versionRepository(
                new AtomicReference<>(),
                Optional.empty(),
                null,
                new AtomicReference<>(),
                Optional.of(version),
                Optional.empty()),
            rubricRepository(new AtomicReference<>(), Optional.empty()),
            criterionRepository(List.of(criterion(version, "correctness", "0.4000")), new AtomicReference<>(), 1),
            userRepository(Optional.of(creator)));

    assertThatThrownBy(
            () ->
                service.updateVersion(
                    version.getPublicId(),
                    new UpdateRubricVersionRequest(RubricVersionStatus.PUBLISHED),
                    "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(error -> ((ResourceException) error).getResponse().code())
        .isEqualTo("RUBRIC_VERSION_PUBLISH_INVALID");
  }

  @Test
  void updateVersionArchivesCurrentAndFallsBackToLatestPublishedVersion() {
    User creator = user();
    Rubric rubric = rubric(creator);
    rubric.setCurrentVersion(3);
    RubricVersion version = version(rubric, 3, RubricVersionStatus.PUBLISHED);
    RubricVersion fallback = version(rubric, 2, RubricVersionStatus.PUBLISHED);
    AtomicReference<RubricVersion> savedVersion = new AtomicReference<>();
    AtomicReference<Rubric> savedRubric = new AtomicReference<>();
    RubricVersionServiceImpl service =
        service(
            versionRepository(
                savedVersion,
                Optional.empty(),
                null,
                new AtomicReference<>(),
                Optional.of(version),
                Optional.of(fallback)),
            rubricRepository(savedRubric, Optional.empty()),
            criterionRepository(List.of(), new AtomicReference<>(), 0),
            userRepository(Optional.of(creator)));

    service.updateVersion(
        version.getPublicId(),
        new UpdateRubricVersionRequest(RubricVersionStatus.ARCHIVED),
        "qc.demo@example.com");

    assertThat(savedVersion.get()).isSameAs(version);
    assertThat(savedRubric.get()).isSameAs(rubric);
    assertThat(version.getStatus()).isEqualTo(RubricVersionStatus.ARCHIVED);
    assertThat(rubric.getCurrentVersion()).isEqualTo(2);
  }

  private RubricVersionServiceImpl service(
      RubricVersionRepository versionRepository,
      RubricRepository rubricRepository,
      RubricCriterionRepository criterionRepository,
      UserRepository userRepository) {
    return new RubricVersionServiceImpl(
        versionRepository, rubricRepository, criterionRepository, userRepository, new RubricMapper());
  }

  private RubricVersionRepository versionRepository(
      AtomicReference<RubricVersion> savedVersion,
      Optional<RubricVersion> latestVersion,
      PageImpl<RubricVersion> versionPage,
      AtomicReference<VersionQuery> versionQuery,
      Optional<RubricVersion> foundVersion,
      Optional<RubricVersion> fallbackVersion) {
    return (RubricVersionRepository)
        Proxy.newProxyInstance(
            RubricVersionRepository.class.getClassLoader(),
            new Class<?>[] {RubricVersionRepository.class},
            (proxy, method, args) -> {
              if ("save".equals(method.getName())) {
                savedVersion.set((RubricVersion) args[0]);
                return args[0];
              }
              if ("findTopByRubricOrderByVersionDesc".equals(method.getName())) {
                return latestVersion;
              }
              if ("findByRubric".equals(method.getName())) {
                versionQuery.set(new VersionQuery((Rubric) args[0], null));
                return versionPage;
              }
              if ("findByRubricAndStatus".equals(method.getName())) {
                versionQuery.set(new VersionQuery((Rubric) args[0], (RubricVersionStatus) args[1]));
                return versionPage;
              }
              if ("findByPublicIdAndRubricCreatedBy".equals(method.getName())) {
                return foundVersion;
              }
              if ("findTopByRubricAndStatusAndPublicIdNotOrderByVersionDesc".equals(method.getName())) {
                return fallbackVersion;
              }
              if ("toString".equals(method.getName())) {
                return "RubricVersionRepositoryTestDouble";
              }
              return null;
            });
  }

  private RubricVersionRepository ignoredVersionRepository() {
    return versionRepository(
        new AtomicReference<>(), Optional.empty(), null, new AtomicReference<>(), Optional.empty(), Optional.empty());
  }

  private RubricRepository rubricRepository(
      AtomicReference<Rubric> savedRubric, Optional<Rubric> foundRubric) {
    return (RubricRepository)
        Proxy.newProxyInstance(
            RubricRepository.class.getClassLoader(),
            new Class<?>[] {RubricRepository.class},
            (proxy, method, args) -> {
              if ("save".equals(method.getName())) {
                savedRubric.set((Rubric) args[0]);
                return args[0];
              }
              if ("findByPublicIdAndCreatedBy".equals(method.getName())) {
                return foundRubric;
              }
              if ("toString".equals(method.getName())) {
                return "RubricRepositoryTestDouble";
              }
              return null;
            });
  }

  private RubricCriterionRepository criterionRepository(
      List<RubricCriterion> criteria,
      AtomicReference<RubricVersion> countedVersion,
      long count) {
    return (RubricCriterionRepository)
        Proxy.newProxyInstance(
            RubricCriterionRepository.class.getClassLoader(),
            new Class<?>[] {RubricCriterionRepository.class},
            (proxy, method, args) -> {
              if ("findByRubricVersionOrderBySortOrderAscIdAsc".equals(method.getName())) {
                return criteria;
              }
              if ("countByRubricVersion".equals(method.getName())) {
                countedVersion.set((RubricVersion) args[0]);
                return count;
              }
              if ("toString".equals(method.getName())) {
                return "RubricCriterionRepositoryTestDouble";
              }
              return null;
            });
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

  private Rubric rubric(User creator) {
    Project project = new Project();
    project.setPublicId(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"));
    Rubric rubric = new Rubric();
    rubric.setId(100L);
    rubric.setPublicId(UUID.fromString("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"));
    rubric.setProject(project);
    rubric.setName("Health Answer Quality Rubric");
    rubric.setStatus(RubricStatus.ACTIVE);
    rubric.setCreatedBy(creator);
    return rubric;
  }

  private RubricVersion version(Rubric rubric, int versionNumber, RubricVersionStatus status) {
    RubricVersion version = new RubricVersion();
    version.setId((long) versionNumber);
    version.setPublicId(UUID.fromString("5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a"));
    version.setRubric(rubric);
    version.setVersion(versionNumber);
    version.setStatus(status);
    version.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:45:00Z"));
    return version;
  }

  private RubricCriterion criterion(RubricVersion version, String metricKey, String weight) {
    RubricCriterion criterion = new RubricCriterion();
    criterion.setPublicId(UUID.randomUUID());
    criterion.setRubricVersion(version);
    criterion.setName(metricKey);
    criterion.setWeight(new BigDecimal(weight));
    criterion.setJudgeInstruction("Judge " + metricKey);
    criterion.setMetricKey(metricKey);
    criterion.setCritical(false);
    criterion.setSortOrder(1);
    criterion.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:50:00Z"));
    criterion.setUpdatedAt(OffsetDateTime.parse("2026-06-08T10:50:00Z"));
    return criterion;
  }

  private record VersionQuery(Rubric rubric, RubricVersionStatus status) {}
}
