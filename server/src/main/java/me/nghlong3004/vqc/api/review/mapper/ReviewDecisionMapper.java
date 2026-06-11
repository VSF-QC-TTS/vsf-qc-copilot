package me.nghlong3004.vqc.api.review.mapper;

import me.nghlong3004.vqc.api.review.entity.ReviewDecision;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationResult;
import me.nghlong3004.vqc.api.review.enums.QcStatus;
import me.nghlong3004.vqc.api.review.response.ReviewDecisionResponse;
import me.nghlong3004.vqc.api.review.response.ReviewUserResponse;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Component
public class ReviewDecisionMapper {

  /**
   * Maps a review decision entity to an API response.
   *
   * @param decision review decision entity
   * @return response payload
   */
  public ReviewDecisionResponse toResponse(ReviewDecision decision) {
    return new ReviewDecisionResponse(
        decision.getPublicId(),
        decision.getEvaluationResult().getPublicId(),
        decision.getQcStatus(),
        decision.getQcNote(),
        toUserResponse(decision.getPicBugUser()),
        toUserResponse(decision.getReviewedBy()),
        decision.getReviewedAt(),
        decision.getUpdatedAt());
  }

  /**
   * Maps an evaluation result with no persisted review decision to the default review state.
   *
   * @param result evaluation result entity
   * @return default not-reviewed response
   */
  public ReviewDecisionResponse toNotReviewedResponse(EvaluationResult result) {
    return new ReviewDecisionResponse(
        null,
        result.getPublicId(),
        QcStatus.NOT_REVIEWED,
        null,
        null,
        null,
        null,
        null);
  }

  private ReviewUserResponse toUserResponse(User user) {
    if (user == null) {
      return null;
    }
    return new ReviewUserResponse(user.getPublicId(), user.getDisplayName());
  }
}
