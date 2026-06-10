package me.nghlong3004.vqc.api.requirement.service.impl;

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
import me.nghlong3004.vqc.api.requirement.entity.BusinessRequirement;
import me.nghlong3004.vqc.api.requirement.enums.RequirementStatus;
import me.nghlong3004.vqc.api.requirement.mapper.RequirementMapper;
import me.nghlong3004.vqc.api.requirement.repository.BusinessRequirementRepository;
import me.nghlong3004.vqc.api.requirement.request.CreateRequirementRequest;
import me.nghlong3004.vqc.api.requirement.response.RequirementListItemResponse;
import me.nghlong3004.vqc.api.requirement.response.RequirementPageResponse;
import me.nghlong3004.vqc.api.requirement.response.RequirementResponse;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class RequirementServiceImplTest {

  @Test
  void createRequirementNormalizesCreatorAndPersistsActiveRequirement() {
    User creator = user();
    Project project = project(creator);
    AtomicReference<BusinessRequirement> savedRequirement = new AtomicReference<>();
    AtomicReference<ProjectLookup> projectLookup = new AtomicReference<>();
    RequirementResponse mappedResponse =
        new RequirementResponse(
            UUID.fromString("ebd7f0f0-4924-4e81-9795-d1f060bec2f2"),
            project.getPublicId(),
            "Evaluate Apple Health step-count answers.",
            1,
            RequirementStatus.ACTIVE,
            OffsetDateTime.parse("2026-06-08T10:30:00Z"),
            OffsetDateTime.parse("2026-06-08T10:30:00Z"));
    RequirementServiceImpl requirementService =
        new RequirementServiceImpl(
            requirementRepository(savedRequirement),
            projectRepository(projectLookup, Optional.of(project)),
            userRepository(Optional.of(creator), new AtomicReference<>()),
            mapper(mappedResponse));

    RequirementResponse response =
        requirementService.createRequirement(
            project.getPublicId(),
            new CreateRequirementRequest("  Evaluate Apple Health step-count answers.  "),
            "  QC.Demo@Example.COM  ");

    assertThat(response).isSameAs(mappedResponse);
    assertThat(projectLookup.get().publicId()).isEqualTo(project.getPublicId());
    assertThat(projectLookup.get().createdBy()).isSameAs(creator);
    assertThat(savedRequirement.get().getProject()).isSameAs(project);
    assertThat(savedRequirement.get().getCreatedBy()).isSameAs(creator);
    assertThat(savedRequirement.get().getContent())
        .isEqualTo("Evaluate Apple Health step-count answers.");
    assertThat(savedRequirement.get().getVersion()).isEqualTo(1);
    assertThat(savedRequirement.get().getStatus()).isEqualTo(RequirementStatus.ACTIVE);
  }

  @Test
  void createRequirementRejectsMissingCreator() {
    RequirementServiceImpl requirementService =
        new RequirementServiceImpl(
            ignoredRequirementRepository(),
            ignoredProjectRepository(),
            userRepository(Optional.empty(), new AtomicReference<>()),
            new RequirementMapper());

    assertThatThrownBy(
            () ->
                requirementService.createRequirement(
                    UUID.randomUUID(),
                    new CreateRequirementRequest("Evaluate Apple Health step-count answers."),
                    "missing@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(error -> ((ResourceException) error).getResponse().code())
        .isEqualTo("USER_NOT_FOUND");
  }

  @Test
  void createRequirementRejectsMissingProject() {
    User creator = user();
    AtomicReference<ProjectLookup> projectLookup = new AtomicReference<>();
    RequirementServiceImpl requirementService =
        new RequirementServiceImpl(
            ignoredRequirementRepository(),
            projectRepository(projectLookup, Optional.empty()),
            userRepository(Optional.of(creator), new AtomicReference<>()),
            new RequirementMapper());
    UUID projectPublicId = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                requirementService.createRequirement(
                    projectPublicId,
                    new CreateRequirementRequest("Evaluate Apple Health step-count answers."),
                    "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(error -> ((ResourceException) error).getResponse().code())
        .isEqualTo("PROJECT_NOT_FOUND");
    assertThat(projectLookup.get().publicId()).isEqualTo(projectPublicId);
    assertThat(projectLookup.get().createdBy()).isSameAs(creator);
  }

  @Test
  void listRequirementsLoadsOwnedProjectAndReturnsFilteredPage() {
    User creator = user();
    Project project = project(creator);
    BusinessRequirement requirement = requirement(project, creator);
    AtomicReference<ProjectLookup> projectLookup = new AtomicReference<>();
    AtomicReference<RequirementQuery> requirementQuery = new AtomicReference<>();
    RequirementListItemResponse itemResponse =
        new RequirementListItemResponse(
            requirement.getPublicId(),
            project.getPublicId(),
            requirement.getContent(),
            requirement.getVersion(),
            requirement.getStatus(),
            requirement.getCreatedAt());
    RequirementServiceImpl requirementService =
        new RequirementServiceImpl(
            requirementRepository(
                new AtomicReference<>(),
                new PageImpl<>(List.of(requirement), PageRequest.of(0, 20), 1),
                requirementQuery),
            projectRepository(projectLookup, Optional.of(project)),
            userRepository(Optional.of(creator), new AtomicReference<>()),
            mapper(itemResponse));

    RequirementPageResponse response =
        requirementService.listRequirements(
            project.getPublicId(),
            RequirementStatus.ACTIVE,
            PageRequest.of(0, 20),
            "  QC.Demo@Example.COM  ");

    assertThat(projectLookup.get().publicId()).isEqualTo(project.getPublicId());
    assertThat(projectLookup.get().createdBy()).isSameAs(creator);
    assertThat(requirementQuery.get().project()).isSameAs(project);
    assertThat(requirementQuery.get().status()).isEqualTo(RequirementStatus.ACTIVE);
    assertThat(response.items()).containsExactly(itemResponse);
    assertThat(response.page()).isZero();
    assertThat(response.size()).isEqualTo(20);
    assertThat(response.totalItems()).isEqualTo(1);
    assertThat(response.totalPages()).isEqualTo(1);
  }

  @Test
  void listRequirementsUsesUnfilteredQueryWhenStatusIsMissing() {
    User creator = user();
    Project project = project(creator);
    AtomicReference<RequirementQuery> requirementQuery = new AtomicReference<>();
    RequirementServiceImpl requirementService =
        new RequirementServiceImpl(
            requirementRepository(
                new AtomicReference<>(),
                new PageImpl<>(List.of(), PageRequest.of(0, 20), 0),
                requirementQuery),
            projectRepository(new AtomicReference<>(), Optional.of(project)),
            userRepository(Optional.of(creator), new AtomicReference<>()),
            new RequirementMapper());

    requirementService.listRequirements(project.getPublicId(), null, PageRequest.of(0, 20), "qc.demo@example.com");

    assertThat(requirementQuery.get().project()).isSameAs(project);
    assertThat(requirementQuery.get().status()).isNull();
  }

  private BusinessRequirementRepository requirementRepository(
      AtomicReference<BusinessRequirement> savedRequirement) {
    return requirementRepository(savedRequirement, null, new AtomicReference<>());
  }

  private BusinessRequirementRepository requirementRepository(
      AtomicReference<BusinessRequirement> savedRequirement,
      PageImpl<BusinessRequirement> requirementPage,
      AtomicReference<RequirementQuery> requirementQuery) {
    return (BusinessRequirementRepository)
        Proxy.newProxyInstance(
            BusinessRequirementRepository.class.getClassLoader(),
            new Class<?>[] {BusinessRequirementRepository.class},
            (proxy, method, args) -> {
              if ("save".equals(method.getName())) {
                savedRequirement.set((BusinessRequirement) args[0]);
                return args[0];
              }
              if ("findByProject".equals(method.getName())) {
                requirementQuery.set(new RequirementQuery((Project) args[0], null));
                return requirementPage;
              }
              if ("findByProjectAndStatus".equals(method.getName())) {
                requirementQuery.set(new RequirementQuery((Project) args[0], (RequirementStatus) args[1]));
                return requirementPage;
              }
              if ("toString".equals(method.getName())) {
                return "BusinessRequirementRepositoryTestDouble";
              }
              return null;
            });
  }

  private BusinessRequirementRepository ignoredRequirementRepository() {
    return requirementRepository(new AtomicReference<>());
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

  private UserRepository userRepository(
      Optional<User> foundUser, AtomicReference<String> normalizedUsername) {
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
              return false;
            });
  }

  private RequirementMapper mapper(RequirementResponse response) {
    return new RequirementMapper() {
      @Override
      public RequirementResponse toResponse(BusinessRequirement requirement) {
        return response;
      }
    };
  }

  private RequirementMapper mapper(RequirementListItemResponse response) {
    return new RequirementMapper() {
      @Override
      public RequirementListItemResponse toListItemResponse(BusinessRequirement requirement) {
        return response;
      }
    };
  }

  private User user() {
    User user = new User();
    user.setId(1L);
    user.setPublicId(UUID.fromString("7b7b7d42-5f42-4c5a-9281-8d1d36f6f59d"));
    user.setUsername("qc.demo@example.com");
    user.setDisplayName("QC Demo");
    return user;
  }

  private Project project(User creator) {
    Project project = new Project();
    project.setId(10L);
    project.setPublicId(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"));
    project.setName("AI Health Chatbot Demo");
    project.setCreatedBy(creator);
    return project;
  }

  private BusinessRequirement requirement(Project project, User creator) {
    BusinessRequirement requirement = new BusinessRequirement();
    requirement.setId(100L);
    requirement.setPublicId(UUID.fromString("ebd7f0f0-4924-4e81-9795-d1f060bec2f2"));
    requirement.setProject(project);
    requirement.setContent("Evaluate Apple Health step-count answers.");
    requirement.setVersion(1);
    requirement.setStatus(RequirementStatus.ACTIVE);
    requirement.setCreatedBy(creator);
    requirement.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"));
    requirement.setUpdatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"));
    return requirement;
  }

  private record ProjectLookup(UUID publicId, User createdBy) {}

  private record RequirementQuery(Project project, RequirementStatus status) {}
}
