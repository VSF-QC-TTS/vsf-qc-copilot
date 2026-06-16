package me.nghlong3004.vqc.api.job.service.impl;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.evaluation.repository.EvaluationRunRepository;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.export.repository.ExportFileRepository;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.job.enums.ResourceType;
import me.nghlong3004.vqc.api.job.repository.JobRepository;
import me.nghlong3004.vqc.api.job.response.JobDetailResponse;
import me.nghlong3004.vqc.api.job.service.JobService;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobServiceImpl implements JobService {

  private final JobRepository jobRepository;
  private final UserRepository userRepository;
  private final EvaluationRunRepository evaluationRunRepository;
  private final ExportFileRepository exportFileRepository;
  private final me.nghlong3004.vqc.api.redteam.repository.RedTeamRunRepository redTeamRunRepository;

  @Override
  @Transactional(readOnly = true)
  public JobDetailResponse getJob(UUID jobPublicId, String username) {
    User creator =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new ResourceException(ErrorCode.USER_NOT_FOUND));

    Job job =
        jobRepository
            .findByPublicIdAndCreatedBy(jobPublicId, creator)
            .orElseThrow(() -> new ResourceException(ErrorCode.JOB_NOT_FOUND));

    UUID resourcePublicId = resolveResourcePublicId(job);
    UUID projectPublicId = job.getProject() == null ? null : job.getProject().getPublicId();

    log.info("Loaded job {} by user {}", job.getPublicId(), creator.getPublicId());

    return new JobDetailResponse(
        job.getPublicId(),
        job.getJobType(),
        job.getStatus(),
        job.getResourceType(),
        resourcePublicId,
        projectPublicId,
        job.getProgressCurrent(),
        job.getProgressTotal(),
        job.getErrorMessage(),
        job.getCreatedAt(),
        job.getStartedAt(),
        job.getCompletedAt(),
        job.getUpdatedAt());
  }

  /**
   * Resolves internal {@code resourceId} to the resource's public UUID.
   * Currently supports {@link ResourceType#EVALUATION_RUN}.
   */
  private UUID resolveResourcePublicId(Job job) {
    if (job.getResourceType() == ResourceType.EVALUATION_RUN) {
      return evaluationRunRepository
          .findById(job.getResourceId())
          .map(run -> run.getPublicId())
          .orElse(null);
    }
    if (job.getResourceType() == ResourceType.EXPORT_FILE) {
      return exportFileRepository
          .findById(job.getResourceId())
          .map(exportFile -> exportFile.getPublicId())
          .orElse(null);
    }
    if (job.getResourceType() == ResourceType.RED_TEAM_RUN) {
      return redTeamRunRepository
          .findById(job.getResourceId())
          .map(run -> run.getPublicId())
          .orElse(null);
    }
    return null;
  }
}
