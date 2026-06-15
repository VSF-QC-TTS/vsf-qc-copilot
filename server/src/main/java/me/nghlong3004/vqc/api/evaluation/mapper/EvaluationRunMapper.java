package me.nghlong3004.vqc.api.evaluation.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationResult;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationResultListItemResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationRunDetailResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationRunListItemResponse;
import me.nghlong3004.vqc.api.review.entity.ReviewDecision;
import me.nghlong3004.vqc.api.review.enums.QcStatus;
import me.nghlong3004.vqc.api.review.response.ReviewUserResponse;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Component
public class EvaluationRunMapper {

  private final ObjectMapper objectMapper;

  public EvaluationRunMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

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
        run.getDataset().getName(),
        run.getRubricVersion().getPublicId(),
        run.getRubricVersion().getRubric().getName(),
        run.getTargetApiConnector().getPublicId(),
        run.getJudgeModel() == null ? null : run.getJudgeModel().getPublicId(),
        run.getStatus(),
        run.getTotalCases(),
        safeInt(run.getPassedCases()) + safeInt(run.getFailedCases())
            + safeInt(run.getWarningCases()) + safeInt(run.getErrorCases()),
        safeInt(run.getPassedCases()),
        safeInt(run.getFailedCases()),
        run.getCreatedAt());
  }

  /**
   * Maps an {@link EvaluationRun} entity to a full detail response.
   *
   * @param run evaluation run entity
   * @return detail response
   */
  public EvaluationRunDetailResponse toDetailResponse(EvaluationRun run) {
    return new EvaluationRunDetailResponse(
        run.getPublicId(),
        run.getProject().getPublicId(),
        run.getDataset().getPublicId(),
        run.getDataset().getName(),
        run.getRubricVersion().getPublicId(),
        run.getRubricVersion().getRubric().getName(),
        run.getRubricVersion().getVersion(),
        run.getTargetApiConnector().getPublicId(),
        run.getTargetApiConnector().getName(),
        run.getJudgeModel() == null ? null : run.getJudgeModel().getPublicId(),
        run.getJudgeModel() == null ? null : run.getJudgeModel().getName(),
        run.getJob() == null ? null : run.getJob().getPublicId(),
        run.getStatus(),
        null, // description — entity has no description field yet
        run.getTotalCases(),
        safeInt(run.getPassedCases()) + safeInt(run.getFailedCases())
            + safeInt(run.getWarningCases()) + safeInt(run.getErrorCases()),
        safeInt(run.getPassedCases()),
        safeInt(run.getFailedCases()),
        safeInt(run.getWarningCases()),
        safeInt(run.getErrorCases()),
        run.getPassRate(),
        run.getMaxConcurrency(),
        run.getStartedAt(),
        run.getCompletedAt(),
        run.getCreatedAt(),
        run.getUpdatedAt());
  }

  /**
   * Maps an {@link EvaluationResult} entity to a list item response.
   *
   * @param result evaluation result entity
   * @return result list item response
   */
  public EvaluationResultListItemResponse toResultListItem(EvaluationResult result) {
    ReviewDecision reviewDecision = result.getReviewDecision();
    TestCase tc = result.getTestCase();
    return new EvaluationResultListItemResponse(
        result.getPublicId(),
        result.getEvaluationRun().getPublicId(),
        tc.getPublicId(),
        tc.getExternalId(),
        tc.getQuestion(),
        serializePrecondition(tc),
        tc.getGroundTruth(),
        result.getActualAnswer(),
        result.getJudgeScore(),
        result.getJudgeStatus(),
        result.getJudgeReason(),
        result.getLatencyMs(),
        result.getErrorMessage(),
        result.getCriteriaResultsJson(),
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

  private String serializePrecondition(TestCase tc) {
    if (tc.getPrecondition() == null || tc.getPrecondition().isEmpty()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(tc.getPrecondition());
    } catch (JsonProcessingException e) {
      return tc.getPrecondition().toString();
    }
  }

  private int safeInt(Integer value) {
    return value == null ? 0 : value;
  }
}
