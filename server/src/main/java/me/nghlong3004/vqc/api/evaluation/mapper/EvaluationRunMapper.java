package me.nghlong3004.vqc.api.evaluation.mapper;

import java.util.UUID;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationResult;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationResultListItemResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationRunDetailResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationRunListItemResponse;
import me.nghlong3004.vqc.api.review.entity.ReviewDecision;
import me.nghlong3004.vqc.api.review.enums.QcStatus;
import me.nghlong3004.vqc.api.review.response.ReviewUserResponse;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Component
public class EvaluationRunMapper {

  /**
   * Maps an {@link EvaluationRun} entity to a list item response.
   *
   * @param run evaluation run entity
   * @return list item response
   */
  public EvaluationRunListItemResponse toListItemResponse(EvaluationRun run) {
    return new EvaluationRunListItemResponse(
        run.getPublicId(),
        run.getDataset().getPublicId(),
        run.getRubricVersion().getPublicId(),
        run.getTargetApiConnector().getPublicId(),
        run.getStatus(),
        run.getTotalCases(),
        run.getCreatedAt());
  }

  /**
   * Maps an {@link EvaluationRun} entity to a full detail response.
   *
   * @param run evaluation run entity
   * @return detail response
   */
  public EvaluationRunDetailResponse toDetailResponse(EvaluationRun run) {
    UUID jobPublicId = run.getJob() == null ? null : run.getJob().getPublicId();
    return new EvaluationRunDetailResponse(
        run.getPublicId(),
        run.getProject().getPublicId(),
        run.getDataset().getPublicId(),
        run.getRubricVersion().getPublicId(),
        run.getTargetApiConnector().getPublicId(),
        jobPublicId,
        run.getStatus(),
        run.getTotalCases(),
        run.getMaxConcurrency(),
        run.getCreatedAt(),
        run.getUpdatedAt());
  }

  /**
   * Maps an {@link EvaluationResult} entity to a list item response.
   *
   * @param result evaluation result entity
   * @return result list item response
   */
  public EvaluationResultListItemResponse toResultListItem(
      EvaluationResult result) {
    ReviewDecision reviewDecision = result.getReviewDecision();
    return new EvaluationResultListItemResponse(
        result.getPublicId(),
        result.getEvaluationRun().getPublicId(),
        result.getTestCase().getPublicId(),
        result.getTestCase().getExternalId(),
        result.getTestCase().getQuestion(),
        result.getTestCase().getGroundTruth(),
        result.getActualAnswer(),
        result.getJudgeScore(),
        result.getJudgeStatus(),
        result.getJudgeReason(),
        result.getLatencyMs(),
        result.getErrorMessage(),
        reviewDecision == null ? QcStatus.NOT_REVIEWED : reviewDecision.getQcStatus(),
        reviewDecision == null ? null : reviewDecision.getQcNote(),
        reviewDecision == null ? null : toReviewUserResponse(reviewDecision.getPicBugUser()),
        result.getCreatedAt());
  }

  private ReviewUserResponse toReviewUserResponse(User user) {
    if (user == null) {
      return null;
    }
    return new ReviewUserResponse(user.getPublicId(), user.getDisplayName());
  }
}
