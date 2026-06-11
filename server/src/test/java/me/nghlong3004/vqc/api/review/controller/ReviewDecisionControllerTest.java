package me.nghlong3004.vqc.api.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.exception.GlobalException;
import me.nghlong3004.vqc.api.review.enums.QcStatus;
import me.nghlong3004.vqc.api.review.request.UpsertReviewDecisionRequest;
import me.nghlong3004.vqc.api.review.response.ReviewDecisionResponse;
import me.nghlong3004.vqc.api.review.response.ReviewUserResponse;
import me.nghlong3004.vqc.api.review.service.ReviewDecisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@WebMvcTest(
    controllers = ReviewDecisionController.class,
    excludeAutoConfiguration = {
      OAuth2ClientAutoConfiguration.class,
      OAuth2ClientWebSecurityAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalException.class, ReviewDecisionControllerTest.MockBeans.class})
class ReviewDecisionControllerTest {

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void reset() {
    RecordingReviewDecisionService.reset();
  }

  @Test
  void upsertReviewDecisionReturnsResponse() throws Exception {
    RecordingReviewDecisionService.response =
        new ReviewDecisionResponse(
            UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
            UUID.fromString("b1b2c3d4-e5f6-7890-abcd-ef1234567890"),
            QcStatus.NEED_FIX,
            "Needs exact value.",
            new ReviewUserResponse(
                UUID.fromString("c1c2c3d4-e5f6-7890-abcd-ef1234567890"), "Long"),
            new ReviewUserResponse(
                UUID.fromString("d1d2c3d4-e5f6-7890-abcd-ef1234567890"), "QC Demo"),
            OffsetDateTime.parse("2026-06-11T10:00:00Z"),
            OffsetDateTime.parse("2026-06-11T10:00:00Z"));

    mockMvc
        .perform(
            put(
                    "/api/v1/evaluation-results/b1b2c3d4-e5f6-7890-abcd-ef1234567890/review-decision")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "qcStatus": "NEED_FIX",
                      "qcNote": "Needs exact value.",
                      "picBugUserPublicId": "c1c2c3d4-e5f6-7890-abcd-ef1234567890"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value("a1b2c3d4-e5f6-7890-abcd-ef1234567890"))
        .andExpect(jsonPath("$.evaluationResultPublicId").value("b1b2c3d4-e5f6-7890-abcd-ef1234567890"))
        .andExpect(jsonPath("$.qcStatus").value("NEED_FIX"))
        .andExpect(jsonPath("$.picBug.displayName").value("Long"))
        .andExpect(jsonPath("$.reviewedBy.displayName").value("QC Demo"));

    assertThat(RecordingReviewDecisionService.resultPublicId)
        .isEqualTo(UUID.fromString("b1b2c3d4-e5f6-7890-abcd-ef1234567890"));
    assertThat(RecordingReviewDecisionService.request.qcStatus()).isEqualTo(QcStatus.NEED_FIX);
    assertThat(RecordingReviewDecisionService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void upsertReviewDecisionReturnsValidationForMissingStatus() throws Exception {
    mockMvc
        .perform(
            put(
                    "/api/v1/evaluation-results/b1b2c3d4-e5f6-7890-abcd-ef1234567890/review-decision")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "qcNote": "Missing status"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    assertThat(RecordingReviewDecisionService.request).isNull();
  }

  @Test
  void upsertReviewDecisionReturnsValidationForInvalidResultId() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/evaluation-results/not-a-uuid/review-decision")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "qcStatus": "FAIL"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    assertThat(RecordingReviewDecisionService.resultPublicId).isNull();
  }

  @Test
  void getReviewDecisionReturnsResponse() throws Exception {
    RecordingReviewDecisionService.response =
        new ReviewDecisionResponse(
            null,
            UUID.fromString("b1b2c3d4-e5f6-7890-abcd-ef1234567890"),
            QcStatus.NOT_REVIEWED,
            null,
            null,
            null,
            null,
            null);

    mockMvc
        .perform(
            get(
                    "/api/v1/evaluation-results/b1b2c3d4-e5f6-7890-abcd-ef1234567890/review-decision")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").isEmpty())
        .andExpect(jsonPath("$.evaluationResultPublicId").value("b1b2c3d4-e5f6-7890-abcd-ef1234567890"))
        .andExpect(jsonPath("$.qcStatus").value("NOT_REVIEWED"));

    assertThat(RecordingReviewDecisionService.resultPublicId)
        .isEqualTo(UUID.fromString("b1b2c3d4-e5f6-7890-abcd-ef1234567890"));
    assertThat(RecordingReviewDecisionService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void getReviewDecisionReturnsValidationForInvalidResultId() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/evaluation-results/not-a-uuid/review-decision")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    assertThat(RecordingReviewDecisionService.resultPublicId).isNull();
  }

  @TestConfiguration
  static class MockBeans {
    @Bean
    ReviewDecisionService reviewDecisionService() {
      return new RecordingReviewDecisionService();
    }
  }

  static class RecordingReviewDecisionService implements ReviewDecisionService {
    static UUID resultPublicId;
    static UpsertReviewDecisionRequest request;
    static String username;
    static ReviewDecisionResponse response;

    static void reset() {
      resultPublicId = null;
      request = null;
      username = null;
      response = null;
    }

    @Override
    public ReviewDecisionResponse upsertReviewDecision(
        UUID resultPublicId, UpsertReviewDecisionRequest request, String username) {
      RecordingReviewDecisionService.resultPublicId = resultPublicId;
      RecordingReviewDecisionService.request = request;
      RecordingReviewDecisionService.username = username;
      return response;
    }

    @Override
    public ReviewDecisionResponse getReviewDecision(UUID resultPublicId, String username) {
      RecordingReviewDecisionService.resultPublicId = resultPublicId;
      RecordingReviewDecisionService.username = username;
      return response;
    }
  }
}
