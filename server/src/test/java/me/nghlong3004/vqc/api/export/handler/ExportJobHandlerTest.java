package me.nghlong3004.vqc.api.export.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationResult;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.evaluation.repository.EvaluationResultRepository;
import me.nghlong3004.vqc.api.export.entity.ExportFile;
import me.nghlong3004.vqc.api.export.enums.ExportFileStatus;
import me.nghlong3004.vqc.api.export.enums.ExportFileType;
import me.nghlong3004.vqc.api.export.generator.ExportGenerator;
import me.nghlong3004.vqc.api.export.generator.GeneratedExportFile;
import me.nghlong3004.vqc.api.export.repository.ExportFileRepository;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.job.enums.JobStatus;
import me.nghlong3004.vqc.api.job.enums.JobType;
import me.nghlong3004.vqc.api.job.enums.ResourceType;
import me.nghlong3004.vqc.api.job.repository.JobRepository;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.storage.model.StoreObjectCommand;
import me.nghlong3004.vqc.api.storage.model.StorageResource;
import me.nghlong3004.vqc.api.storage.model.StoredObject;
import me.nghlong3004.vqc.api.storage.service.ObjectStorageService;
import me.nghlong3004.vqc.api.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
class ExportJobHandlerTest {

  @Test
  void handleGeneratesExportAndCompletesJob() {
    User creator = user();
    Project project = project(creator);
    EvaluationRun run = run(project, creator);
    ExportFile exportFile = exportFile(run, project, creator, ExportFileType.JSON);
    Job job = job(exportFile, project, creator, JobType.EXPORT_JSON);
    AtomicReference<Job> savedJob = new AtomicReference<>();
    AtomicReference<ExportFile> savedExport = new AtomicReference<>();
    AtomicReference<StoreObjectCommand> storeCommand = new AtomicReference<>();
    ExportJobHandler handler =
        new ExportJobHandler(
            jobRepository(Optional.of(job), savedJob),
            exportFileRepository(Optional.of(exportFile), savedExport),
            evaluationResultRepository(),
            List.of(generator(ExportFileType.JSON, false)),
            storageService(storeCommand, false));

    handler.handle(job.getPublicId());

    assertThat(savedExport.get().getStatus()).isEqualTo(ExportFileStatus.READY);
    assertThat(savedExport.get().getFileName()).isEqualTo("export.json");
    assertThat(savedExport.get().getStorageProvider()).isEqualTo("LOCAL");
    assertThat(savedExport.get().getStorageKey())
        .isEqualTo("exports/" + exportFile.getPublicId() + "/export.json");
    assertThat(savedExport.get().getContentType()).isEqualTo("application/json");
    assertThat(savedExport.get().getSizeBytes()).isEqualTo(2L);
    assertThat(savedExport.get().getReadyAt()).isNotNull();
    assertThat(storeCommand.get().content()).containsExactly((byte) '[', (byte) ']');
    assertThat(savedJob.get().getStatus()).isEqualTo(JobStatus.COMPLETED);
    assertThat(savedJob.get().getProgressCurrent()).isEqualTo(1);
    assertThat(savedJob.get().getProgressTotal()).isEqualTo(1);
  }

  @Test
  void handleMarksJobAndExportFailedWhenGeneratorFails() {
    User creator = user();
    Project project = project(creator);
    EvaluationRun run = run(project, creator);
    ExportFile exportFile = exportFile(run, project, creator, ExportFileType.EXCEL);
    Job job = job(exportFile, project, creator, JobType.EXPORT_EXCEL);
    AtomicReference<Job> savedJob = new AtomicReference<>();
    AtomicReference<ExportFile> savedExport = new AtomicReference<>();
    ExportJobHandler handler =
        new ExportJobHandler(
            jobRepository(Optional.of(job), savedJob),
            exportFileRepository(Optional.of(exportFile), savedExport),
            evaluationResultRepository(),
            List.of(generator(ExportFileType.EXCEL, true)),
            storageService(new AtomicReference<>(), false));

    handler.handle(job.getPublicId());

    assertThat(savedJob.get().getStatus()).isEqualTo(JobStatus.FAILED);
    assertThat(savedJob.get().getErrorMessage()).contains("generate failed");
    assertThat(savedExport.get().getStatus()).isEqualTo(ExportFileStatus.FAILED);
    assertThat(savedExport.get().getErrorMessage()).contains("generate failed");
  }

  @Test
  void handleMarksJobAndExportFailedWhenStorageFails() {
    User creator = user();
    Project project = project(creator);
    EvaluationRun run = run(project, creator);
    ExportFile exportFile = exportFile(run, project, creator, ExportFileType.JSON);
    Job job = job(exportFile, project, creator, JobType.EXPORT_JSON);
    AtomicReference<Job> savedJob = new AtomicReference<>();
    AtomicReference<ExportFile> savedExport = new AtomicReference<>();
    ExportJobHandler handler =
        new ExportJobHandler(
            jobRepository(Optional.of(job), savedJob),
            exportFileRepository(Optional.of(exportFile), savedExport),
            evaluationResultRepository(),
            List.of(generator(ExportFileType.JSON, false)),
            storageService(new AtomicReference<>(), true));

    handler.handle(job.getPublicId());

    assertThat(savedJob.get().getStatus()).isEqualTo(JobStatus.FAILED);
    assertThat(savedJob.get().getErrorMessage()).contains("storage failed");
    assertThat(savedExport.get().getStatus()).isEqualTo(ExportFileStatus.FAILED);
    assertThat(savedExport.get().getErrorMessage()).contains("storage failed");
  }

  @Test
  void handleIgnoresMissingJob() {
    AtomicReference<Job> savedJob = new AtomicReference<>();
    ExportJobHandler handler =
        new ExportJobHandler(
            jobRepository(Optional.empty(), savedJob),
            exportFileRepository(Optional.empty(), new AtomicReference<>()),
            evaluationResultRepository(),
            List.of(generator(ExportFileType.JSON, false)),
            storageService(new AtomicReference<>(), false));

    handler.handle(UUID.randomUUID());

    assertThat(savedJob.get()).isNull();
  }

  private JobRepository jobRepository(Optional<Job> job, AtomicReference<Job> savedJob) {
    return proxy(
        JobRepository.class,
        (proxy, method, args) -> {
          if ("findByPublicId".equals(method.getName())) return job;
          if ("save".equals(method.getName())) {
            savedJob.set((Job) args[0]);
            return args[0];
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private ExportFileRepository exportFileRepository(
      Optional<ExportFile> exportFile, AtomicReference<ExportFile> savedExport) {
    return proxy(
        ExportFileRepository.class,
        (proxy, method, args) -> {
          if ("findById".equals(method.getName())) return exportFile;
          if ("save".equals(method.getName())) {
            savedExport.set((ExportFile) args[0]);
            return args[0];
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private EvaluationResultRepository evaluationResultRepository() {
    return proxy(
        EvaluationResultRepository.class,
        (proxy, method, args) -> {
          if ("findByEvaluationRunId".equals(method.getName())) return new PageImpl<>(List.of());
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private ExportGenerator generator(ExportFileType fileType, boolean fail) {
    return new ExportGenerator() {
      @Override
      public boolean supports(ExportFileType candidate) {
        return candidate == fileType;
      }

      @Override
      public GeneratedExportFile generate(ExportFile exportFile, List<EvaluationResult> results) {
        if (fail) {
          throw new IllegalStateException("generate failed");
        }
        return new GeneratedExportFile("export.json", "application/json", "[]".getBytes());
      }
    };
  }

  private ObjectStorageService storageService(
      AtomicReference<StoreObjectCommand> storeCommand, boolean fail) {
    return new ObjectStorageService() {
      @Override
      public StoredObject store(StoreObjectCommand command) {
        storeCommand.set(command);
        if (fail) {
          throw new IllegalStateException("storage failed");
        }
        return new StoredObject("LOCAL", command.key(), command.contentType(), command.content().length);
      }

      @Override
      public StorageResource load(String key) {
        throw new UnsupportedOperationException("load");
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
        .displayName("QC Demo")
        .build();
  }

  private Project project(User creator) {
    return Project.builder().id(1L).publicId(UUID.randomUUID()).name("Project").createdBy(creator).build();
  }

  private EvaluationRun run(Project project, User creator) {
    return EvaluationRun.builder()
        .id(1L)
        .publicId(UUID.randomUUID())
        .project(project)
        .createdBy(creator)
        .build();
  }

  private ExportFile exportFile(
      EvaluationRun run, Project project, User creator, ExportFileType fileType) {
    return ExportFile.builder()
        .id(2L)
        .publicId(UUID.randomUUID())
        .project(project)
        .evaluationRun(run)
        .fileType(fileType)
        .status(ExportFileStatus.PENDING)
        .createdBy(creator)
        .build();
  }

  private Job job(ExportFile exportFile, Project project, User creator, JobType jobType) {
    return Job.builder()
        .id(3L)
        .publicId(UUID.randomUUID())
        .jobType(jobType)
        .status(JobStatus.PENDING)
        .resourceType(ResourceType.EXPORT_FILE)
        .resourceId(exportFile.getId())
        .project(project)
        .createdBy(creator)
        .build();
  }
}
