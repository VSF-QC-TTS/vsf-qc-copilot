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
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.repository.ProjectRepository;
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
  void createRubricNormalizesCreatorAndPersistsActiveRubric() {
    User creator = user();
    Project project = project(creator);
    AtomicReference<Rubric> savedRubric = new AtomicReference<>();
    AtomicReference<ProjectLookup> projectLookup = new AtomicReference<>();
    RubricServiceImpl service =
        new RubricServiceImpl(
            rubricRepository(savedRubric),
            projectRepository(projectLookup, Optional.of(project)),
            userRepository(Optional.of(creator), new AtomicReference<>()),
            new RubricMapper());

    RubricResponse response =
        service.createRubric(
            project.getPublicId(),
            new CreateRubricRequest("  Health Answer Quality Rubric  ", "  Checks safety.  "),
            "  QC.Demo@Example.COM  ");

    assertThat(projectLookup.get().publicId()).isEqualTo(project.getPublicId());
    assertThat(projectLookup.get().createdBy()).isSameAs(creator);
    assertThat(savedRubric.get().getProject()).isSameAs(project);
    assertThat(savedRubric.get().getCreatedBy()).isSameAs(creator);
    assertThat(savedRubric.get().getName()).isEqualTo("Health Answer Quality Rubric");
    assertThat(savedRubric.get().getDescription()).isEqualTo("Checks safety.");
    assertThat(savedRubric.get().getStatus()).isEqualTo(RubricStatus.ACTIVE);
    assertThat(savedRubric.get().getCurrentVersion()).isNull();
    assertThat(response.name()).isEqualTo("Health Answer Quality Rubric");
    assertThat(response.currentVersion()).isNull();
  }

  @Test
  void createRubricRejectsMissingProject() {
    User creator = user();
    Project project = project(creator);
    AtomicReference<ProjectLookup> projectLookup = new AtomicReference<>();
    RubricServiceImpl service =
        new RubricServiceImpl(
            ignoredRubricRepository(),
            projectRepository(projectLookup, Optional.empty()),
            userRepository(Optional.of(creator), new AtomicReference<>()),
            new RubricMapper());

    assertThatThrownBy(
            () ->
                service.createRubric(
                    project.getPublicId(),
                    new CreateRubricRequest("Health Answer Quality Rubric", null),
                    "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(error -> ((ResourceException) error).getResponse().code())
        .isEqualTo("PROJECT_NOT_FOUND");
    assertThat(projectLookup.get().publicId()).isEqualTo(project.getPublicId());
  }

  @Test
  void listRubricsReturnsFilteredPage() {
    User creator = user();
    Project project = project(creator);
    Rubric rubric = rubric(project, creator);
    AtomicReference<RubricQuery> rubricQuery = new AtomicReference<>();
    RubricServiceImpl service =
        new RubricServiceImpl(
            rubricRepository(
                new AtomicReference<>(),
                new PageImpl<>(List.of(rubric), PageRequest.of(0, 20), 1),
                rubricQuery),
            projectRepository(new AtomicReference<>(), Optional.of(project)),
            userRepository(Optional.of(creator), new AtomicReference<>()),
            new RubricMapper());

    RubricPageResponse response =
        service.listRubrics(
            project.getPublicId(), RubricStatus.ACTIVE, PageRequest.of(0, 20), "qc.demo@example.com");

    assertThat(rubricQuery.get().project()).isSameAs(project);
    assertThat(rubricQuery.get().status()).isEqualTo(RubricStatus.ACTIVE);
    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().publicId()).isEqualTo(rubric.getPublicId());
    assertThat(response.totalItems()).isEqualTo(1);
  }

  @Test
  void getRubricLoadsOwnerScopedRubric() {
    User creator = user();
    Project project = project(creator);
    Rubric rubric = rubric(project, creator);
    AtomicReference<RubricLookup> rubricLookup = new AtomicReference<>();
    RubricServiceImpl service =
        new RubricServiceImpl(
            rubricRepository(
                new AtomicReference<>(),
                null,
                new AtomicReference<>(),
                Optional.of(rubric),
                rubricLookup),
            ignoredProjectRepository(),
            userRepository(Optional.of(creator), new AtomicReference<>()),
            new RubricMapper());

    RubricResponse response = service.getRubric(rubric.getPublicId(), "qc.demo@example.com");

    assertThat(rubricLookup.get().publicId()).isEqualTo(rubric.getPublicId());
    assertThat(rubricLookup.get().createdBy()).isSameAs(creator);
    assertThat(response.projectPublicId()).isEqualTo(project.getPublicId());
  }

  @Test
  void updateRubricRejectsArchivedRubric() {
    User creator = user();
    Project project = project(creator);
    Rubric rubric = rubric(project, creator);
    rubric.setStatus(RubricStatus.ARCHIVED);
    RubricServiceImpl service =
        new RubricServiceImpl(
            rubricRepository(
                new AtomicReference<>(),
                null,
                new AtomicReference<>(),
                Optional.of(rubric),
                new AtomicReference<>()),
            ignoredProjectRepository(),
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
    Project project = project(creator);
    Rubric rubric = rubric(project, creator);
    AtomicReference<Rubric> savedRubric = new AtomicReference<>();
    RubricServiceImpl service =
        new RubricServiceImpl(
            rubricRepository(
                savedRubric,
                null,
                new AtomicReference<>(),
                Optional.of(rubric),
                new AtomicReference<>()),
            ignoredProjectRepository(),
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
              if ("findByProject".equals(method.getName())) {
                rubricQuery.set(new RubricQuery((Project) args[0], null));
                return rubricPage;
              }
              if ("findByProjectAndStatus".equals(method.getName())) {
                rubricQuery.set(new RubricQuery((Project) args[0], (RubricStatus) args[1]));
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

  private ProjectRepository projectRepository(
      AtomicReference<ProjectLookup> projectLookup, Optional<Project> foundProject) {
    return (ProjectRepository)
        Proxy.newProxyInstance(
            ProjectRepository.class.getClassLoader(),
            new Class<?>[] {ProjectRepository.class},
            (proxy, method, args) -> {
              if ("findByPublicIdAndCreatedBy".equals(method.getName())) {
                projectLookup.set(new ProjectLookup((UUID) args[0], (User) args[1]));
                return foundProject;
              }
              if ("toString".equals(method.getName())) {
                return "ProjectRepositoryTestDouble";
              }
              return null;
            });
  }

  private ProjectRepository ignoredProjectRepository() {
    return projectRepository(new AtomicReference<>(), Optional.empty());
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

  private Project project(User creator) {
    Project project = new Project();
    project.setId(10L);
    project.setPublicId(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"));
    project.setCreatedBy(creator);
    return project;
  }

  private Rubric rubric(Project project, User creator) {
    Rubric rubric = new Rubric();
    rubric.setId(100L);
    rubric.setPublicId(UUID.fromString("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"));
    rubric.setProject(project);
    rubric.setName("Health Answer Quality Rubric");
    rubric.setDescription("Checks correctness and safety.");
    rubric.setStatus(RubricStatus.ACTIVE);
    rubric.setCreatedBy(creator);
    rubric.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"));
    rubric.setUpdatedAt(OffsetDateTime.parse("2026-06-08T10:35:00Z"));
    return rubric;
  }

  private record ProjectLookup(UUID publicId, User createdBy) {}

  private record RubricQuery(Project project, RubricStatus status) {}

  private record RubricLookup(UUID publicId, User createdBy) {}
}
