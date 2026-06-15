package me.nghlong3004.vqc.api.job.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import me.nghlong3004.vqc.api.config.WorkerProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
class JobQueuePublisherTest {

  @AfterEach
  void tearDown() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void publishWithoutTransactionPushesImmediately() {
    Fixture fixture = fixture();

    fixture.publisher.publish("job-1");

    verify(fixture.listOps).rightPush("vqc:jobs:queue", "job-1");
  }

  @Test
  void publishWithinTransactionPushesAfterCommit() {
    Fixture fixture = fixture();
    TransactionSynchronizationManager.initSynchronization();

    fixture.publisher.publish("job-2");

    verify(fixture.listOps, never()).rightPush("vqc:jobs:queue", "job-2");
    List<TransactionSynchronization> synchronizations =
        TransactionSynchronizationManager.getSynchronizations();

    synchronizations.getFirst().afterCommit();

    verify(fixture.listOps).rightPush("vqc:jobs:queue", "job-2");
  }

  @Test
  void publishWithinRolledBackTransactionDoesNotPush() {
    Fixture fixture = fixture();
    TransactionSynchronizationManager.initSynchronization();

    fixture.publisher.publish("job-3");

    TransactionSynchronizationManager.getSynchronizations().getFirst()
        .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

    verify(fixture.listOps, never()).rightPush("vqc:jobs:queue", "job-3");
  }

  @SuppressWarnings("unchecked")
  private Fixture fixture() {
    StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    ListOperations<String, String> listOps = mock(ListOperations.class);
    WorkerProperties workerProperties = new WorkerProperties();
    when(redisTemplate.opsForList()).thenReturn(listOps);
    return new Fixture(new JobQueuePublisher(redisTemplate, workerProperties), listOps);
  }

  private record Fixture(JobQueuePublisher publisher, ListOperations<String, String> listOps) {}
}
