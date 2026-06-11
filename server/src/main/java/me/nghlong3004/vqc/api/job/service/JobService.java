package me.nghlong3004.vqc.api.job.service;

import java.util.UUID;
import me.nghlong3004.vqc.api.job.response.JobDetailResponse;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public interface JobService {

  /**
   * Gets a single job detail by public identifier, owner-scoped.
   *
   * @param jobPublicId public job identifier
   * @param username authenticated principal username/email
   * @return job detail response
   */
  JobDetailResponse getJob(UUID jobPublicId, String username);
}
