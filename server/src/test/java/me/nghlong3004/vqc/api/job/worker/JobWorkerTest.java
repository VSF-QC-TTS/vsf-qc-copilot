package me.nghlong3004.vqc.api.job.worker;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.config.WorkerProperties;
import me.nghlong3004.vqc.api.evaluation.handler.EvaluationJobHandler;
import me.nghlong3004.vqc.api.export.handler.ExportJobHandler;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.job.enums.JobType;
import me.nghlong3004.vqc.api.job.repository.JobRepository;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
class JobWorkerTest {

  @Test
  void processMessageDelegatesEvaluationJobToEvaluationHandler() {
    AtomicReference<UUID> handled = new AtomicReference<>();
    UUID jobPublicId = UUID.randomUUID();
    JobWorker worker =
        new JobWorker(
            null,
            new WorkerProperties(),
            jobRepository(Optional.of(job(jobPublicId, JobType.EVALUATION_RUN))),
            evaluationHandler(handled, false),
            exportHandler(new AtomicReference<>(), false));

    worker.processMessage(jobPublicId.toString());

    assertThat(handled.get()).isEqualTo(jobPublicId);
  }

  @Test
  void processMessageDelegatesExportJobToExportHandler() {
    AtomicReference<UUID> handled = new AtomicReference<>();
    UUID jobPublicId = UUID.randomUUID();
    JobWorker worker =
        new JobWorker(
            null,
            new WorkerProperties(),
            jobRepository(Optional.of(job(jobPublicId, JobType.EXPORT_JSON))),
            evaluationHandler(new AtomicReference<>(), false),
            exportHandler(handled, false));

    worker.processMessage(jobPublicId.toString());

    assertThat(handled.get()).isEqualTo(jobPublicId);
  }

  @Test
  void processMessageIgnoresInvalidUuid() {
    AtomicReference<UUID> handled = new AtomicReference<>();
    JobWorker worker =
        new JobWorker(
            null,
            new WorkerProperties(),
            jobRepository(Optional.empty()),
            evaluationHandler(handled, false),
            exportHandler(new AtomicReference<>(), false));

    worker.processMessage("not-a-uuid");

    assertThat(handled.get()).isNull();
  }

  @Test
  void processMessageDoesNotThrowWhenHandlerFails() {
    AtomicReference<UUID> handled = new AtomicReference<>();
    UUID jobPublicId = UUID.randomUUID();
    JobWorker worker =
        new JobWorker(
            null,
            new WorkerProperties(),
            jobRepository(Optional.of(job(jobPublicId, JobType.EVALUATION_RUN))),
            evaluationHandler(handled, true),
            exportHandler(new AtomicReference<>(), false));

    worker.processMessage(jobPublicId.toString());

    assertThat(handled.get()).isEqualTo(jobPublicId);
  }

  private EvaluationJobHandler evaluationHandler(AtomicReference<UUID> handled, boolean fail) {
    return new EvaluationJobHandler(null, null, null, null, null, null, null) {
      @Override
      public void handle(UUID jobPublicId) {
        handled.set(jobPublicId);
        if (fail) {
          throw new IllegalStateException("boom");
        }
      }
    };
  }

  private ExportJobHandler exportHandler(AtomicReference<UUID> handled, boolean fail) {
    return new ExportJobHandler(null, null, null, null) {
      @Override
      public void handle(UUID jobPublicId) {
        handled.set(jobPublicId);
        if (fail) {
          throw new IllegalStateException("boom");
        }
      }
    };
  }

  private Job job(UUID publicId, JobType jobType) {
    return Job.builder().publicId(publicId).jobType(jobType).build();
  }

  private JobRepository jobRepository(Optional<Job> result) {
    return proxy(
        JobRepository.class,
        (proxy, method, args) -> {
          if ("findByPublicId".equals(method.getName())) return result;
          throw new UnsupportedOperationException(method.getName());
        });
  }

  @SuppressWarnings("unchecked")
  private <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
  }
}
