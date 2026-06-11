package me.nghlong3004.vqc.api.review.service;

import java.util.UUID;
import me.nghlong3004.vqc.api.review.request.UpsertReviewDecisionRequest;
import me.nghlong3004.vqc.api.review.response.ReviewDecisionResponse;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public interface ReviewDecisionService {

  /**
   * Creates or updates a review decision for an evaluation result.
   *
   * @param resultPublicId evaluation result public identifier
   * @param request review decision payload
   * @param username authenticated username/email
   * @return review decision response
   */
  ReviewDecisionResponse upsertReviewDecision(
      UUID resultPublicId, UpsertReviewDecisionRequest request, String username);

  /**
   * Gets a review decision for an evaluation result.
   *
   * @param resultPublicId evaluation result public identifier
   * @param username authenticated username/email
   * @return persisted decision or default NOT_REVIEWED response
   */
  ReviewDecisionResponse getReviewDecision(UUID resultPublicId, String username);
}
