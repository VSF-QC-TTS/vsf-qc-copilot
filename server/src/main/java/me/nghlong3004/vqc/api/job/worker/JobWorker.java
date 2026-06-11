package me.nghlong3004.vqc.api.job.worker;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.config.WorkerProperties;
import me.nghlong3004.vqc.api.evaluation.handler.EvaluationJobHandler;
import me.nghlong3004.vqc.api.export.handler.ExportJobHandler;
import me.nghlong3004.vqc.api.job.enums.JobType;
import me.nghlong3004.vqc.api.job.repository.JobRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Component
@ConditionalOnProperty(name = "vqc.worker.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class JobWorker implements SmartLifecycle {

  private static final Duration POLL_TIMEOUT = Duration.ofSeconds(5);

  private final StringRedisTemplate redisTemplate;
  private final WorkerProperties workerProperties;
  private final JobRepository jobRepository;
  private final EvaluationJobHandler evaluationJobHandler;
  private final ExportJobHandler exportJobHandler;

  private volatile boolean running;
  private Thread workerThread;

  @Override
  public void start() {
    if (running) {
      return;
    }
    running = true;
    workerThread = new Thread(this::runLoop, "vqc-job-worker");
    workerThread.setDaemon(true);
    workerThread.start();
    log.info("Started job worker for queue {}", workerProperties.getQueueKey());
  }

  @Override
  public void stop() {
    running = false;
    if (workerThread != null) {
      workerThread.interrupt();
    }
    log.info("Stopped job worker for queue {}", workerProperties.getQueueKey());
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public boolean isAutoStartup() {
    return true;
  }

  void processMessage(String message) {
    try {
      UUID jobPublicId = UUID.fromString(message);
      JobType jobType =
          jobRepository
              .findByPublicId(jobPublicId)
              .map(job -> job.getJobType())
              .orElse(null);
      if (jobType == JobType.EVALUATION_RUN) {
        evaluationJobHandler.handle(jobPublicId);
      } else if (jobType == JobType.EXPORT_EXCEL || jobType == JobType.EXPORT_JSON) {
        exportJobHandler.handle(jobPublicId);
      } else {
        log.warn("Discarding unsupported job queue message {} with type {}", message, jobType);
      }
    } catch (IllegalArgumentException ex) {
      log.warn("Discarding invalid job queue message: {}", message);
    } catch (Exception ex) {
      log.error("Failed to process job queue message {}", message, ex);
    }
  }

  private void runLoop() {
    while (running) {
      try {
        String message =
            redisTemplate.opsForList().leftPop(workerProperties.getQueueKey(), POLL_TIMEOUT);
        if (message != null) {
          processMessage(message);
        }
      } catch (Exception ex) {
        if (running) {
          log.error("Job worker polling failed", ex);
        }
      }
    }
  }
}
