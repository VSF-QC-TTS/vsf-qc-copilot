package me.nghlong3004.vqc.api.project.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.enums.ProjectStatus;
import me.nghlong3004.vqc.api.project.mapper.ProjectMapper;
import me.nghlong3004.vqc.api.project.repository.ProjectRepository;
import me.nghlong3004.vqc.api.project.request.CreateProjectRequest;
import me.nghlong3004.vqc.api.project.response.ProjectCreatorResponse;
import me.nghlong3004.vqc.api.project.response.ProjectResponse;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
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

  private ProjectRepository projectRepository(AtomicReference<Project> savedProject) {
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
    return new ProjectMapper() {
      @Override
      public ProjectResponse toResponse(Project project) {
        return response;
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
      public ProjectCreatorResponse toCreatorResponse(User user) {
        throw new AssertionError("Mapper should not be called");
      }
    };
  }
}
