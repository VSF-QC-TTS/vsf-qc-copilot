package me.nghlong3004.vqc.api.review.repository;

import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationResult;
import me.nghlong3004.vqc.api.review.entity.ReviewDecision;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public interface ReviewDecisionRepository extends JpaRepository<ReviewDecision, Long> {

  /**
   * Finds a review decision by evaluation result.
   *
   * @param evaluationResult reviewed evaluation result
   * @return {@link Optional} containing the matching decision when present
   */
  Optional<ReviewDecision> findByEvaluationResult(EvaluationResult evaluationResult);

  /**
   * Finds a review decision by public id.
   *
   * @param publicId public review decision identifier
   * @return {@link Optional} containing the matching decision when present
   */
  Optional<ReviewDecision> findByPublicId(UUID publicId);

  /**
   * Finds a review decision by public id and evaluation run creator.
   *
   * @param publicId public review decision identifier
   * @param createdBy evaluation run creator
   * @return {@link Optional} containing the matching decision when present
   */
  Optional<ReviewDecision> findByPublicIdAndEvaluationResultEvaluationRunCreatedBy(
      UUID publicId, User createdBy);
}
