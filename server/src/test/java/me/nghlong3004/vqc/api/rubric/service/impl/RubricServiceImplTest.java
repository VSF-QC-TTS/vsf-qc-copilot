package me.nghlong3004.vqc.api.rubric.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.rubric.entity.Rubric;
import me.nghlong3004.vqc.api.rubric.enums.RubricStatus;
import me.nghlong3004.vqc.api.rubric.mapper.RubricMapper;
import me.nghlong3004.vqc.api.rubric.repository.RubricRepository;
import me.nghlong3004.vqc.api.rubric.request.CreateRubricRequest;
import me.nghlong3004.vqc.api.rubric.request.UpdateRubricRequest;
import me.nghlong3004.vqc.api.rubric.response.RubricPageResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricResponse;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class RubricServiceImplTest {


  @Test
  void getRubricLoadsOwnerScopedRubric() {
    User creator = user();
    Rubric rubric = rubric(creator);
    AtomicReference<RubricLookup> rubricLookup = new AtomicReference<>();
    RubricServiceImpl service =
        new RubricServiceImpl(
            rubricRepository(
                new AtomicReference<>(),
                null,
                new AtomicReference<>(),
                Optional.of(rubric),
                rubricLookup),
            userRepository(Optional.of(creator), new AtomicReference<>()),
            new RubricMapper());

    RubricResponse response = service.getRubric(rubric.getPublicId(), "qc.demo@example.com");

    assertThat(rubricLookup.get().publicId()).isEqualTo(rubric.getPublicId());
    assertThat(rubricLookup.get().createdBy()).isSameAs(creator);
  }

  @Test
  void updateRubricRejectsArchivedRubric() {
    User creator = user();
    Rubric rubric = rubric(creator);
    rubric.setStatus(RubricStatus.ARCHIVED);
    RubricServiceImpl service =
        new RubricServiceImpl(
            rubricRepository(
                new AtomicReference<>(),
                null,
                new AtomicReference<>(),
                Optional.of(rubric),
                new AtomicReference<>()),
            userRepository(Optional.of(creator), new AtomicReference<>()),
            new RubricMapper());

    assertThatThrownBy(
            () ->
                service.updateRubric(
                    rubric.getPublicId(),
                    new UpdateRubricRequest("Updated Rubric", null),
                    "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(error -> ((ResourceException) error).getResponse().code())
        .isEqualTo("RUBRIC_ARCHIVED");
  }

  @Test
  void archiveRubricSetsStatusAndArchivedAt() {
    User creator = user();
    Rubric rubric = rubric(creator);
    AtomicReference<Rubric> savedRubric = new AtomicReference<>();
    RubricServiceImpl service =
        new RubricServiceImpl(
            rubricRepository(
                savedRubric,
                null,
                new AtomicReference<>(),
                Optional.of(rubric),
                new AtomicReference<>()),
            userRepository(Optional.of(creator), new AtomicReference<>()),
            new RubricMapper());

    service.archiveRubric(rubric.getPublicId(), "qc.demo@example.com");

    assertThat(savedRubric.get()).isSameAs(rubric);
    assertThat(rubric.getStatus()).isEqualTo(RubricStatus.ARCHIVED);
    assertThat(rubric.getArchivedAt()).isNotNull();
  }

  private RubricRepository rubricRepository(AtomicReference<Rubric> savedRubric) {
    return rubricRepository(savedRubric, null, new AtomicReference<>());
  }

  private RubricRepository rubricRepository(
      AtomicReference<Rubric> savedRubric,
      PageImpl<Rubric> rubricPage,
      AtomicReference<RubricQuery> rubricQuery) {
    return rubricRepository(savedRubric, rubricPage, rubricQuery, Optional.empty(), new AtomicReference<>());
  }

  private RubricRepository rubricRepository(
      AtomicReference<Rubric> savedRubric,
      PageImpl<Rubric> rubricPage,
      AtomicReference<RubricQuery> rubricQuery,
      Optional<Rubric> foundRubric,
      AtomicReference<RubricLookup> rubricLookup) {
    return (RubricRepository)
        Proxy.newProxyInstance(
            RubricRepository.class.getClassLoader(),
            new Class<?>[] {RubricRepository.class},
            (proxy, method, args) -> {
              if ("save".equals(method.getName())) {
                savedRubric.set((Rubric) args[0]);
                return args[0];
              }
              if ("findByCreatedBy".equals(method.getName())) {
                rubricQuery.set(new RubricQuery(null, null, (User) args[0]));
                return rubricPage;
              }
              if ("findByCreatedByAndStatus".equals(method.getName())) {
                rubricQuery.set(new RubricQuery(null, (RubricStatus) args[1], (User) args[0]));
                return rubricPage;
              }
              if ("findByPublicIdAndCreatedBy".equals(method.getName())) {
                rubricLookup.set(new RubricLookup((UUID) args[0], (User) args[1]));
                return foundRubric;
              }
              if ("toString".equals(method.getName())) {
                return "RubricRepositoryTestDouble";
              }
              return null;
            });
  }

  private RubricRepository ignoredRubricRepository() {
    return rubricRepository(new AtomicReference<>());
  }


  private UserRepository userRepository(Optional<User> foundUser, AtomicReference<String> normalizedUsername) {
    return (UserRepository)
        Proxy.newProxyInstance(
            UserRepository.class.getClassLoader(),
            new Class<?>[] {UserRepository.class},
            (proxy, method, args) -> {
              if ("findByUsername".equals(method.getName())) {
                normalizedUsername.set((String) args[0]);
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
    Rubric rubric = new Rubric();
    rubric.setId(100L);
    rubric.setPublicId(UUID.fromString("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"));
    rubric.setName("Health Answer Quality Rubric");
    rubric.setDescription("Checks correctness and safety.");
    rubric.setStatus(RubricStatus.ACTIVE);
    rubric.setCreatedBy(creator);
    rubric.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"));
    rubric.setUpdatedAt(OffsetDateTime.parse("2026-06-08T10:35:00Z"));
    return rubric;
  }

  private record RubricQuery(Object project, RubricStatus status, User createdBy) {}

  private record RubricLookup(UUID publicId, User createdBy) {}
}
