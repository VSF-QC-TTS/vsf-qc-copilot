package me.nghlong3004.vqc.api.evaluation.service;

import java.util.UUID;
import me.nghlong3004.vqc.api.evaluation.request.CreateEvaluationRunRequest;
import me.nghlong3004.vqc.api.evaluation.response.CreateEvaluationRunResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationRunPageResponse;
import org.springframework.data.domain.Pageable;

/**
 * Coordinates evaluation run use cases.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public interface EvaluationRunService {

  /**
   * Creates an evaluation run and queues it for async processing.
   *
   * @param projectPublicId public project identifier
   * @param request create evaluation run payload
   * @param username authenticated principal username/email
   * @return created evaluation run response with job reference
   */
  CreateEvaluationRunResponse createEvaluationRun(
      UUID projectPublicId, CreateEvaluationRunRequest request, String username);

  /**
   * Lists evaluation runs under a project owned by the authenticated user.
   *
   * @param projectPublicId public project identifier
   * @param pageable page and sort request
   * @param username authenticated principal username/email
   * @return page of evaluation runs
   */
  EvaluationRunPageResponse listEvaluationRuns(
      UUID projectPublicId, Pageable pageable, String username);
}
