package me.nghlong3004.vqc.api.job.worker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.config.WorkerProperties;
import me.nghlong3004.vqc.api.evaluation.handler.EvaluationJobHandler;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
class JobWorkerTest {

  @Test
  void processMessageDelegatesValidUuidToHandler() {
    AtomicReference<UUID> handled = new AtomicReference<>();
    JobWorker worker = new JobWorker(null, new WorkerProperties(), handler(handled, false));
    UUID jobPublicId = UUID.randomUUID();

    worker.processMessage(jobPublicId.toString());

    assertThat(handled.get()).isEqualTo(jobPublicId);
  }

  @Test
  void processMessageIgnoresInvalidUuid() {
    AtomicReference<UUID> handled = new AtomicReference<>();
    JobWorker worker = new JobWorker(null, new WorkerProperties(), handler(handled, false));

    worker.processMessage("not-a-uuid");

    assertThat(handled.get()).isNull();
  }

  @Test
  void processMessageDoesNotThrowWhenHandlerFails() {
    AtomicReference<UUID> handled = new AtomicReference<>();
    JobWorker worker = new JobWorker(null, new WorkerProperties(), handler(handled, true));
    UUID jobPublicId = UUID.randomUUID();

    worker.processMessage(jobPublicId.toString());

    assertThat(handled.get()).isEqualTo(jobPublicId);
  }

  private EvaluationJobHandler handler(AtomicReference<UUID> handled, boolean fail) {
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
}
