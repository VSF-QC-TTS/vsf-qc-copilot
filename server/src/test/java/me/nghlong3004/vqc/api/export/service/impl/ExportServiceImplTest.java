package me.nghlong3004.vqc.api.export.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.evaluation.repository.EvaluationRunRepository;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.export.entity.ExportFile;
import me.nghlong3004.vqc.api.export.enums.ExportFileStatus;
import me.nghlong3004.vqc.api.export.enums.ExportFileType;
import me.nghlong3004.vqc.api.export.repository.ExportFileRepository;
import me.nghlong3004.vqc.api.export.request.CreateExportRequest;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.job.enums.JobType;
import me.nghlong3004.vqc.api.job.repository.JobRepository;
import me.nghlong3004.vqc.api.job.service.JobQueuePublisher;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
class ExportServiceImplTest {

  @Test
  void createExportCreatesMetadataJobAndPublishes() {
    User creator = user();
    Project project = project(creator);
    EvaluationRun run = run(project, creator);
    AtomicReference<ExportFile> savedExport = new AtomicReference<>();
    AtomicReference<Job> savedJob = new AtomicReference<>();
    AtomicReference<String> publishedJobId = new AtomicReference<>();
    ExportServiceImpl service =
        service(
            Optional.of(creator),
            Optional.of(run),
            savedExport,
            savedJob,
            publishedJobId);

    var response =
        service.createExport(
            run.getPublicId(),
            new CreateExportRequest(ExportFileType.EXCEL),
            creator.getUsername());

    assertThat(response.exportPublicId()).isEqualTo(savedExport.get().getPublicId());
    assertThat(response.jobPublicId()).isEqualTo(savedJob.get().getPublicId());
    assertThat(response.status()).isEqualTo(ExportFileStatus.PENDING);
    assertThat(savedExport.get().getFileType()).isEqualTo(ExportFileType.EXCEL);
    assertThat(savedExport.get().getJob()).isEqualTo(savedJob.get());
    assertThat(savedJob.get().getJobType()).isEqualTo(JobType.EXPORT_EXCEL);
    assertThat(publishedJobId.get()).isEqualTo(savedJob.get().getPublicId().toString());
  }

  @Test
  void createJsonExportUsesJsonJobType() {
    User creator = user();
    Project project = project(creator);
    EvaluationRun run = run(project, creator);
    AtomicReference<Job> savedJob = new AtomicReference<>();
    ExportServiceImpl service =
        service(
            Optional.of(creator),
            Optional.of(run),
            new AtomicReference<>(),
            savedJob,
            new AtomicReference<>());

    service.createExport(
        run.getPublicId(), new CreateExportRequest(ExportFileType.JSON), creator.getUsername());

    assertThat(savedJob.get().getJobType()).isEqualTo(JobType.EXPORT_JSON);
  }

  @Test
  void createExportRejectsMissingUser() {
    ExportServiceImpl service =
        service(Optional.empty(), Optional.empty(), new AtomicReference<>(), new AtomicReference<>(), new AtomicReference<>());

    assertThatThrownBy(
            () ->
                service.createExport(
                    UUID.randomUUID(), new CreateExportRequest(ExportFileType.EXCEL), "missing@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("USER_NOT_FOUND");
  }

  @Test
  void createExportRejectsMissingRun() {
    User creator = user();
    ExportServiceImpl service =
        service(Optional.of(creator), Optional.empty(), new AtomicReference<>(), new AtomicReference<>(), new AtomicReference<>());

    assertThatThrownBy(
            () ->
                service.createExport(
                    UUID.randomUUID(), new CreateExportRequest(ExportFileType.EXCEL), creator.getUsername()))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("EVALUATION_RUN_NOT_FOUND");
  }

  private ExportServiceImpl service(
      Optional<User> creator,
      Optional<EvaluationRun> run,
      AtomicReference<ExportFile> savedExport,
      AtomicReference<Job> savedJob,
      AtomicReference<String> publishedJobId) {
    return new ExportServiceImpl(
        exportFileRepository(savedExport),
        evaluationRunRepository(run),
        jobRepository(savedJob),
        userRepository(creator),
        publisher(publishedJobId));
  }

  private ExportFileRepository exportFileRepository(AtomicReference<ExportFile> savedExport) {
    return proxy(
        ExportFileRepository.class,
        (proxy, method, args) -> {
          if ("save".equals(method.getName())) {
            ExportFile exportFile = (ExportFile) args[0];
            if (exportFile.getId() == null) {
              exportFile.setId(1L);
            }
            if (exportFile.getPublicId() == null) {
              exportFile.setPublicId(UUID.randomUUID());
            }
            savedExport.set(exportFile);
            return exportFile;
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private EvaluationRunRepository evaluationRunRepository(Optional<EvaluationRun> run) {
    return proxy(
        EvaluationRunRepository.class,
        (proxy, method, args) -> {
          if ("findByPublicIdAndCreatedBy".equals(method.getName())) return run;
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private JobRepository jobRepository(AtomicReference<Job> savedJob) {
    return proxy(
        JobRepository.class,
        (proxy, method, args) -> {
          if ("save".equals(method.getName())) {
            Job job = (Job) args[0];
            if (job.getId() == null) {
              job.setId(1L);
            }
            if (job.getPublicId() == null) {
              job.setPublicId(UUID.randomUUID());
            }
            savedJob.set(job);
            return job;
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private UserRepository userRepository(Optional<User> creator) {
    return proxy(
        UserRepository.class,
        (proxy, method, args) -> {
          if ("findByUsername".equals(method.getName())) return creator;
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private JobQueuePublisher publisher(AtomicReference<String> publishedJobId) {
    return new JobQueuePublisher(null, null) {
      @Override
      public void publish(String jobPublicId) {
        publishedJobId.set(jobPublicId);
      }
    };
  }

  @SuppressWarnings("unchecked")
  private <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
  }

  private User user() {
    return User.builder()
        .id(1L)
        .publicId(UUID.randomUUID())
        .username("qc.demo@example.com")
        .passwordHash("hash")
        .displayName("QC Demo")
        .build();
  }

  private Project project(User creator) {
    return Project.builder()
        .id(1L)
        .publicId(UUID.randomUUID())
        .name("Project")
        .createdBy(creator)
        .build();
  }

  private EvaluationRun run(Project project, User creator) {
    return EvaluationRun.builder()
        .id(1L)
        .publicId(UUID.randomUUID())
        .project(project)
        .createdBy(creator)
        .build();
  }
}
