package me.nghlong3004.vqc.api.evaluation.service;

import java.util.UUID;
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;
import me.nghlong3004.vqc.api.evaluation.request.CreateEvaluationRunRequest;
import me.nghlong3004.vqc.api.evaluation.request.QuickEvaluateRequest;
import me.nghlong3004.vqc.api.evaluation.response.CreateEvaluationRunResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationResultPageResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationRunDetailResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationRunPageResponse;
import me.nghlong3004.vqc.api.review.enums.QcStatus;
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
   */
  CreateEvaluationRunResponse createEvaluationRun(
      UUID projectPublicId, CreateEvaluationRunRequest request, String username);

  /**
   * Lists evaluation runs under a project owned by the authenticated user.
   */
  EvaluationRunPageResponse listEvaluationRuns(
      UUID projectPublicId, Pageable pageable, String username);

  /**
   * Gets a single evaluation run detail by public identifier.
   *
   * @param runPublicId public run identifier
   * @param username authenticated principal username/email
   * @return evaluation run detail response
   */
  EvaluationRunDetailResponse getEvaluationRun(UUID runPublicId, String username);

  /**
   * Lists evaluation results for a run, optionally filtered by judge status.
   *
   * @param runPublicId public run identifier
   * @param judgeStatus optional judge status filter
   * @param pageable page and sort request
   * @param username authenticated principal username/email
   * @return page of evaluation results
   */
  EvaluationResultPageResponse listEvaluationResults(
      UUID runPublicId, JudgeStatus judgeStatus, QcStatus qcStatus, Pageable pageable, String username);

  /**
   * Lists job events for an evaluation run.
   *
   * @param runPublicId public run identifier
   * @param username authenticated principal username/email
   * @return chronological list of job events
   */
  java.util.List<me.nghlong3004.vqc.api.job.response.JobEventResponse> listEvaluationRunEvents(
      UUID runPublicId, String username);

  /**
   * Quick evaluate with auto-resolve: null fields are resolved to the sole candidate in the
   * project. Returns 422 if a field cannot be auto-resolved (0 or >1 candidates).
   *
   * @param projectPublicId project public identifier
   * @param request quick evaluate request with optional fields
   * @param username authenticated principal username/email
   * @return async job tracking response (202-style)
   */
  CreateEvaluationRunResponse quickEvaluate(
      UUID projectPublicId, QuickEvaluateRequest request, String username);
}
