package me.nghlong3004.vqc.api.review.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationResult;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;
import me.nghlong3004.vqc.api.evaluation.repository.EvaluationResultRepository;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.review.entity.ReviewDecision;
import me.nghlong3004.vqc.api.review.enums.QcStatus;
import me.nghlong3004.vqc.api.review.mapper.ReviewDecisionMapper;
import me.nghlong3004.vqc.api.review.repository.ReviewDecisionRepository;
import me.nghlong3004.vqc.api.review.request.UpsertReviewDecisionRequest;
import me.nghlong3004.vqc.api.review.response.ReviewDecisionResponse;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.enums.UserStatus;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
class ReviewDecisionServiceImplTest {

  @Test
  void upsertReviewDecisionCreatesNewDecision() {
    User reviewer = user(1L, "QC Demo", UserStatus.ACTIVE);
    User picBugUser = user(2L, "Long", UserStatus.ACTIVE);
    EvaluationResult result = result(reviewer);
    AtomicReference<ReviewDecision> saved = new AtomicReference<>();
    ReviewDecisionServiceImpl service =
        service(reviewer, Optional.of(picBugUser), Optional.of(result), Optional.empty(), saved);

    ReviewDecisionResponse response =
        service.upsertReviewDecision(
            result.getPublicId(),
            new UpsertReviewDecisionRequest(QcStatus.NEED_FIX, "  Needs exact value.  ", picBugUser.getPublicId()),
            reviewer.getUsername());

    assertThat(response.qcStatus()).isEqualTo(QcStatus.NEED_FIX);
    assertThat(response.qcNote()).isEqualTo("Needs exact value.");
    assertThat(response.picBug().publicId()).isEqualTo(picBugUser.getPublicId());
    assertThat(response.reviewedBy().publicId()).isEqualTo(reviewer.getPublicId());
    assertThat(saved.get().getEvaluationResult()).isEqualTo(result);
  }

  @Test
  void upsertReviewDecisionUpdatesExistingDecision() {
    User reviewer = user(1L, "QC Demo", UserStatus.ACTIVE);
    User picBugUser = user(2L, "Long", UserStatus.ACTIVE);
    EvaluationResult result = result(reviewer);
    ReviewDecision existing =
        ReviewDecision.builder()
            .id(1L)
            .publicId(UUID.randomUUID())
            .evaluationResult(result)
            .qcStatus(QcStatus.FAIL)
            .qcNote("old")
            .reviewedBy(reviewer)
            .reviewedAt(OffsetDateTime.now().minusHours(1))
            .updatedAt(OffsetDateTime.now().minusHours(1))
            .build();
    AtomicReference<ReviewDecision> saved = new AtomicReference<>();
    ReviewDecisionServiceImpl service =
        service(reviewer, Optional.of(picBugUser), Optional.of(result), Optional.of(existing), saved);

    ReviewDecisionResponse response =
        service.upsertReviewDecision(
            result.getPublicId(),
            new UpsertReviewDecisionRequest(QcStatus.PASS, null, picBugUser.getPublicId()),
            reviewer.getUsername());

    assertThat(response.publicId()).isEqualTo(existing.getPublicId());
    assertThat(response.qcStatus()).isEqualTo(QcStatus.PASS);
    assertThat(response.qcNote()).isNull();
    assertThat(saved.get()).isSameAs(existing);
  }

  @Test
  void rejectsNotReviewedStatusForWrites() {
    User reviewer = user(1L, "QC Demo", UserStatus.ACTIVE);
    EvaluationResult result = result(reviewer);
    ReviewDecisionServiceImpl service =
        service(reviewer, Optional.empty(), Optional.of(result), Optional.empty(), new AtomicReference<>());

    assertThatThrownBy(
            () ->
                service.upsertReviewDecision(
                    result.getPublicId(),
                    new UpsertReviewDecisionRequest(QcStatus.NOT_REVIEWED, null, null),
                    reviewer.getUsername()))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("REVIEW_DECISION_STATUS_INVALID");
  }

  @Test
  void rejectsMissingResult() {
    User reviewer = user(1L, "QC Demo", UserStatus.ACTIVE);
    ReviewDecisionServiceImpl service =
        service(reviewer, Optional.empty(), Optional.empty(), Optional.empty(), new AtomicReference<>());

    assertThatThrownBy(
            () ->
                service.upsertReviewDecision(
                    UUID.randomUUID(),
                    new UpsertReviewDecisionRequest(QcStatus.FAIL, null, null),
                    reviewer.getUsername()))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("EVALUATION_RESULT_NOT_FOUND");
  }

  @Test
  void rejectsInactivePicBugUser() {
    User reviewer = user(1L, "QC Demo", UserStatus.ACTIVE);
    User inactivePic = user(2L, "Disabled", UserStatus.DISABLED);
    EvaluationResult result = result(reviewer);
    ReviewDecisionServiceImpl service =
        service(reviewer, Optional.of(inactivePic), Optional.of(result), Optional.empty(), new AtomicReference<>());

    assertThatThrownBy(
            () ->
                service.upsertReviewDecision(
                    result.getPublicId(),
                    new UpsertReviewDecisionRequest(QcStatus.NEED_FIX, null, inactivePic.getPublicId()),
                    reviewer.getUsername()))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("PIC_BUG_USER_NOT_FOUND");
  }

  @Test
  void rejectsMissingReviewer() {
    ReviewDecisionServiceImpl service =
        new ReviewDecisionServiceImpl(
            ignored(ReviewDecisionRepository.class),
            ignored(EvaluationResultRepository.class),
            userRepository(Optional.empty(), Optional.empty()),
            new ReviewDecisionMapper());

    assertThatThrownBy(
            () ->
                service.upsertReviewDecision(
                    UUID.randomUUID(),
                    new UpsertReviewDecisionRequest(QcStatus.FAIL, null, null),
                    "missing@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("USER_NOT_FOUND");
  }

  private ReviewDecisionServiceImpl service(
      User reviewer,
      Optional<User> picBugUser,
      Optional<EvaluationResult> result,
      Optional<ReviewDecision> existingDecision,
      AtomicReference<ReviewDecision> savedDecision) {
    return new ReviewDecisionServiceImpl(
        reviewDecisionRepository(existingDecision, savedDecision),
        evaluationResultRepository(result),
        userRepository(Optional.of(reviewer), picBugUser),
        new ReviewDecisionMapper());
  }

  private ReviewDecisionRepository reviewDecisionRepository(
      Optional<ReviewDecision> existingDecision, AtomicReference<ReviewDecision> savedDecision) {
    return proxy(
        ReviewDecisionRepository.class,
        (proxy, method, args) -> {
          if ("findByEvaluationResult".equals(method.getName())) return existingDecision;
          if ("save".equals(method.getName())) {
            savedDecision.set((ReviewDecision) args[0]);
            return args[0];
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private EvaluationResultRepository evaluationResultRepository(Optional<EvaluationResult> result) {
    return proxy(
        EvaluationResultRepository.class,
        (proxy, method, args) -> {
          if ("findByPublicIdAndEvaluationRunCreatedBy".equals(method.getName())) return result;
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private UserRepository userRepository(Optional<User> reviewer, Optional<User> picBugUser) {
    return proxy(
        UserRepository.class,
        (proxy, method, args) -> {
          if ("findByUsername".equals(method.getName())) return reviewer;
          if ("findByPublicId".equals(method.getName())) return picBugUser;
          throw new UnsupportedOperationException(method.getName());
        });
  }

  @SuppressWarnings("unchecked")
  private <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
  }

  private <T> T ignored(Class<T> type) {
    return proxy(type, (proxy, method, args) -> {
      throw new UnsupportedOperationException(method.getName());
    });
  }

  private EvaluationResult result(User creator) {
    Project project =
        Project.builder().id(1L).publicId(UUID.randomUUID()).name("Project").createdBy(creator).build();
    EvaluationRun run =
        EvaluationRun.builder()
            .id(1L)
            .publicId(UUID.randomUUID())
            .project(project)
            .createdBy(creator)
            .build();
    return EvaluationResult.builder()
        .id(1L)
        .publicId(UUID.randomUUID())
        .evaluationRun(run)
        .judgeStatus(JudgeStatus.FAIL)
        .build();
  }

  private User user(Long id, String displayName, UserStatus status) {
    String normalized = displayName.toLowerCase().replace(" ", ".") + "@example.com";
    return User.builder()
        .id(id)
        .publicId(UUID.randomUUID())
        .username(normalized)
        .passwordHash("hash")
        .displayName(displayName)
        .status(status)
        .build();
  }
}
