package me.nghlong3004.vqc.api.project.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.enums.ProjectStatus;
import me.nghlong3004.vqc.api.project.mapper.ProjectMapper;
import me.nghlong3004.vqc.api.project.repository.ProjectRepository;
import me.nghlong3004.vqc.api.project.request.CreateProjectRequest;
import me.nghlong3004.vqc.api.project.request.UpdateProjectRequest;
import me.nghlong3004.vqc.api.project.response.ProjectCreatorResponse;
import me.nghlong3004.vqc.api.project.response.ProjectListItemResponse;
import me.nghlong3004.vqc.api.project.response.ProjectPageResponse;
import me.nghlong3004.vqc.api.project.response.ProjectResponse;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class ProjectServiceImplTest {

  @Test
  void createProjectNormalizesCreatorAndPersistsActiveProjectWithDefaults() {
    User creator = new User();
    creator.setUsername("qc.demo@example.com");
    creator.setDisplayName("QC Demo");
    AtomicReference<String> lookedUpUsername = new AtomicReference<>();
    AtomicReference<Project> savedProject = new AtomicReference<>();
    ProjectResponse mappedResponse =
        new ProjectResponse(
            null,
            "AI Health Chatbot Demo",
            "Evaluate health chatbot answers.",
            null,
            30,
            ProjectStatus.ACTIVE,
            new ProjectCreatorResponse(null, "QC Demo"),
            null,
            null);
    ProjectServiceImpl projectService =
        new ProjectServiceImpl(
            projectRepository(savedProject),
            userRepository(Optional.of(creator), lookedUpUsername),
            mapper(mappedResponse));

    ProjectResponse response =
        projectService.createProject(
            new CreateProjectRequest(
                "  AI Health Chatbot Demo  ", "  Evaluate health chatbot answers.  ", "   ", null),
            "  QC.Demo@Example.COM  ");

    assertThat(lookedUpUsername).hasValue("qc.demo@example.com");
    assertThat(savedProject.get().getName()).isEqualTo("AI Health Chatbot Demo");
    assertThat(savedProject.get().getDescription()).isEqualTo("Evaluate health chatbot answers.");
    assertThat(savedProject.get().getEvaluationScope()).isNull();
    assertThat(savedProject.get().getRetentionDays()).isEqualTo(30);
    assertThat(savedProject.get().getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    assertThat(savedProject.get().getCreatedBy()).isSameAs(creator);
    assertThat(response).isSameAs(mappedResponse);
  }

  @Test
  void createProjectRejectsMissingCreator() {
    ProjectServiceImpl projectService =
        new ProjectServiceImpl(
            ignoredProjectRepository(),
            userRepository(Optional.empty(), new AtomicReference<>()),
            ignoredMapper());

    assertThatThrownBy(
            () ->
                projectService.createProject(
                    new CreateProjectRequest("AI Health Chatbot Demo", null, null, 30),
                    "missing@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting("response.code")
        .isEqualTo("USER_NOT_FOUND");
  }

  @Test
  void listProjectsLoadsCreatorAndReturnsFilteredPage() {
    User creator = new User();
    creator.setUsername("qc.demo@example.com");
    Project project = new Project();
    project.setName("AI Health Chatbot Demo");
    project.setStatus(ProjectStatus.ACTIVE);
    project.setCreatedBy(creator);
    project.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"));
    project.setUpdatedAt(OffsetDateTime.parse("2026-06-08T10:35:00Z"));
    AtomicReference<String> lookedUpUsername = new AtomicReference<>();
    AtomicReference<ProjectQuery> projectQuery = new AtomicReference<>();
    ProjectListItemResponse itemResponse =
        new ProjectListItemResponse(
            project.getPublicId(),
            "AI Health Chatbot Demo",
            ProjectStatus.ACTIVE,
            project.getCreatedAt(),
            project.getUpdatedAt());
    ProjectServiceImpl projectService =
        new ProjectServiceImpl(
            projectRepository(
                new AtomicReference<>(),
                new PageImpl<>(List.of(project), PageRequest.of(0, 20), 1),
                projectQuery),
            userRepository(Optional.of(creator), lookedUpUsername),
            mapper(null, itemResponse));

    ProjectPageResponse response =
        projectService.listProjects(
            ProjectStatus.ACTIVE, PageRequest.of(0, 20), "  QC.Demo@Example.COM  ");

    assertThat(lookedUpUsername).hasValue("qc.demo@example.com");
    assertThat(projectQuery.get().createdBy()).isSameAs(creator);
    assertThat(projectQuery.get().status()).isEqualTo(ProjectStatus.ACTIVE);
    assertThat(response.items()).containsExactly(itemResponse);
    assertThat(response.page()).isZero();
    assertThat(response.size()).isEqualTo(20);
    assertThat(response.totalItems()).isEqualTo(1);
    assertThat(response.totalPages()).isEqualTo(1);
  }

  @Test
  void listProjectsRejectsMissingCreator() {
    ProjectServiceImpl projectService =
        new ProjectServiceImpl(
            ignoredProjectRepository(),
            userRepository(Optional.empty(), new AtomicReference<>()),
            ignoredMapper());

    assertThatThrownBy(
            () -> projectService.listProjects(null, PageRequest.of(0, 20), "missing@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting("response.code")
        .isEqualTo("USER_NOT_FOUND");
  }

  @Test
  void getProjectLoadsOwnerScopedProject() {
    User creator = new User();
    creator.setUsername("qc.demo@example.com");
    Project project = new Project();
    project.setName("AI Health Chatbot Demo");
    AtomicReference<String> lookedUpUsername = new AtomicReference<>();
    AtomicReference<ProjectLookup> projectLookup = new AtomicReference<>();
    ProjectResponse mappedResponse =
        new ProjectResponse(
            project.getPublicId(),
            "AI Health Chatbot Demo",
            null,
            null,
            30,
            ProjectStatus.ACTIVE,
            null,
            null,
            null);
    ProjectServiceImpl projectService =
        new ProjectServiceImpl(
            projectRepository(new AtomicReference<>(), null, new AtomicReference<>(), Optional.of(project), projectLookup),
            userRepository(Optional.of(creator), lookedUpUsername),
            mapper(mappedResponse));

    ProjectResponse response =
        projectService.getProject(project.getPublicId(), "  QC.Demo@Example.COM  ");

    assertThat(lookedUpUsername).hasValue("qc.demo@example.com");
    assertThat(projectLookup.get().publicId()).isEqualTo(project.getPublicId());
    assertThat(projectLookup.get().createdBy()).isSameAs(creator);
    assertThat(response).isSameAs(mappedResponse);
  }

  @Test
  void getProjectRejectsMissingProject() {
    User creator = new User();
    ProjectServiceImpl projectService =
        new ProjectServiceImpl(
            projectRepository(
                new AtomicReference<>(), null, new AtomicReference<>(), Optional.empty(), new AtomicReference<>()),
            userRepository(Optional.of(creator), new AtomicReference<>()),
            ignoredMapper());

    assertThatThrownBy(
            () -> projectService.getProject(java.util.UUID.randomUUID(), "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting("response.code")
        .isEqualTo("PROJECT_NOT_FOUND");
  }

  @Test
  void updateProjectAppliesProvidedFieldsAndClearsBlankOptionalFields() {
    User creator = new User();
    creator.setUsername("qc.demo@example.com");
    Project project = new Project();
    project.setName("AI Health Chatbot Demo");
    project.setDescription("Old description");
    project.setEvaluationScope("Old scope");
    project.setRetentionDays(30);
    AtomicReference<Project> savedProject = new AtomicReference<>();
    ProjectResponse mappedResponse =
        new ProjectResponse(
            project.getPublicId(),
            "AI Health Chatbot Demo v2",
            null,
            null,
            60,
            ProjectStatus.ACTIVE,
            null,
            null,
            null);
    ProjectServiceImpl projectService =
        new ProjectServiceImpl(
            projectRepository(
                savedProject, null, new AtomicReference<>(), Optional.of(project), new AtomicReference<>()),
            userRepository(Optional.of(creator), new AtomicReference<>()),
            mapper(mappedResponse));

    ProjectResponse response =
        projectService.updateProject(
            project.getPublicId(),
            new UpdateProjectRequest("  AI Health Chatbot Demo v2  ", "   ", "", 60),
            "qc.demo@example.com");

    assertThat(savedProject.get()).isSameAs(project);
    assertThat(project.getName()).isEqualTo("AI Health Chatbot Demo v2");
    assertThat(project.getDescription()).isNull();
    assertThat(project.getEvaluationScope()).isNull();
    assertThat(project.getRetentionDays()).isEqualTo(60);
    assertThat(response).isSameAs(mappedResponse);
  }

  private ProjectRepository projectRepository(AtomicReference<Project> savedProject) {
    return projectRepository(savedProject, null, new AtomicReference<>());
  }

  private ProjectRepository projectRepository(
      AtomicReference<Project> savedProject,
      PageImpl<Project> projectPage,
      AtomicReference<ProjectQuery> projectQuery) {
    return projectRepository(savedProject, projectPage, projectQuery, Optional.empty(), new AtomicReference<>());
  }

  private ProjectRepository projectRepository(
      AtomicReference<Project> savedProject,
      PageImpl<Project> projectPage,
      AtomicReference<ProjectQuery> projectQuery,
      Optional<Project> foundProject,
      AtomicReference<ProjectLookup> projectLookup) {
    return (ProjectRepository)
        Proxy.newProxyInstance(
            ProjectRepository.class.getClassLoader(),
            new Class<?>[] {ProjectRepository.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "save" -> {
                  savedProject.set((Project) args[0]);
                  yield args[0];
                }
                case "findByCreatedBy" -> {
                  projectQuery.set(new ProjectQuery((User) args[0], null));
                  yield projectPage;
                }
                case "findByCreatedByAndStatus" -> {
                  projectQuery.set(new ProjectQuery((User) args[0], (ProjectStatus) args[1]));
                  yield projectPage;
                }
                case "findByPublicIdAndCreatedBy" -> {
                  projectLookup.set(
                      new ProjectLookup((java.util.UUID) args[0], (User) args[1]));
                  yield foundProject;
                }
                case "toString" -> "ProjectRepositoryTestDouble";
                default -> throw new UnsupportedOperationException(method.getName());
              };
            });
  }

  private ProjectRepository ignoredProjectRepository() {
    return projectRepository(new AtomicReference<>());
  }

  private UserRepository userRepository(
      Optional<User> user, AtomicReference<String> lookedUpUsername) {
    return (UserRepository)
        Proxy.newProxyInstance(
            UserRepository.class.getClassLoader(),
            new Class<?>[] {UserRepository.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "findByUsername" -> {
                  lookedUpUsername.set((String) args[0]);
                  yield user;
                }
                case "toString" -> "UserRepositoryTestDouble";
                default -> throw new UnsupportedOperationException(method.getName());
              };
            });
  }

  private ProjectMapper mapper(ProjectResponse response) {
    return mapper(response, null);
  }

  private ProjectMapper mapper(ProjectResponse response, ProjectListItemResponse itemResponse) {
    return new ProjectMapper() {
      @Override
      public ProjectResponse toResponse(Project project) {
        return response;
      }

      @Override
      public ProjectListItemResponse toListItemResponse(Project project) {
        return itemResponse;
      }

      @Override
      public ProjectCreatorResponse toCreatorResponse(User user) {
        throw new AssertionError("Creator mapping should not be called directly");
      }
    };
  }

  private ProjectMapper ignoredMapper() {
    return new ProjectMapper() {
      @Override
      public ProjectResponse toResponse(Project project) {
        throw new AssertionError("Mapper should not be called");
      }

      @Override
      public ProjectListItemResponse toListItemResponse(Project project) {
        throw new AssertionError("Mapper should not be called");
      }

      @Override
      public ProjectCreatorResponse toCreatorResponse(User user) {
        throw new AssertionError("Mapper should not be called");
      }
    };
  }

  private record ProjectQuery(User createdBy, ProjectStatus status) {}

  private record ProjectLookup(java.util.UUID publicId, User createdBy) {}
}
