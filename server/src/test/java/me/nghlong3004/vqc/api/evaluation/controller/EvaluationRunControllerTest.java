package me.nghlong3004.vqc.api.evaluation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import me.nghlong3004.vqc.api.evaluation.enums.EvaluationRunStatus;
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;
import me.nghlong3004.vqc.api.evaluation.request.CreateEvaluationRunRequest;
import me.nghlong3004.vqc.api.evaluation.request.QuickEvaluateRequest;
import me.nghlong3004.vqc.api.evaluation.response.CreateEvaluationRunResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationResultListItemResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationResultPageResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationRunDetailResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationRunListItemResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationRunPageResponse;
import me.nghlong3004.vqc.api.evaluation.service.EvaluationRunService;
import me.nghlong3004.vqc.api.exception.GlobalException;
import me.nghlong3004.vqc.api.job.response.JobEventResponse;
import me.nghlong3004.vqc.api.review.enums.QcStatus;
import me.nghlong3004.vqc.api.review.response.ReviewUserResponse;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@WebMvcTest(
    controllers = EvaluationRunController.class,
    excludeAutoConfiguration = {
      OAuth2ClientAutoConfiguration.class,
      OAuth2ClientWebSecurityAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalException.class, EvaluationRunControllerTest.MockBeans.class})
class EvaluationRunControllerTest {

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void resetTestDoubles() {
    RecordingEvaluationRunService.reset();
  }

  // ── POST create evaluation run ──

  @Test
  void createEvaluationRunReturnsAccepted() throws Exception {
    RecordingEvaluationRunService.createResponse =
        new CreateEvaluationRunResponse(
            UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
            UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901"),
            "PENDING",
            "Evaluation run queued");

    mockMvc
        .perform(
            post("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/evaluation-runs")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "datasetPublicId": "d1d2d3d4-e5f6-7890-abcd-ef1234567890",
                      "rubricVersionPublicId": "a1a2a3a4-e5f6-7890-abcd-ef1234567890",
                      "targetConnectorPublicId": "c1c2c3c4-e5f6-7890-abcd-ef1234567890",
                      "judgeModelPublicId": "8d2f6a2a-4974-4e9c-83ad-f2e1e58d39f0",
                      "maxConcurrency": 2
                    }
                    """))
        .andExpect(status().isAccepted())
        .andExpect(
            jsonPath("$.runPublicId").value("a1b2c3d4-e5f6-7890-abcd-ef1234567890"))
        .andExpect(
            jsonPath("$.jobPublicId").value("b2c3d4e5-f6a7-8901-bcde-f12345678901"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.message").value("Evaluation run queued"));

    assertThat(RecordingEvaluationRunService.projectPublicId)
        .isEqualTo(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"));
    assertThat(RecordingEvaluationRunService.createRequest.maxConcurrency()).isEqualTo(2);
    assertThat(RecordingEvaluationRunService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void createEvaluationRunReturnsValidationProblemDetails() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/evaluation-runs")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "maxConcurrency": 2
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(
            jsonPath("$.instance")
                .value(
                    "/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/evaluation-runs"));

    assertThat(RecordingEvaluationRunService.createRequest).isNull();
  }

  // ── GET list evaluation runs ──

  @Test
  void listEvaluationRunsReturnsPage() throws Exception {
    RecordingEvaluationRunService.pageResponse =
        new EvaluationRunPageResponse(
            List.of(
                new EvaluationRunListItemResponse(
                    UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
                    UUID.fromString("d1d2d3d4-e5f6-7890-abcd-ef1234567890"),
                    "Test Dataset",
                    UUID.fromString("a1a2a3a4-e5f6-7890-abcd-ef1234567890"),
                    "Test Rubric",
                    UUID.fromString("c1c2c3c4-e5f6-7890-abcd-ef1234567890"),
                    UUID.fromString("8d2f6a2a-4974-4e9c-83ad-f2e1e58d39f0"),
                    EvaluationRunStatus.COMPLETED,
                    10,
                    10,
                    8,
                    2,
                    OffsetDateTime.parse("2026-06-11T10:00:00Z"))),
            0,
            20,
            1,
            1);

    mockMvc
        .perform(
            get("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/evaluation-runs")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.items[0].publicId")
                .value("a1b2c3d4-e5f6-7890-abcd-ef1234567890"))
        .andExpect(jsonPath("$.items[0].status").value("COMPLETED"))
        .andExpect(jsonPath("$.items[0].totalCases").value(10))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.totalItems").value(1));

    assertThat(RecordingEvaluationRunService.projectPublicId)
        .isEqualTo(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"));
    assertThat(RecordingEvaluationRunService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void listEvaluationRunsReturnsValidationForInvalidProjectId() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/projects/not-a-uuid/evaluation-runs")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    assertThat(RecordingEvaluationRunService.projectPublicId).isNull();
  }

  // ── GET evaluation run detail ──

  @Test
  void getEvaluationRunReturnsDetail() throws Exception {
    RecordingEvaluationRunService.detailResponse =
        new EvaluationRunDetailResponse(
            UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
            UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"),
            UUID.fromString("d1d2d3d4-e5f6-7890-abcd-ef1234567890"),
            "Test Dataset",
            UUID.fromString("a1a2a3a4-e5f6-7890-abcd-ef1234567890"),
            "Test Rubric",
            1,
            UUID.fromString("c1c2c3c4-e5f6-7890-abcd-ef1234567890"),
            "Test Connector",
            UUID.fromString("8d2f6a2a-4974-4e9c-83ad-f2e1e58d39f0"),
            "Test Judge",
            UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901"),
            EvaluationRunStatus.COMPLETED,
            "Test Description",
            10,
            10,
            8,
            2,
            0,
            0,
            BigDecimal.valueOf(0.8),
            1,
            OffsetDateTime.parse("2026-06-11T10:00:00Z"),
            OffsetDateTime.parse("2026-06-11T10:05:00Z"),
            OffsetDateTime.parse("2026-06-11T10:00:00Z"),
            OffsetDateTime.parse("2026-06-11T10:05:00Z"));

    mockMvc
        .perform(
            get("/api/v1/evaluation-runs/a1b2c3d4-e5f6-7890-abcd-ef1234567890")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.publicId").value("a1b2c3d4-e5f6-7890-abcd-ef1234567890"))
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.totalCases").value(10))
        .andExpect(jsonPath("$.maxConcurrency").value(1));

    assertThat(RecordingEvaluationRunService.runPublicId)
        .isEqualTo(UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
    assertThat(RecordingEvaluationRunService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void getEvaluationRunReturnsValidationForInvalidId() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/evaluation-runs/not-a-uuid")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    assertThat(RecordingEvaluationRunService.runPublicId).isNull();
  }

  // ── GET evaluation results ──

  @Test
  void listEvaluationResultsReturnsPage() throws Exception {
    RecordingEvaluationRunService.resultPageResponse =
        new EvaluationResultPageResponse(
            List.of(
                new EvaluationResultListItemResponse(
                    UUID.fromString("e1e2e3e4-e5f6-7890-abcd-ef1234567890"),
                    UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
                    UUID.fromString("b1b2b3b4-e5f6-7890-abcd-ef1234567890"),
                    "HEALTH_001",
                    "How many steps?",
                    "{\"date\":\"2026-06-11\"}",
                    "8,200 steps",
                    "Test answer",
                    BigDecimal.valueOf(0.9),
                    JudgeStatus.PASS,
                    "Correct answer",
                    120,
                    null,
                    "[]",
                    QcStatus.NEED_FIX,
                    "Needs exact value.",
                    new ReviewUserResponse(
                        UUID.fromString("d1d2c3d4-e5f6-7890-abcd-ef1234567890"), "Long"),
                    OffsetDateTime.parse("2026-06-11T10:01:00Z"))),
            0,
            20,
            1,
            1);

    mockMvc
        .perform(
            get("/api/v1/evaluation-runs/a1b2c3d4-e5f6-7890-abcd-ef1234567890/results")
                .param("judgeStatus", "PASS")
                .param("qcStatus", "NEED_FIX")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.items[0].publicId")
                .value("e1e2e3e4-e5f6-7890-abcd-ef1234567890"))
        .andExpect(jsonPath("$.items[0].judgeStatus").value("PASS"))
        .andExpect(jsonPath("$.items[0].externalId").value("HEALTH_001"))
        .andExpect(jsonPath("$.items[0].question").value("How many steps?"))
        .andExpect(jsonPath("$.items[0].qcStatus").value("NEED_FIX"))
        .andExpect(jsonPath("$.items[0].picBug.displayName").value("Long"))
        .andExpect(jsonPath("$.items[0].judgeScore").value(0.9))
        .andExpect(jsonPath("$.items[0].latencyMs").value(120))
        .andExpect(jsonPath("$.totalItems").value(1));

    assertThat(RecordingEvaluationRunService.runPublicId)
        .isEqualTo(UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
    assertThat(RecordingEvaluationRunService.judgeStatus).isEqualTo(JudgeStatus.PASS);
    assertThat(RecordingEvaluationRunService.qcStatus).isEqualTo(QcStatus.NEED_FIX);
  }

  // ── GET evaluation events ──

  @Test
  void listEvaluationRunEventsReturnsList() throws Exception {
    RecordingEvaluationRunService.eventsResponse =
        List.of(
            new JobEventResponse(
                UUID.fromString("e01e01ef-e5f6-7890-abcd-ef1234567890"),
                "STARTED",
                null,
                OffsetDateTime.parse("2026-06-11T10:00:00Z")));

    mockMvc
        .perform(
            get("/api/v1/evaluation-runs/a1b2c3d4-e5f6-7890-abcd-ef1234567890/events")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].publicId").value("e01e01ef-e5f6-7890-abcd-ef1234567890"))
        .andExpect(jsonPath("$[0].eventType").value("STARTED"));

    assertThat(RecordingEvaluationRunService.runPublicId)
        .isEqualTo(UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
  }

  // ── MockBeans + RecordingService ──

  @TestConfiguration
  static class MockBeans {

    @Bean
    EvaluationRunService evaluationRunService() {
      return new RecordingEvaluationRunService();
    }
  }

  static class RecordingEvaluationRunService implements EvaluationRunService {

    static UUID projectPublicId;
    static UUID runPublicId;
    static CreateEvaluationRunRequest createRequest;
    static QuickEvaluateRequest quickEvaluateRequest;
    static JudgeStatus judgeStatus;
    static QcStatus qcStatus;
    static Pageable pageable;
    static String username;
    static CreateEvaluationRunResponse createResponse;
    static EvaluationRunPageResponse pageResponse;
    static EvaluationRunDetailResponse detailResponse;
    static EvaluationResultPageResponse resultPageResponse;
    static List<JobEventResponse> eventsResponse;

    static void reset() {
      projectPublicId = null;
      runPublicId = null;
      createRequest = null;
      quickEvaluateRequest = null;
      judgeStatus = null;
      qcStatus = null;
      pageable = null;
      username = null;
      createResponse = null;
      pageResponse = null;
      detailResponse = null;
      resultPageResponse = null;
      eventsResponse = null;
    }

    @Override
    public CreateEvaluationRunResponse createEvaluationRun(
        UUID projectPublicId, CreateEvaluationRunRequest request, String username) {
      RecordingEvaluationRunService.projectPublicId = projectPublicId;
      RecordingEvaluationRunService.createRequest = request;
      RecordingEvaluationRunService.username = username;
      return createResponse;
    }

    @Override
    public EvaluationRunPageResponse listEvaluationRuns(
        UUID projectPublicId, Pageable pageable, String username) {
      RecordingEvaluationRunService.projectPublicId = projectPublicId;
      RecordingEvaluationRunService.pageable = pageable;
      RecordingEvaluationRunService.username = username;
      return pageResponse;
    }

    @Override
    public EvaluationRunDetailResponse getEvaluationRun(UUID runPublicId, String username) {
      RecordingEvaluationRunService.runPublicId = runPublicId;
      RecordingEvaluationRunService.username = username;
      return detailResponse;
    }

    @Override
    public EvaluationResultPageResponse listEvaluationResults(
        UUID runPublicId, JudgeStatus judgeStatus, QcStatus qcStatus, Pageable pageable, String username) {
      RecordingEvaluationRunService.runPublicId = runPublicId;
      RecordingEvaluationRunService.judgeStatus = judgeStatus;
      RecordingEvaluationRunService.qcStatus = qcStatus;
      RecordingEvaluationRunService.pageable = pageable;
      RecordingEvaluationRunService.username = username;
      return resultPageResponse;
    }

    @Override
    public List<JobEventResponse> listEvaluationRunEvents(UUID runPublicId, String username) {
      RecordingEvaluationRunService.runPublicId = runPublicId;
      RecordingEvaluationRunService.username = username;
      return eventsResponse;
    }

    @Override
    public CreateEvaluationRunResponse quickEvaluate(
        UUID projectPublicId, QuickEvaluateRequest request, String username) {
      RecordingEvaluationRunService.projectPublicId = projectPublicId;
      RecordingEvaluationRunService.quickEvaluateRequest = request;
      RecordingEvaluationRunService.username = username;
      return createResponse;
    }
  }
}
