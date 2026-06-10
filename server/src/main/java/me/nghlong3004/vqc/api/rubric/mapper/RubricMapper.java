package me.nghlong3004.vqc.api.rubric.mapper;

import java.util.List;
import me.nghlong3004.vqc.api.rubric.entity.Rubric;
import me.nghlong3004.vqc.api.rubric.entity.RubricCriterion;
import me.nghlong3004.vqc.api.rubric.entity.RubricVersion;
import me.nghlong3004.vqc.api.rubric.response.RubricCriterionResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricListItemResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricVersionListItemResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricVersionResponse;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Component
public class RubricMapper {

  public RubricResponse toResponse(Rubric rubric) {
    return new RubricResponse(
        rubric.getPublicId(),
        rubric.getProject().getPublicId(),
        rubric.getName(),
        rubric.getDescription(),
        rubric.getCurrentVersion(),
        rubric.getStatus(),
        rubric.getCreatedAt(),
        rubric.getUpdatedAt(),
        rubric.getArchivedAt());
  }

  public RubricListItemResponse toListItemResponse(Rubric rubric) {
    return new RubricListItemResponse(
        rubric.getPublicId(),
        rubric.getProject().getPublicId(),
        rubric.getName(),
        rubric.getCurrentVersion(),
        rubric.getStatus(),
        rubric.getCreatedAt());
  }

  public RubricVersionResponse toVersionResponse(
      RubricVersion rubricVersion, List<RubricCriterion> criteria) {
    return new RubricVersionResponse(
        rubricVersion.getPublicId(),
        rubricVersion.getRubric().getPublicId(),
        rubricVersion.getVersion(),
        rubricVersion.getStatus(),
        rubricVersion.getCreatedAt(),
        rubricVersion.getPublishedAt(),
        criteria.stream().map(this::toCriterionResponse).toList());
  }

  public RubricVersionListItemResponse toVersionListItemResponse(
      RubricVersion rubricVersion, long totalCriteria) {
    return new RubricVersionListItemResponse(
        rubricVersion.getPublicId(),
        rubricVersion.getRubric().getPublicId(),
        rubricVersion.getVersion(),
        rubricVersion.getStatus(),
        totalCriteria,
        rubricVersion.getCreatedAt(),
        rubricVersion.getPublishedAt());
  }

  public RubricCriterionResponse toCriterionResponse(RubricCriterion criterion) {
    return new RubricCriterionResponse(
        criterion.getPublicId(),
        criterion.getRubricVersion().getPublicId(),
        criterion.getName(),
        criterion.getDescription(),
        criterion.getWeight(),
        criterion.getPassCondition(),
        criterion.getFailCondition(),
        criterion.getJudgeInstruction(),
        criterion.getMetricKey(),
        Boolean.TRUE.equals(criterion.getCritical()),
        criterion.getSortOrder(),
        criterion.getCreatedAt(),
        criterion.getUpdatedAt());
  }
}
