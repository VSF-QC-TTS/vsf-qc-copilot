package me.nghlong3004.vqc.api.testcase.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import me.nghlong3004.vqc.api.exception.GlobalException;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;
import me.nghlong3004.vqc.api.testcase.request.CreateTestCaseRequest;
import me.nghlong3004.vqc.api.testcase.response.TestCaseResponse;
import me.nghlong3004.vqc.api.testcase.service.TestCaseService;
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
 * @since 6/10/2026
 */
@WebMvcTest(
    controllers = TestCaseController.class,
    excludeAutoConfiguration = {
      OAuth2ClientAutoConfiguration.class,
      OAuth2ClientWebSecurityAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalException.class, TestCaseControllerTest.MockBeans.class})
class TestCaseControllerTest {

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void resetTestDoubles() {
    RecordingTestCaseService.reset();
  }

  @Test
  void createTestCaseReturnsCreatedTestCase() throws Exception {
    RecordingTestCaseService.testCaseResponse =
        new TestCaseResponse(
            UUID.fromString("b4788db3-6cf3-47df-8ae1-4c73dbb7d0a8"),
            UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"),
            "HEALTH_001",
            "How many steps did I walk today?",
            Map.of("steps", 8200, "date", "2026-06-08"),
            "The user walked 8,200 steps today.",
            Map.of("userId", "demo-user-1"),
            TestCaseStatus.ACTIVE,
            1,
            OffsetDateTime.parse("2026-06-08T10:30:00Z"),
            OffsetDateTime.parse("2026-06-08T10:30:00Z"));

    mockMvc
        .perform(
            post("/api/v1/datasets/0f6d90c2-7410-4db2-86be-8adfd3140f63/test-cases")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "externalId": "HEALTH_001",
                      "question": "How many steps did I walk today?",
                      "precondition": {
                        "steps": 8200,
                        "date": "2026-06-08"
                      },
                      "groundTruth": "The user walked 8,200 steps today.",
                      "metadata": {
                        "userId": "demo-user-1"
                      },
                      "status": "ACTIVE",
                      "sortOrder": 1
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.publicId").value("b4788db3-6cf3-47df-8ae1-4c73dbb7d0a8"))
        .andExpect(
            jsonPath("$.datasetPublicId").value("0f6d90c2-7410-4db2-86be-8adfd3140f63"))
        .andExpect(jsonPath("$.externalId").value("HEALTH_001"))
        .andExpect(jsonPath("$.question").value("How many steps did I walk today?"))
        .andExpect(jsonPath("$.precondition.steps").value(8200))
        .andExpect(jsonPath("$.groundTruth").value("The user walked 8,200 steps today."))
        .andExpect(jsonPath("$.metadata.userId").value("demo-user-1"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.sortOrder").value(1));

    assertThat(RecordingTestCaseService.datasetPublicId)
        .isEqualTo(UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"));
    assertThat(RecordingTestCaseService.createTestCaseRequest.externalId()).isEqualTo("HEALTH_001");
    assertThat(RecordingTestCaseService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void createTestCaseReturnsValidationProblemDetails() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/datasets/0f6d90c2-7410-4db2-86be-8adfd3140f63/test-cases")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "question": " "
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(
            jsonPath("$.instance")
                .value("/api/v1/datasets/0f6d90c2-7410-4db2-86be-8adfd3140f63/test-cases"))
        .andExpect(jsonPath("$.errors[*].field", hasItem("question")));

    assertThat(RecordingTestCaseService.createTestCaseRequest).isNull();
  }

  @Test
  void createTestCaseReturnsValidationProblemDetailsForInvalidDatasetPublicId()
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/datasets/not-a-uuid/test-cases")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "question": "How many steps did I walk today?"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.instance").value("/api/v1/datasets/not-a-uuid/test-cases"));

    assertThat(RecordingTestCaseService.createTestCaseRequest).isNull();
  }

  @TestConfiguration
  static class MockBeans {

    @Bean
    TestCaseService testCaseService() {
      return new RecordingTestCaseService();
    }
  }

  static class RecordingTestCaseService implements TestCaseService {

    static UUID datasetPublicId;
    static CreateTestCaseRequest createTestCaseRequest;
    static String username;
    static TestCaseResponse testCaseResponse;

    static void reset() {
      datasetPublicId = null;
      createTestCaseRequest = null;
      username = null;
      testCaseResponse = null;
    }

    @Override
    public TestCaseResponse createTestCase(
        UUID datasetPublicId, CreateTestCaseRequest request, String username) {
      RecordingTestCaseService.datasetPublicId = datasetPublicId;
      RecordingTestCaseService.createTestCaseRequest = request;
      RecordingTestCaseService.username = username;
      return testCaseResponse;
    }
  }
}
