package me.nghlong3004.vqc.api.evaluation.repository;

import me.nghlong3004.vqc.api.evaluation.entity.EvaluationResult;
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, Long> {

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
}
