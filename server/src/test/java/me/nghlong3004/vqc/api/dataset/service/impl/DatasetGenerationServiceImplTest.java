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
import me.nghlong3004.vqc.api.dataset.repository.DatasetRepository;
import me.nghlong3004.vqc.api.dataset.request.GenerateDatasetRequest;
import me.nghlong3004.vqc.api.dataset.response.GenerateDatasetResponse;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.job.enums.JobType;
import me.nghlong3004.vqc.api.job.enums.ResourceType;
import me.nghlong3004.vqc.api.job.repository.JobRepository;
import me.nghlong3004.vqc.api.job.service.JobQueuePublisher;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.requirement.entity.BusinessRequirement;
import me.nghlong3004.vqc.api.requirement.enums.RequirementStatus;
import me.nghlong3004.vqc.api.requirement.repository.BusinessRequirementRepository;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;
import me.nghlong3004.vqc.api.testcase.repository.TestCaseRepository;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/13/2026
 */
class DatasetGenerationServiceImplTest {

  @Test
  void generateCreatesJobAndPublishesToRedis() {
    User creator = user();
    Project project = project(creator);
    Dataset dataset = dataset(project, creator, DatasetStatus.DRAFT);
    BusinessRequirement requirement = requirement(project, creator, RequirementStatus.ACTIVE);

    AtomicReference<Job> savedJob = new AtomicReference<>();
    AtomicReference<Dataset> savedDataset = new AtomicReference<>();
    AtomicReference<String> publishedJobId = new AtomicReference<>();

    DatasetGenerationServiceImpl service =
        new DatasetGenerationServiceImpl(
            datasetRepository(Optional.of(dataset), savedDataset),
            requirementRepository(Optional.of(requirement)),
            testCaseRepository(0L),
            jobRepository(savedJob),
            userRepository(Optional.of(creator)),
            jobQueuePublisher(publishedJobId));

    GenerateDatasetResponse response =
        service.generateTestCases(
            dataset.getPublicId(),
            new GenerateDatasetRequest(
                requirement.getPublicId(), 30, "Focus on edge cases"),
            "qc.demo@example.com");

    assertThat(response.datasetPublicId()).isEqualTo(dataset.getPublicId());
    assertThat(response.jobPublicId()).isNotNull();
    assertThat(response.status()).isEqualTo("PENDING");
    assertThat(response.message()).isEqualTo("Dataset generation queued successfully.");

    assertThat(savedJob.get().getJobType()).isEqualTo(JobType.DATASET_GENERATION);
    assertThat(savedJob.get().getResourceType()).isEqualTo(ResourceType.DATASET);
    assertThat(savedJob.get().getResourceId()).isEqualTo(dataset.getId());
    assertThat(savedJob.get().getProgressTotal()).isEqualTo(30);

    assertThat(savedDataset.get().getGenerationPrompt()).isEqualTo("Focus on edge cases");

    assertThat(publishedJobId.get()).isEqualTo(savedJob.get().getPublicId().toString());
  }

  @Test
  void generateRejectsNonDraftDataset() {
    User creator = user();
    Project project = project(creator);
    Dataset dataset = dataset(project, creator, DatasetStatus.APPROVED);
    BusinessRequirement requirement = requirement(project, creator, RequirementStatus.ACTIVE);

    DatasetGenerationServiceImpl service =
        new DatasetGenerationServiceImpl(
            datasetRepository(Optional.of(dataset), new AtomicReference<>()),
            requirementRepository(Optional.of(requirement)),
            testCaseRepository(0L),
            ignoredJobRepository(),
            userRepository(Optional.of(creator)),
            ignoredJobQueuePublisher());

    assertThatThrownBy(
            () ->
                service.generateTestCases(
                    dataset.getPublicId(),
                    new GenerateDatasetRequest(requirement.getPublicId(), 10, null),
                    "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("DATASET_ARCHIVED");
  }

  @Test
  void generateRejectsMissingDataset() {
    User creator = user();

    DatasetGenerationServiceImpl service =
        new DatasetGenerationServiceImpl(
            datasetRepository(Optional.empty(), new AtomicReference<>()),
            ignoredRequirementRepository(),
            testCaseRepository(0L),
            ignoredJobRepository(),
            userRepository(Optional.of(creator)),
            ignoredJobQueuePublisher());

    assertThatThrownBy(
            () ->
                service.generateTestCases(
                    UUID.randomUUID(),
                    new GenerateDatasetRequest(UUID.randomUUID(), 10, null),
                    "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("DATASET_NOT_FOUND");
  }

  @Test
  void generateRejectsMissingRequirement() {
    User creator = user();
    Project project = project(creator);
    Dataset dataset = dataset(project, creator, DatasetStatus.DRAFT);

    DatasetGenerationServiceImpl service =
        new DatasetGenerationServiceImpl(
            datasetRepository(Optional.of(dataset), new AtomicReference<>()),
            requirementRepository(Optional.empty()),
            testCaseRepository(0L),
            ignoredJobRepository(),
            userRepository(Optional.of(creator)),
            ignoredJobQueuePublisher());

    assertThatThrownBy(
            () ->
                service.generateTestCases(
                    dataset.getPublicId(),
                    new GenerateDatasetRequest(UUID.randomUUID(), 10, null),
                    "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("REQUIREMENT_NOT_FOUND");
  }

  @Test
  void generateRejectsRequirementFromDifferentProject() {
    User creator = user();
    Project project1 = project(creator);
    Project project2 =
        Project.builder()
            .id(2L)
            .publicId(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
            .name("Other Project")
            .createdBy(creator)
            .build();
    Dataset dataset = dataset(project1, creator, DatasetStatus.DRAFT);
    BusinessRequirement requirement = requirement(project2, creator, RequirementStatus.ACTIVE);

    DatasetGenerationServiceImpl service =
        new DatasetGenerationServiceImpl(
            datasetRepository(Optional.of(dataset), new AtomicReference<>()),
            requirementRepository(Optional.of(requirement)),
            testCaseRepository(0L),
            ignoredJobRepository(),
            userRepository(Optional.of(creator)),
            ignoredJobQueuePublisher());

    assertThatThrownBy(
            () ->
                service.generateTestCases(
                    dataset.getPublicId(),
                    new GenerateDatasetRequest(requirement.getPublicId(), 10, null),
                    "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("REQUIREMENT_NOT_FOUND");
  }

  @Test
  void generateRejectsWhenCountWouldExceedMax() {
    User creator = user();
    Project project = project(creator);
    Dataset dataset = dataset(project, creator, DatasetStatus.DRAFT);
    BusinessRequirement requirement = requirement(project, creator, RequirementStatus.ACTIVE);

    DatasetGenerationServiceImpl service =
        new DatasetGenerationServiceImpl(
            datasetRepository(Optional.of(dataset), new AtomicReference<>()),
            requirementRepository(Optional.of(requirement)),
            testCaseRepository(95L),
            ignoredJobRepository(),
            userRepository(Optional.of(creator)),
            ignoredJobQueuePublisher());

    assertThatThrownBy(
            () ->
                service.generateTestCases(
                    dataset.getPublicId(),
                    new GenerateDatasetRequest(requirement.getPublicId(), 10, null),
                    "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("IMPORT_TOO_MANY_ROWS");
  }

  @Test
  void generateRejectsMissingUser() {
    DatasetGenerationServiceImpl service =
        new DatasetGenerationServiceImpl(
            ignoredDatasetRepository(),
            ignoredRequirementRepository(),
            testCaseRepository(0L),
            ignoredJobRepository(),
            userRepository(Optional.empty()),
            ignoredJobQueuePublisher());

    assertThatThrownBy(
            () ->
                service.generateTestCases(
                    UUID.randomUUID(),
                    new GenerateDatasetRequest(UUID.randomUUID(), 10, null),
                    "missing@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("USER_NOT_FOUND");
  }

  // ── Factory helpers ──

  private DatasetRepository datasetRepository(
      Optional<Dataset> dataset, AtomicReference<Dataset> savedDataset) {
    return proxy(
        DatasetRepository.class,
        (proxy, method, args) -> {
          if ("findByPublicIdAndCreatedBy".equals(method.getName())) {
            return dataset;
          }
          if ("save".equals(method.getName())) {
            Dataset d = (Dataset) args[0];
            savedDataset.set(d);
            return d;
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

  private BusinessRequirementRepository requirementRepository(
      Optional<BusinessRequirement> requirement) {
    return proxy(
        BusinessRequirementRepository.class,
        (proxy, method, args) -> {
          if ("findByPublicIdAndCreatedBy".equals(method.getName())) {
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

  private TestCaseRepository testCaseRepository(long activeCount) {
    return proxy(
        TestCaseRepository.class,
        (proxy, method, args) -> {
          if ("countByDatasetAndStatus".equals(method.getName())) {
            return activeCount;
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private JobRepository jobRepository(AtomicReference<Job> savedJob) {
    return proxy(
        JobRepository.class,
        (proxy, method, args) -> {
          if ("save".equals(method.getName())) {
            Job job = (Job) args[0];
            savedJob.set(job);
            return job;
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private JobRepository ignoredJobRepository() {
    return proxy(
        JobRepository.class,
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

  private JobQueuePublisher jobQueuePublisher(AtomicReference<String> publishedJobId) {
    return new JobQueuePublisher(null, null) {
      @Override
      public void publish(String jobPublicId) {
        publishedJobId.set(jobPublicId);
      }
    };
  }

  private JobQueuePublisher ignoredJobQueuePublisher() {
    return new JobQueuePublisher(null, null) {
      @Override
      public void publish(String jobPublicId) {
        throw new UnsupportedOperationException("publish");
      }
    };
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
        .id(1L)
        .publicId(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"))
        .name("AI Health Chatbot Demo")
        .createdBy(creator)
        .build();
  }

  private Dataset dataset(Project project, User creator, DatasetStatus status) {
    return Dataset.builder()
        .id(1L)
        .publicId(UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"))
        .project(project)
        .name("Health Demo Dataset")
        .sourceType(DatasetSourceType.SAMPLE_DEMO)
        .status(status)
        .createdBy(creator)
        .createdAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"))
        .updatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"))
        .build();
  }

  private BusinessRequirement requirement(
      Project project, User creator, RequirementStatus status) {
    return BusinessRequirement.builder()
        .id(1L)
        .publicId(UUID.fromString("ebd7f0f0-4924-4e81-9795-d1f060bec2f2"))
        .project(project)
        .content("The chatbot should answer health-related questions accurately.")
        .status(status)
        .createdBy(creator)
        .createdAt(OffsetDateTime.parse("2026-06-08T10:00:00Z"))
        .updatedAt(OffsetDateTime.parse("2026-06-08T10:00:00Z"))
        .build();
  }
}
