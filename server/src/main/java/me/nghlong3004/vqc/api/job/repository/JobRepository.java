package me.nghlong3004.vqc.api.job.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.job.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public interface JobRepository extends JpaRepository<Job, Long> {

  /**
   * Finds a job by its public identifier.
   *
   * @param publicId public job identifier
   * @return {@link Optional} containing the matching {@link Job} when present
   */
  Optional<Job> findByPublicId(UUID publicId);

  /**
   * Finds jobs with any of the given statuses.
   *
   * @param statuses list of {@link JobStatus} values to match
   * @return list of matching {@link Job} entities
   */
  List<Job> findByStatusIn(List<JobStatus> statuses);
}
