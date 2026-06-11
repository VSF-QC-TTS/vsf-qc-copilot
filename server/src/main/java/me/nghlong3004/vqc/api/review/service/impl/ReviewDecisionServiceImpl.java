package me.nghlong3004.vqc.api.review.service.impl;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationResult;
import me.nghlong3004.vqc.api.evaluation.repository.EvaluationResultRepository;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.review.entity.ReviewDecision;
import me.nghlong3004.vqc.api.review.enums.QcStatus;
import me.nghlong3004.vqc.api.review.mapper.ReviewDecisionMapper;
import me.nghlong3004.vqc.api.review.repository.ReviewDecisionRepository;
import me.nghlong3004.vqc.api.review.request.UpsertReviewDecisionRequest;
import me.nghlong3004.vqc.api.review.response.ReviewDecisionResponse;
import me.nghlong3004.vqc.api.review.service.ReviewDecisionService;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.enums.UserStatus;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewDecisionServiceImpl implements ReviewDecisionService {

  private final ReviewDecisionRepository reviewDecisionRepository;
  private final EvaluationResultRepository evaluationResultRepository;
  private final UserRepository userRepository;
  private final ReviewDecisionMapper reviewDecisionMapper;

  @Override
  @Transactional
  public ReviewDecisionResponse upsertReviewDecision(
      UUID resultPublicId, UpsertReviewDecisionRequest request, String username) {
    User reviewer = findReviewer(username);
    validateWritableStatus(request.qcStatus());
    EvaluationResult result = findResult(resultPublicId, reviewer);
    User picBugUser = findPicBugUser(request.picBugUserPublicId());

    ReviewDecision decision =
        reviewDecisionRepository
            .findByEvaluationResult(result)
            .orElseGet(() -> ReviewDecision.builder().evaluationResult(result).build());
    decision.applyDecision(request.qcStatus(), request.qcNote(), picBugUser, reviewer);
    ReviewDecision saved = reviewDecisionRepository.save(decision);

    log.info(
        "Upserted review decision {} for result {} by user {} with status {}",
        saved.getPublicId(),
        result.getPublicId(),
        reviewer.getPublicId(),
        saved.getQcStatus());
    return reviewDecisionMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public ReviewDecisionResponse getReviewDecision(UUID resultPublicId, String username) {
    User reviewer = findReviewer(username);
    EvaluationResult result = findResult(resultPublicId, reviewer);
    return reviewDecisionRepository
        .findByEvaluationResult(result)
        .map(reviewDecisionMapper::toResponse)
        .orElseGet(() -> reviewDecisionMapper.toNotReviewedResponse(result));
  }

  private User findReviewer(String username) {
    return userRepository
        .findByUsername(username.trim().toLowerCase())
        .orElseThrow(() -> new ResourceException(ErrorCode.USER_NOT_FOUND));
  }

  private EvaluationResult findResult(UUID resultPublicId, User reviewer) {
    return evaluationResultRepository
        .findByPublicIdAndEvaluationRunCreatedBy(resultPublicId, reviewer)
        .orElseThrow(() -> new ResourceException(ErrorCode.EVALUATION_RESULT_NOT_FOUND));
  }

  private User findPicBugUser(UUID picBugUserPublicId) {
    if (picBugUserPublicId == null) {
      return null;
    }
    return userRepository
        .findByPublicId(picBugUserPublicId)
        .filter(user -> user.getStatus() == UserStatus.ACTIVE)
        .orElseThrow(() -> new ResourceException(ErrorCode.PIC_BUG_USER_NOT_FOUND));
  }

  private void validateWritableStatus(QcStatus qcStatus) {
    if (qcStatus == null || !qcStatus.isWritable()) {
      throw new ResourceException(ErrorCode.REVIEW_DECISION_STATUS_INVALID);
    }
  }
}
