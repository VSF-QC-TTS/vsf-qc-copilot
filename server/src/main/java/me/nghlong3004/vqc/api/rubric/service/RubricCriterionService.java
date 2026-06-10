package me.nghlong3004.vqc.api.rubric.service;

import java.util.UUID;
import me.nghlong3004.vqc.api.rubric.request.CreateRubricCriterionRequest;
import me.nghlong3004.vqc.api.rubric.request.UpdateRubricCriterionRequest;
import me.nghlong3004.vqc.api.rubric.response.RubricCriterionPageResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricCriterionResponse;
import org.springframework.data.domain.Pageable;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface RubricCriterionService {

  RubricCriterionResponse createCriterion(
      UUID rubricVersionPublicId, CreateRubricCriterionRequest request, String username);

  RubricCriterionPageResponse listCriteria(
      UUID rubricVersionPublicId, Pageable pageable, String username);

  RubricCriterionResponse updateCriterion(
      UUID criterionPublicId, UpdateRubricCriterionRequest request, String username);

  void deleteCriterion(UUID criterionPublicId, String username);
}
