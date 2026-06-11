package me.nghlong3004.vqc.api.evaluation.mapper;

import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
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
}
