package me.nghlong3004.vqc.api.evaluation.service.impl;

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
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.evaluation.enums.EvaluationRunStatus;
import me.nghlong3004.vqc.api.evaluation.mapper.EvaluationRunMapper;
import me.nghlong3004.vqc.api.evaluation.repository.EvaluationRunRepository;
import me.nghlong3004.vqc.api.evaluation.request.CreateEvaluationRunRequest;
import me.nghlong3004.vqc.api.evaluation.response.CreateEvaluationRunResponse;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.job.repository.JobRepository;
import me.nghlong3004.vqc.api.job.service.JobQueuePublisher;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.repository.ProjectRepository;
import me.nghlong3004.vqc.api.rubric.entity.Rubric;
import me.nghlong3004.vqc.api.rubric.entity.RubricVersion;
import me.nghlong3004.vqc.api.rubric.enums.RubricVersionStatus;
import me.nghlong3004.vqc.api.rubric.repository.RubricVersionRepository;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.targetconnector.enums.BodyType;
import me.nghlong3004.vqc.api.targetconnector.enums.HttpMethodType;
import me.nghlong3004.vqc.api.targetconnector.enums.ResponseFormat;
import me.nghlong3004.vqc.api.targetconnector.repository.TargetApiConnectorRepository;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;
import me.nghlong3004.vqc.api.testcase.repository.TestCaseRepository;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
class EvaluationRunServiceImplTest {

  // ── Happy path ──

  @Test
  void createEvaluationRunQueuesJobAndReturns202() {
    User creator = user();
    Project project = project(creator);
    Dataset dataset = approvedDataset(project, creator);
    RubricVersion rubricVersion = publishedRubricVersion(creator);
    TargetApiConnector connector = activeConnector(project, creator);

    AtomicReference<String> publishedJobId = new AtomicReference<>();
    EvaluationRunServiceImpl service = buildService(
        creator, project, dataset, rubricVersion, connector, 10, publishedJobId);

    CreateEvaluationRunResponse response = service.createEvaluationRun(
        project.getPublicId(),
        new CreateEvaluationRunRequest(
            dataset.getPublicId(),
            rubricVersion.getPublicId(),
            connector.getPublicId(),
            null),
        "qc.demo@example.com");

    assertThat(response.runPublicId()).isNotNull();
    assertThat(response.jobPublicId()).isNotNull();
    assertThat(response.status()).isEqualTo(EvaluationRunStatus.PENDING.name());
    assertThat(response.message()).isEqualTo("Evaluation run queued successfully.");
    assertThat(publishedJobId.get()).isNotNull();
  }

  // ── Validation: dataset ──

  @Test
  void rejectsDatasetNotApproved() {
    User creator = user();
    Project project = project(creator);
    Dataset dataset = draftDataset(project, creator);
    RubricVersion rubricVersion = publishedRubricVersion(creator);
    TargetApiConnector connector = activeConnector(project, creator);

    EvaluationRunServiceImpl service = buildService(
        creator, project, dataset, rubricVersion, connector, 10, new AtomicReference<>());

    assertThatThrownBy(() -> service.createEvaluationRun(
            project.getPublicId(),
            request(dataset, rubricVersion, connector),
            "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("DATASET_NOT_APPROVED");
  }

  @Test
  void rejectsDatasetWithNoActiveCases() {
    User creator = user();
    Project project = project(creator);
    Dataset dataset = approvedDataset(project, creator);
    RubricVersion rubricVersion = publishedRubricVersion(creator);
    TargetApiConnector connector = activeConnector(project, creator);

    EvaluationRunServiceImpl service = buildService(
        creator, project, dataset, rubricVersion, connector, 0, new AtomicReference<>());

    assertThatThrownBy(() -> service.createEvaluationRun(
            project.getPublicId(),
            request(dataset, rubricVersion, connector),
            "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("DATASET_NO_ACTIVE_CASES");
  }

  @Test
  void rejectsDatasetWithTooManyCases() {
    User creator = user();
    Project project = project(creator);
    Dataset dataset = approvedDataset(project, creator);
    RubricVersion rubricVersion = publishedRubricVersion(creator);
    TargetApiConnector connector = activeConnector(project, creator);

    EvaluationRunServiceImpl service = buildService(
        creator, project, dataset, rubricVersion, connector, 101, new AtomicReference<>());

    assertThatThrownBy(() -> service.createEvaluationRun(
            project.getPublicId(),
            request(dataset, rubricVersion, connector),
            "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("DATASET_TOO_MANY_CASES");
  }

  @Test
  void rejectsDatasetFromAnotherProject() {
    User creator = user();
    Project project = project(creator);
    Project otherProject = Project.builder()
        .id(99L)
        .publicId(UUID.randomUUID())
        .name("Other Project")
        .createdBy(creator)
        .build();
    Dataset dataset = approvedDataset(otherProject, creator);
    RubricVersion rubricVersion = publishedRubricVersion(creator);
    TargetApiConnector connector = activeConnector(project, creator);

    EvaluationRunServiceImpl service = buildService(
        creator, project, dataset, rubricVersion, connector, 10, new AtomicReference<>());

    assertThatThrownBy(() -> service.createEvaluationRun(
            project.getPublicId(),
            request(dataset, rubricVersion, connector),
            "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("DATASET_NOT_FOUND");
  }

  // ── Validation: rubric version ──

  @Test
  void rejectsRubricVersionNotPublished() {
    User creator = user();
    Project project = project(creator);
    Dataset dataset = approvedDataset(project, creator);
    RubricVersion rubricVersion = draftRubricVersion(creator);
    TargetApiConnector connector = activeConnector(project, creator);

    EvaluationRunServiceImpl service = buildService(
        creator, project, dataset, rubricVersion, connector, 10, new AtomicReference<>());

    assertThatThrownBy(() -> service.createEvaluationRun(
            project.getPublicId(),
            request(dataset, rubricVersion, connector),
            "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("RUBRIC_VERSION_NOT_PUBLISHED");
  }

  // ── Validation: connector ──

  @Test
  void rejectsConnectorNotActive() {
    User creator = user();
    Project project = project(creator);
    Dataset dataset = approvedDataset(project, creator);
    RubricVersion rubricVersion = publishedRubricVersion(creator);
    TargetApiConnector connector = inactiveConnector(project, creator);

    EvaluationRunServiceImpl service = buildService(
        creator, project, dataset, rubricVersion, connector, 10, new AtomicReference<>());

    assertThatThrownBy(() -> service.createEvaluationRun(
            project.getPublicId(),
            request(dataset, rubricVersion, connector),
            "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("CONNECTOR_NOT_ACTIVE");
  }

  @Test
  void rejectsConnectorFromAnotherProject() {
    User creator = user();
    Project project = project(creator);
    Dataset dataset = approvedDataset(project, creator);
    RubricVersion rubricVersion = publishedRubricVersion(creator);
    Project otherProject = Project.builder()
        .id(99L)
        .publicId(UUID.randomUUID())
        .name("Other Project")
        .createdBy(creator)
        .build();
    TargetApiConnector connector = activeConnector(otherProject, creator);

    EvaluationRunServiceImpl service = buildService(
        creator, project, dataset, rubricVersion, connector, 10, new AtomicReference<>());

    assertThatThrownBy(() -> service.createEvaluationRun(
            project.getPublicId(),
            request(dataset, rubricVersion, connector),
            "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("TARGET_CONNECTOR_NOT_FOUND");
  }

  // ── Validation: auth ──

  @Test
  void rejectsMissingUser() {
    EvaluationRunServiceImpl service = new EvaluationRunServiceImpl(
        ignoredRepo(EvaluationRunRepository.class),
        ignoredRepo(JobRepository.class),
        ignoredRepo(ProjectRepository.class),
        ignoredRepo(DatasetRepository.class),
        ignoredRepo(RubricVersionRepository.class),
        ignoredRepo(TargetApiConnectorRepository.class),
        ignoredRepo(TestCaseRepository.class),
        userRepository(Optional.empty()),
        ignoredPublisher(),
        new EvaluationRunMapper());

    assertThatThrownBy(() -> service.createEvaluationRun(
            UUID.randomUUID(),
            new CreateEvaluationRunRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null),
            "missing@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("USER_NOT_FOUND");
  }

  @Test
  void rejectsMissingProject() {
    User creator = user();
    EvaluationRunServiceImpl service = new EvaluationRunServiceImpl(
        ignoredRepo(EvaluationRunRepository.class),
        ignoredRepo(JobRepository.class),
        projectRepository(Optional.empty()),
        ignoredRepo(DatasetRepository.class),
        ignoredRepo(RubricVersionRepository.class),
        ignoredRepo(TargetApiConnectorRepository.class),
        ignoredRepo(TestCaseRepository.class),
        userRepository(Optional.of(creator)),
        ignoredPublisher(),
        new EvaluationRunMapper());

    assertThatThrownBy(() -> service.createEvaluationRun(
            UUID.randomUUID(),
            new CreateEvaluationRunRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null),
            "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("PROJECT_NOT_FOUND");
  }

  // ── List evaluation runs ──

  @Test
  void listEvaluationRunsReturnsPage() {
    User creator = user();
    Project project = project(creator);
    Dataset dataset = approvedDataset(project, creator);
    RubricVersion rubricVersion = publishedRubricVersion(creator);
    TargetApiConnector connector = activeConnector(project, creator);

    EvaluationRun run = EvaluationRun.builder()
        .publicId(UUID.randomUUID())
        .project(project)
        .dataset(dataset)
        .rubricVersion(rubricVersion)
        .targetApiConnector(connector)
        .status(EvaluationRunStatus.PENDING)
        .totalCases(10)
        .createdBy(creator)
        .createdAt(java.time.OffsetDateTime.now())
        .build();

    EvaluationRunServiceImpl service = new EvaluationRunServiceImpl(
        evaluationRunRepository(new org.springframework.data.domain.PageImpl<>(
            java.util.List.of(run),
            org.springframework.data.domain.PageRequest.of(0, 20), 1)),
        ignoredRepo(JobRepository.class),
        projectRepository(Optional.of(project)),
        ignoredRepo(DatasetRepository.class),
        ignoredRepo(RubricVersionRepository.class),
        ignoredRepo(TargetApiConnectorRepository.class),
        ignoredRepo(TestCaseRepository.class),
        userRepository(Optional.of(creator)),
        ignoredPublisher(),
        new EvaluationRunMapper());

    var response = service.listEvaluationRuns(
        project.getPublicId(),
        org.springframework.data.domain.PageRequest.of(0, 20),
        "qc.demo@example.com");

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().publicId()).isEqualTo(run.getPublicId());
    assertThat(response.items().getFirst().status()).isEqualTo(EvaluationRunStatus.PENDING);
    assertThat(response.page()).isZero();
    assertThat(response.size()).isEqualTo(20);
    assertThat(response.totalItems()).isEqualTo(1);
    assertThat(response.totalPages()).isEqualTo(1);
  }

  @Test
  void listEvaluationRunsReturnsEmptyPage() {
    User creator = user();
    Project project = project(creator);

    EvaluationRunServiceImpl service = new EvaluationRunServiceImpl(
        evaluationRunRepository(new org.springframework.data.domain.PageImpl<>(
            java.util.List.of(),
            org.springframework.data.domain.PageRequest.of(0, 20), 0)),
        ignoredRepo(JobRepository.class),
        projectRepository(Optional.of(project)),
        ignoredRepo(DatasetRepository.class),
        ignoredRepo(RubricVersionRepository.class),
        ignoredRepo(TargetApiConnectorRepository.class),
        ignoredRepo(TestCaseRepository.class),
        userRepository(Optional.of(creator)),
        ignoredPublisher(),
        new EvaluationRunMapper());

    var response = service.listEvaluationRuns(
        project.getPublicId(),
        org.springframework.data.domain.PageRequest.of(0, 20),
        "qc.demo@example.com");

    assertThat(response.items()).isEmpty();
    assertThat(response.totalItems()).isZero();
  }

  // ── Get evaluation run detail ──

  @Test
  void getEvaluationRunReturnsDetail() {
    User creator = user();
    Project project = project(creator);
    Dataset dataset = approvedDataset(project, creator);
    RubricVersion rubricVersion = publishedRubricVersion(creator);
    TargetApiConnector connector = activeConnector(project, creator);

    EvaluationRun run = EvaluationRun.builder()
        .publicId(UUID.randomUUID())
        .project(project)
        .dataset(dataset)
        .rubricVersion(rubricVersion)
        .targetApiConnector(connector)
        .status(EvaluationRunStatus.PENDING)
        .totalCases(10)
        .maxConcurrency(1)
        .createdBy(creator)
        .createdAt(java.time.OffsetDateTime.now())
        .updatedAt(java.time.OffsetDateTime.now())
        .build();

    EvaluationRunServiceImpl service = new EvaluationRunServiceImpl(
        evaluationRunRepositoryForDetail(Optional.of(run)),
        ignoredRepo(JobRepository.class),
        ignoredRepo(ProjectRepository.class),
        ignoredRepo(DatasetRepository.class),
        ignoredRepo(RubricVersionRepository.class),
        ignoredRepo(TargetApiConnectorRepository.class),
        ignoredRepo(TestCaseRepository.class),
        userRepository(Optional.of(creator)),
        ignoredPublisher(),
        new EvaluationRunMapper());

    var detail = service.getEvaluationRun(run.getPublicId(), "qc.demo@example.com");

    assertThat(detail.publicId()).isEqualTo(run.getPublicId());
    assertThat(detail.projectPublicId()).isEqualTo(project.getPublicId());
    assertThat(detail.datasetPublicId()).isEqualTo(dataset.getPublicId());
    assertThat(detail.status()).isEqualTo(EvaluationRunStatus.PENDING);
    assertThat(detail.totalCases()).isEqualTo(10);
    assertThat(detail.maxConcurrency()).isEqualTo(1);
  }

  @Test
  void getEvaluationRunNotFoundThrows() {
    User creator = user();
    EvaluationRunServiceImpl service = new EvaluationRunServiceImpl(
        evaluationRunRepositoryForDetail(Optional.empty()),
        ignoredRepo(JobRepository.class),
        ignoredRepo(ProjectRepository.class),
        ignoredRepo(DatasetRepository.class),
        ignoredRepo(RubricVersionRepository.class),
        ignoredRepo(TargetApiConnectorRepository.class),
        ignoredRepo(TestCaseRepository.class),
        userRepository(Optional.of(creator)),
        ignoredPublisher(),
        new EvaluationRunMapper());

    assertThatThrownBy(() -> service.getEvaluationRun(UUID.randomUUID(), "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("EVALUATION_RUN_NOT_FOUND");
  }

  // ── Builder / helpers ──

  private EvaluationRunServiceImpl buildService(
      User creator, Project project, Dataset dataset, RubricVersion rubricVersion,
      TargetApiConnector connector, long activeCases, AtomicReference<String> publishedJobId) {
    return new EvaluationRunServiceImpl(
        evaluationRunRepository(),
        jobRepository(),
        projectRepository(Optional.of(project)),
        datasetRepository(Optional.of(dataset)),
        rubricVersionRepository(Optional.of(rubricVersion)),
        connectorRepository(Optional.of(connector)),
        testCaseRepository(activeCases),
        userRepository(Optional.of(creator)),
        jobQueuePublisher(publishedJobId),
        new EvaluationRunMapper());
  }

  private CreateEvaluationRunRequest request(
      Dataset dataset, RubricVersion rv, TargetApiConnector connector) {
    return new CreateEvaluationRunRequest(
        dataset.getPublicId(), rv.getPublicId(), connector.getPublicId(), null);
  }

  // ── Proxy repositories ──

  private EvaluationRunRepository evaluationRunRepository() {
    return proxy(EvaluationRunRepository.class, (p, m, args) -> {
      if ("save".equals(m.getName())) return args[0];
      throw new UnsupportedOperationException(m.getName());
    });
  }

  private EvaluationRunRepository evaluationRunRepository(
      org.springframework.data.domain.Page<me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun> page) {
    return proxy(EvaluationRunRepository.class, (p, m, args) -> {
      if ("findByProjectIdAndCreatedBy".equals(m.getName())) return page;
      if ("save".equals(m.getName())) return args[0];
      throw new UnsupportedOperationException(m.getName());
    });
  }

  private EvaluationRunRepository evaluationRunRepositoryForDetail(Optional<EvaluationRun> result) {
    return proxy(EvaluationRunRepository.class, (p, m, args) -> {
      if ("findByPublicIdAndCreatedBy".equals(m.getName())) return result;
      if ("save".equals(m.getName())) return args[0];
      throw new UnsupportedOperationException(m.getName());
    });
  }

  private JobRepository jobRepository() {
    return proxy(JobRepository.class, (p, m, args) -> {
      if ("save".equals(m.getName())) {
        Job job = (Job) args[0];
        if (job.getPublicId() == null) job.setPublicId(UUID.randomUUID());
        return job;
      }
      throw new UnsupportedOperationException(m.getName());
    });
  }

  private ProjectRepository projectRepository(Optional<Project> result) {
    return proxy(ProjectRepository.class, (p, m, args) -> {
      if ("findByPublicIdAndCreatedBy".equals(m.getName())) return result;
      throw new UnsupportedOperationException(m.getName());
    });
  }

  private DatasetRepository datasetRepository(Optional<Dataset> result) {
    return proxy(DatasetRepository.class, (p, m, args) -> {
      if ("findByPublicIdAndCreatedBy".equals(m.getName())) return result;
      throw new UnsupportedOperationException(m.getName());
    });
  }

  private RubricVersionRepository rubricVersionRepository(Optional<RubricVersion> result) {
    return proxy(RubricVersionRepository.class, (p, m, args) -> {
      if ("findByPublicIdAndRubricCreatedBy".equals(m.getName())) return result;
      throw new UnsupportedOperationException(m.getName());
    });
  }

  private TargetApiConnectorRepository connectorRepository(Optional<TargetApiConnector> result) {
    return proxy(TargetApiConnectorRepository.class, (p, m, args) -> {
      if ("findByPublicIdAndCreatedBy".equals(m.getName())) return result;
      throw new UnsupportedOperationException(m.getName());
    });
  }

  private TestCaseRepository testCaseRepository(long activeCases) {
    return proxy(TestCaseRepository.class, (p, m, args) -> {
      if ("countByDatasetAndStatus".equals(m.getName())) {
        assertThat(args[1]).isEqualTo(TestCaseStatus.ACTIVE);
        return activeCases;
      }
      throw new UnsupportedOperationException(m.getName());
    });
  }

  private UserRepository userRepository(Optional<User> result) {
    return proxy(UserRepository.class, (p, m, args) -> {
      if ("findByUsername".equals(m.getName())) return result;
      throw new UnsupportedOperationException(m.getName());
    });
  }

  private JobQueuePublisher jobQueuePublisher(AtomicReference<String> published) {
    return new JobQueuePublisher(null, null) {
      @Override
      public void publish(String jobPublicId) {
        published.set(jobPublicId);
      }
    };
  }

  private JobQueuePublisher ignoredPublisher() {
    return new JobQueuePublisher(null, null) {
      @Override
      public void publish(String jobPublicId) {
        throw new UnsupportedOperationException("publish");
      }
    };
  }

  @SuppressWarnings("unchecked")
  private <T> T ignoredRepo(Class<T> type) {
    return proxy(type, (p, m, args) -> {
      throw new UnsupportedOperationException(m.getName());
    });
  }

  @SuppressWarnings("unchecked")
  private <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
  }

  // ── Test fixtures ──

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
        .createdAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"))
        .updatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"))
        .build();
  }

  private Dataset approvedDataset(Project project, User creator) {
    return Dataset.builder()
        .id(1L)
        .publicId(UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"))
        .project(project)
        .name("Health Demo Dataset")
        .sourceType(DatasetSourceType.SAMPLE_DEMO)
        .status(DatasetStatus.APPROVED)
        .createdBy(creator)
        .build();
  }

  private Dataset draftDataset(Project project, User creator) {
    Dataset ds = approvedDataset(project, creator);
    ds.setStatus(DatasetStatus.DRAFT);
    return ds;
  }

  private RubricVersion publishedRubricVersion(User creator) {
    Rubric rubric = Rubric.builder()
        .id(1L)
        .publicId(UUID.randomUUID())
        .name("Health Rubric")
        .createdBy(creator)
        .build();
    return RubricVersion.builder()
        .id(1L)
        .publicId(UUID.fromString("c3a1b2c3-d4e5-4f7a-8b9c-0d1e2f3a4b5c"))
        .rubric(rubric)
        .version(1)
        .status(RubricVersionStatus.PUBLISHED)
        .createdBy(creator)
        .build();
  }

  private RubricVersion draftRubricVersion(User creator) {
    RubricVersion rv = publishedRubricVersion(creator);
    rv.setStatus(RubricVersionStatus.DRAFT);
    return rv;
  }

  private TargetApiConnector activeConnector(Project project, User creator) {
    return TargetApiConnector.builder()
        .id(1L)
        .publicId(UUID.fromString("a1b2c3d4-e5f6-4a8b-9c0d-1e2f3a4b5c6d"))
        .project(project)
        .name("Mock Chatbot")
        .method(HttpMethodType.POST)
        .url("http://localhost:8080/mock-chatbot/chat")
        .bodyType(BodyType.RAW_JSON)
        .responseFormat(ResponseFormat.JSON)
        .active(true)
        .createdBy(creator)
        .build();
  }

  private TargetApiConnector inactiveConnector(Project project, User creator) {
    TargetApiConnector c = activeConnector(project, creator);
    c.setActive(false);
    return c;
  }
}
