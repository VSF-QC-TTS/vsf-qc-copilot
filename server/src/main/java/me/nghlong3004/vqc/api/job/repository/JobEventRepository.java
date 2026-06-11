package me.nghlong3004.vqc.api.job.repository;

import java.util.List;
import me.nghlong3004.vqc.api.job.entity.JobEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public interface JobEventRepository extends JpaRepository<JobEvent, Long> {

  /**
   * Finds all events for a job ordered by creation time.
   *
   * @param jobId internal job identifier
   * @return list of {@link JobEvent} entities in chronological order
   */
  List<JobEvent> findByJobIdOrderByCreatedAtAsc(Long jobId);
}
