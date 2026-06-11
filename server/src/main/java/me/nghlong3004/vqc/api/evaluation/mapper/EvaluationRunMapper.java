package me.nghlong3004.vqc.api.evaluation.mapper;

import java.util.UUID;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationResultListItemResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationRunDetailResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationRunListItemResponse;
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
      me.nghlong3004.vqc.api.evaluation.entity.EvaluationResult result) {
    return new EvaluationResultListItemResponse(
        result.getPublicId(),
        result.getTestCase().getPublicId(),
        result.getActualAnswer(),
        result.getJudgeScore(),
        result.getJudgeStatus(),
        result.getJudgeReason(),
        result.getLatencyMs(),
        result.getErrorMessage(),
        result.getCreatedAt());
  }
}
