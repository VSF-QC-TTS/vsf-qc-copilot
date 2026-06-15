package me.nghlong3004.vqc.api.job.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.config.WorkerProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes job identifiers to the Redis queue for async processing.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobQueuePublisher {

  private final StringRedisTemplate redisTemplate;
  private final WorkerProperties workerProperties;

  /**
   * Pushes a job public id to the Redis queue.
   *
   * @param jobPublicId string representation of the job's public UUID
   */
  public void publish(String jobPublicId) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              push(jobPublicId);
            }
          });
      log.info("Scheduled job {} for publish after transaction commit", jobPublicId);
      return;
    }

    push(jobPublicId);
  }

  private void push(String jobPublicId) {
    redisTemplate.opsForList().rightPush(workerProperties.getQueueKey(), jobPublicId);
    log.info("Published job {} to queue {}", jobPublicId, workerProperties.getQueueKey());
  }
}
