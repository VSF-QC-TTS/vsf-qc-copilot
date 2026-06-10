package me.nghlong3004.vqc.api.dataset.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.dataset.enums.DatasetSourceType;
import me.nghlong3004.vqc.api.dataset.enums.DatasetStatus;
import me.nghlong3004.vqc.api.dataset.mapper.DatasetMapper;
import me.nghlong3004.vqc.api.dataset.repository.DatasetRepository;
import me.nghlong3004.vqc.api.dataset.request.CreateDatasetRequest;
import me.nghlong3004.vqc.api.dataset.response.DatasetResponse;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.repository.ProjectRepository;
import me.nghlong3004.vqc.api.requirement.entity.BusinessRequirement;
import me.nghlong3004.vqc.api.requirement.repository.BusinessRequirementRepository;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class DatasetServiceImplTest {

  @Test
  void createDatasetNormalizesCreatorAndPersistsDraftDataset() {
    User creator = user();
    Project project = project(creator);
    BusinessRequirement requirement = requirement(project, creator);
    AtomicReference<Dataset> savedDataset = new AtomicReference<>();
    AtomicReference<ProjectLookup> projectLookup = new AtomicReference<>();
    AtomicReference<RequirementLookup> requirementLookup = new AtomicReference<>();
    DatasetServiceImpl datasetService =
        new DatasetServiceImpl(
            datasetRepository(savedDataset),
            projectRepository(projectLookup, Optional.of(project)),
            requirementRepository(requirementLookup, Optional.of(requirement)),
            userRepository(Optional.of(creator)),
            new DatasetMapper());

    DatasetResponse response =
        datasetService.createDataset(
            project.getPublicId(),
            new CreateDatasetRequest(
                requirement.getPublicId(),
                DatasetSourceType.SAMPLE_DEMO,
                "  Health Demo Dataset  ",
                "  Sample dataset for Week 4 demo.  "),
            "  QC.Demo@Example.COM  ");

    assertThat(projectLookup.get().publicId()).isEqualTo(project.getPublicId());
    assertThat(projectLookup.get().createdBy()).isSameAs(creator);
    assertThat(requirementLookup.get().publicId()).isEqualTo(requirement.getPublicId());
    assertThat(requirementLookup.get().createdBy()).isSameAs(creator);
    assertThat(savedDataset.get().getProject()).isSameAs(project);
    assertThat(savedDataset.get().getRequirement()).isSameAs(requirement);
    assertThat(savedDataset.get().getCreatedBy()).isSameAs(creator);
    assertThat(savedDataset.get().getName()).isEqualTo("Health Demo Dataset");
    assertThat(savedDataset.get().getDescription()).isEqualTo("Sample dataset for Week 4 demo.");
    assertThat(savedDataset.get().getVersion()).isEqualTo(1);
    assertThat(savedDataset.get().getSourceType()).isEqualTo(DatasetSourceType.SAMPLE_DEMO);
    assertThat(savedDataset.get().getStatus()).isEqualTo(DatasetStatus.DRAFT);
    assertThat(response.projectPublicId()).isEqualTo(project.getPublicId());
    assertThat(response.requirementPublicId()).isEqualTo(requirement.getPublicId());
    assertThat(response.totalCases()).isZero();
  }

  @Test
  void createDatasetAllowsMissingRequirement() {
    User creator = user();
    Project project = project(creator);
    AtomicReference<Dataset> savedDataset = new AtomicReference<>();
    DatasetServiceImpl datasetService =
        new DatasetServiceImpl(
            datasetRepository(savedDataset),
            projectRepository(new AtomicReference<>(), Optional.of(project)),
            ignoredRequirementRepository(),
            userRepository(Optional.of(creator)),
            new DatasetMapper());

    DatasetResponse response =
        datasetService.createDataset(
            project.getPublicId(),
            new CreateDatasetRequest(null, DatasetSourceType.MANUAL, "Manual Dataset", null),
            "qc.demo@example.com");

    assertThat(savedDataset.get().getRequirement()).isNull();
    assertThat(savedDataset.get().getDescription()).isNull();
    assertThat(response.requirementPublicId()).isNull();
  }

  @Test
  void createDatasetRejectsMissingCreator() {
    DatasetServiceImpl datasetService =
        new DatasetServiceImpl(
            ignoredDatasetRepository(),
            ignoredProjectRepository(),
            ignoredRequirementRepository(),
            userRepository(Optional.empty()),
            new DatasetMapper());

    assertThatThrownBy(
            () ->
                datasetService.createDataset(
                    UUID.randomUUID(),
                    new CreateDatasetRequest(null, DatasetSourceType.MANUAL, "Manual Dataset", null),
                    "missing@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(error -> ((ResourceException) error).getResponse().code())
        .isEqualTo("USER_NOT_FOUND");
  }

  @Test
  void createDatasetRejectsMissingProject() {
    User creator = user();
    AtomicReference<ProjectLookup> projectLookup = new AtomicReference<>();
    DatasetServiceImpl datasetService =
        new DatasetServiceImpl(
            ignoredDatasetRepository(),
            projectRepository(projectLookup, Optional.empty()),
            ignoredRequirementRepository(),
            userRepository(Optional.of(creator)),
            new DatasetMapper());
    UUID projectPublicId = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                datasetService.createDataset(
                    projectPublicId,
                    new CreateDatasetRequest(null, DatasetSourceType.MANUAL, "Manual Dataset", null),
                    "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(error -> ((ResourceException) error).getResponse().code())
        .isEqualTo("PROJECT_NOT_FOUND");
    assertThat(projectLookup.get().publicId()).isEqualTo(projectPublicId);
    assertThat(projectLookup.get().createdBy()).isSameAs(creator);
  }

  @Test
  void createDatasetRejectsMissingRequirement() {
    User creator = user();
    Project project = project(creator);
    AtomicReference<RequirementLookup> requirementLookup = new AtomicReference<>();
    DatasetServiceImpl datasetService =
        new DatasetServiceImpl(
            ignoredDatasetRepository(),
            projectRepository(new AtomicReference<>(), Optional.of(project)),
            requirementRepository(requirementLookup, Optional.empty()),
            userRepository(Optional.of(creator)),
            new DatasetMapper());
    UUID requirementPublicId = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                datasetService.createDataset(
                    project.getPublicId(),
                    new CreateDatasetRequest(
                        requirementPublicId, DatasetSourceType.MANUAL, "Manual Dataset", null),
                    "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(error -> ((ResourceException) error).getResponse().code())
        .isEqualTo("REQUIREMENT_NOT_FOUND");
    assertThat(requirementLookup.get().publicId()).isEqualTo(requirementPublicId);
    assertThat(requirementLookup.get().createdBy()).isSameAs(creator);
  }

  @Test
  void createDatasetRejectsRequirementFromAnotherProject() {
    User creator = user();
    Project project = project(creator);
    Project otherProject = project(creator);
    otherProject.setPublicId(UUID.fromString("94b0bc1c-d717-4998-8a28-0d2e9c034ca8"));
    BusinessRequirement requirement = requirement(otherProject, creator);
    DatasetServiceImpl datasetService =
        new DatasetServiceImpl(
            ignoredDatasetRepository(),
            projectRepository(new AtomicReference<>(), Optional.of(project)),
            requirementRepository(new AtomicReference<>(), Optional.of(requirement)),
            userRepository(Optional.of(creator)),
            new DatasetMapper());

    assertThatThrownBy(
            () ->
                datasetService.createDataset(
                    project.getPublicId(),
                    new CreateDatasetRequest(
                        requirement.getPublicId(), DatasetSourceType.MANUAL, "Manual Dataset", null),
                    "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(error -> ((ResourceException) error).getResponse().code())
        .isEqualTo("REQUIREMENT_NOT_FOUND");
  }

  private DatasetRepository datasetRepository(AtomicReference<Dataset> savedDataset) {
    return proxy(
        DatasetRepository.class,
        (proxy, method, args) -> {
          if ("save".equals(method.getName())) {
            Dataset dataset = (Dataset) args[0];
            savedDataset.set(dataset);
            return dataset;
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private DatasetRepository ignoredDatasetRepository() {
    return proxy(
        DatasetRepository.class,
        (proxy, method, args) -> {
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private ProjectRepository projectRepository(
      AtomicReference<ProjectLookup> projectLookup, Optional<Project> project) {
    return proxy(
        ProjectRepository.class,
        (proxy, method, args) -> {
          if ("findByPublicIdAndCreatedBy".equals(method.getName())) {
            projectLookup.set(new ProjectLookup((UUID) args[0], (User) args[1]));
            return project;
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private ProjectRepository ignoredProjectRepository() {
    return proxy(
        ProjectRepository.class,
        (proxy, method, args) -> {
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private BusinessRequirementRepository requirementRepository(
      AtomicReference<RequirementLookup> requirementLookup,
      Optional<BusinessRequirement> requirement) {
    return proxy(
        BusinessRequirementRepository.class,
        (proxy, method, args) -> {
          if ("findByPublicIdAndCreatedBy".equals(method.getName())) {
            requirementLookup.set(new RequirementLookup((UUID) args[0], (User) args[1]));
            return requirement;
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private BusinessRequirementRepository ignoredRequirementRepository() {
    return proxy(
        BusinessRequirementRepository.class,
        (proxy, method, args) -> {
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private UserRepository userRepository(Optional<User> user) {
    return proxy(
        UserRepository.class,
        (proxy, method, args) -> {
          if ("findByUsername".equals(method.getName())) {
            assertThat(args[0]).isEqualTo(String.valueOf(args[0]).trim().toLowerCase());
            return user;
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  @SuppressWarnings("unchecked")
  private <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
  }

  private User user() {
    return User.builder()
        .publicId(UUID.fromString("7b7b7d42-5f42-4c5a-9281-8d1d36f6f59d"))
        .username("qc.demo@example.com")
        .passwordHash("hash")
        .displayName("QC Demo")
        .build();
  }

  private Project project(User creator) {
    return Project.builder()
        .publicId(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"))
        .name("AI Health Chatbot Demo")
        .createdBy(creator)
        .createdAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"))
        .updatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"))
        .build();
  }

  private BusinessRequirement requirement(Project project, User creator) {
    return BusinessRequirement.builder()
        .publicId(UUID.fromString("ebd7f0f0-4924-4e81-9795-d1f060bec2f2"))
        .project(project)
        .content("Evaluate Apple Health step-count answers.")
        .createdBy(creator)
        .createdAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"))
        .updatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"))
        .build();
  }

  private record ProjectLookup(UUID publicId, User createdBy) {}

  private record RequirementLookup(UUID publicId, User createdBy) {}
}
