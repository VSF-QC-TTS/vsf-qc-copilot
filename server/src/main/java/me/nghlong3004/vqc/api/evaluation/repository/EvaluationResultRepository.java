package me.nghlong3004.vqc.api.evaluation.repository;

import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationResult;
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;
import me.nghlong3004.vqc.api.review.enums.QcStatus;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, Long> {

  /**
   * Finds a result by public id and evaluation run creator.
   *
   * @param publicId public result identifier
   * @param createdBy evaluation run creator
   * @return {@link Optional} containing the matching result when present
   */
  Optional<EvaluationResult> findByPublicIdAndEvaluationRunCreatedBy(
      UUID publicId, User createdBy);

  /**
   * Finds evaluation results for a run.
   *
   * @param evaluationRunId internal run identifier
   * @param pageable page and sort request
   * @return page of matching {@link EvaluationResult} entities
   */
  Page<EvaluationResult> findByEvaluationRunId(Long evaluationRunId, Pageable pageable);

  /**
   * Finds evaluation results for a run filtered by judge status.
   *
   * @param evaluationRunId internal run identifier
   * @param judgeStatus judge status filter
   * @param pageable page and sort request
   * @return page of matching {@link EvaluationResult} entities
   */
  Page<EvaluationResult> findByEvaluationRunIdAndJudgeStatus(
      Long evaluationRunId, JudgeStatus judgeStatus, Pageable pageable);

  Page<EvaluationResult> findByEvaluationRunIdAndReviewDecisionIsNull(
      Long evaluationRunId, Pageable pageable);

  Page<EvaluationResult> findByEvaluationRunIdAndJudgeStatusAndReviewDecisionIsNull(
      Long evaluationRunId, JudgeStatus judgeStatus, Pageable pageable);

  Page<EvaluationResult> findByEvaluationRunIdAndReviewDecisionQcStatus(
      Long evaluationRunId, QcStatus qcStatus, Pageable pageable);

  Page<EvaluationResult> findByEvaluationRunIdAndJudgeStatusAndReviewDecisionQcStatus(
      Long evaluationRunId, JudgeStatus judgeStatus, QcStatus qcStatus, Pageable pageable);

  /**
   * Counts results by run ID and judge status.
   *
   * @param evaluationRunId internal run identifier
   * @param judgeStatus judge status
   * @return count of matching results
   */
  long countByEvaluationRunIdAndJudgeStatus(Long evaluationRunId, JudgeStatus judgeStatus);
}
