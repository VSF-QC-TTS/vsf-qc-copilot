package me.nghlong3004.vqc.api.job.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.evaluation.enums.EvaluationRunStatus;
import me.nghlong3004.vqc.api.evaluation.repository.EvaluationRunRepository;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.export.entity.ExportFile;
import me.nghlong3004.vqc.api.export.repository.ExportFileRepository;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.job.enums.JobStatus;
import me.nghlong3004.vqc.api.job.enums.JobType;
import me.nghlong3004.vqc.api.job.enums.ResourceType;
import me.nghlong3004.vqc.api.job.repository.JobRepository;
import me.nghlong3004.vqc.api.job.response.JobDetailResponse;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.redteam.entity.RedTeamRun;
import me.nghlong3004.vqc.api.redteam.repository.RedTeamRunRepository;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
class JobServiceImplTest {

  // ── Happy path ──

  @Test
  void getJobReturnsDetailWithResourcePublicId() {
    User creator = user();
    Project project = project(creator);
    UUID evalRunPublicId = UUID.randomUUID();
    EvaluationRun evalRun = EvaluationRun.builder()
        .id(1L)
        .publicId(evalRunPublicId)
        .status(EvaluationRunStatus.PENDING)
        .totalCases(10)
        .createdBy(creator)
        .build();

    Job job = Job.builder()
        .id(1L)
        .publicId(UUID.randomUUID())
        .jobType(JobType.EVALUATION_RUN)
        .status(JobStatus.PENDING)
        .resourceType(ResourceType.EVALUATION_RUN)
        .resourceId(1L)
        .project(project)
        .createdBy(creator)
        .progressCurrent(0)
        .progressTotal(10)
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .build();

    JobServiceImpl service = new JobServiceImpl(
        jobRepository(Optional.of(job)),
        userRepository(Optional.of(creator)),
        evalRunRepository(Optional.of(evalRun)),
        exportFileRepository(Optional.empty()),
        redTeamRunRepository(Optional.empty()));

    JobDetailResponse detail = service.getJob(job.getPublicId(), "qc.demo@example.com");

    assertThat(detail.publicId()).isEqualTo(job.getPublicId());
    assertThat(detail.jobType()).isEqualTo(JobType.EVALUATION_RUN);
    assertThat(detail.status()).isEqualTo(JobStatus.PENDING);
    assertThat(detail.resourceType()).isEqualTo(ResourceType.EVALUATION_RUN);
    assertThat(detail.resourcePublicId()).isEqualTo(evalRunPublicId);
    assertThat(detail.projectPublicId()).isEqualTo(project.getPublicId());
    assertThat(detail.progressCurrent()).isZero();
    assertThat(detail.progressTotal()).isEqualTo(10);
  }

  // ── Not found ──

  @Test
  void getJobReturnsExportResourcePublicId() {
    User creator = user();
    Project project = project(creator);
    UUID exportPublicId = UUID.randomUUID();
    ExportFile exportFile =
        ExportFile.builder().id(9L).publicId(exportPublicId).project(project).createdBy(creator).build();
    Job job =
        Job.builder()
            .id(1L)
            .publicId(UUID.randomUUID())
            .jobType(JobType.EXPORT_JSON)
            .status(JobStatus.COMPLETED)
            .resourceType(ResourceType.EXPORT_FILE)
            .resourceId(exportFile.getId())
            .project(project)
            .createdBy(creator)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

    JobServiceImpl service =
        new JobServiceImpl(
            jobRepository(Optional.of(job)),
            userRepository(Optional.of(creator)),
            evalRunRepository(Optional.empty()),
            exportFileRepository(Optional.of(exportFile)),
            redTeamRunRepository(Optional.empty()));

    JobDetailResponse detail = service.getJob(job.getPublicId(), creator.getUsername());

    assertThat(detail.resourceType()).isEqualTo(ResourceType.EXPORT_FILE);
    assertThat(detail.resourcePublicId()).isEqualTo(exportPublicId);
  }

  @Test
  void getJobReturnsRedTeamResourcePublicId() {
    User creator = user();
    Project project = project(creator);
    UUID redTeamPublicId = UUID.randomUUID();
    RedTeamRun redTeamRun =
        RedTeamRun.builder().id(7L).publicId(redTeamPublicId).project(project).createdBy(creator).build();
    Job job =
        Job.builder()
            .id(1L)
            .publicId(UUID.randomUUID())
            .jobType(JobType.RED_TEAM_RUN)
            .status(JobStatus.RUNNING)
            .resourceType(ResourceType.RED_TEAM_RUN)
            .resourceId(redTeamRun.getId())
            .project(project)
            .createdBy(creator)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

    JobServiceImpl service =
        new JobServiceImpl(
            jobRepository(Optional.of(job)),
            userRepository(Optional.of(creator)),
            evalRunRepository(Optional.empty()),
            exportFileRepository(Optional.empty()),
            redTeamRunRepository(Optional.of(redTeamRun)));

    JobDetailResponse detail = service.getJob(job.getPublicId(), creator.getUsername());

    assertThat(detail.resourceType()).isEqualTo(ResourceType.RED_TEAM_RUN);
    assertThat(detail.resourcePublicId()).isEqualTo(redTeamPublicId);
  }

  @Test
  void getJobNotFoundThrows() {
    User creator = user();
    JobServiceImpl service = new JobServiceImpl(
        jobRepository(Optional.empty()),
        userRepository(Optional.of(creator)),
        evalRunRepository(Optional.empty()),
        exportFileRepository(Optional.empty()),
        redTeamRunRepository(Optional.empty()));

    assertThatThrownBy(() -> service.getJob(UUID.randomUUID(), "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("JOB_NOT_FOUND");
  }

  @Test
  void getJobMissingUserThrows() {
    JobServiceImpl service = new JobServiceImpl(
        jobRepository(Optional.empty()),
        userRepository(Optional.empty()),
        evalRunRepository(Optional.empty()),
        exportFileRepository(Optional.empty()),
        redTeamRunRepository(Optional.empty()));

    assertThatThrownBy(() -> service.getJob(UUID.randomUUID(), "missing@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("USER_NOT_FOUND");
  }

  @Test
  void getJobWithDeletedResourceReturnsNullPublicId() {
    User creator = user();
    Project project = project(creator);

    Job job = Job.builder()
        .id(1L)
        .publicId(UUID.randomUUID())
        .jobType(JobType.EVALUATION_RUN)
        .status(JobStatus.COMPLETED)
        .resourceType(ResourceType.EVALUATION_RUN)
        .resourceId(999L)
        .project(project)
        .createdBy(creator)
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .build();

    JobServiceImpl service = new JobServiceImpl(
        jobRepository(Optional.of(job)),
        userRepository(Optional.of(creator)),
        evalRunRepository(Optional.empty()),
        exportFileRepository(Optional.empty()),
        redTeamRunRepository(Optional.empty()));

    JobDetailResponse detail = service.getJob(job.getPublicId(), "qc.demo@example.com");

    assertThat(detail.resourcePublicId()).isNull();
  }

  // ── Proxy helpers ──

  private JobRepository jobRepository(Optional<Job> result) {
    return proxy(JobRepository.class, (p, m, args) -> {
      if ("findByPublicIdAndCreatedBy".equals(m.getName())) return result;
      throw new UnsupportedOperationException(m.getName());
    });
  }

  private UserRepository userRepository(Optional<User> result) {
    return proxy(UserRepository.class, (p, m, args) -> {
      if ("findByUsername".equals(m.getName())) return result;
      throw new UnsupportedOperationException(m.getName());
    });
  }

  private EvaluationRunRepository evalRunRepository(Optional<EvaluationRun> result) {
    return proxy(EvaluationRunRepository.class, (p, m, args) -> {
      if ("findById".equals(m.getName())) return result;
      throw new UnsupportedOperationException(m.getName());
    });
  }

  private ExportFileRepository exportFileRepository(Optional<ExportFile> result) {
    return proxy(ExportFileRepository.class, (p, m, args) -> {
      if ("findById".equals(m.getName())) return result;
      throw new UnsupportedOperationException(m.getName());
    });
  }

  private RedTeamRunRepository redTeamRunRepository(Optional<RedTeamRun> result) {
    return proxy(RedTeamRunRepository.class, (p, m, args) -> {
      if ("findById".equals(m.getName())) return result;
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
        .build();
  }
}
