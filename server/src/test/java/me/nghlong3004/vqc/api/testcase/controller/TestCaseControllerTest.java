package me.nghlong3004.vqc.api.testcase.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.nghlong3004.vqc.api.exception.GlobalException;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;
import me.nghlong3004.vqc.api.testcase.request.CreateTestCaseRequest;
import me.nghlong3004.vqc.api.testcase.request.UpdateTestCaseRequest;
import me.nghlong3004.vqc.api.testcase.response.ImportTestCaseResponse;
import me.nghlong3004.vqc.api.testcase.response.ImportTestCaseResponse.ImportError;
import me.nghlong3004.vqc.api.testcase.response.TestCasePageResponse;
import me.nghlong3004.vqc.api.testcase.response.TestCaseResponse;
import me.nghlong3004.vqc.api.testcase.service.TestCaseImportService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

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
    RecordingTestCaseImportService.reset();
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

  @Test
  void listTestCasesReturnsTestCasePage() throws Exception {
    RecordingTestCaseService.testCasePageResponse =
        new TestCasePageResponse(
            List.of(
                new TestCaseResponse(
                    UUID.fromString("b4788db3-6cf3-47df-8ae1-4c73dbb7d0a8"),
                    UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"),
                    "HEALTH_001",
                    "How many steps did I walk today?",
                    Map.of("steps", 8200),
                    "The user walked 8,200 steps today.",
                    Map.of("userId", "demo-user-1"),
                    TestCaseStatus.ACTIVE,
                    1,
                    OffsetDateTime.parse("2026-06-08T10:30:00Z"),
                    OffsetDateTime.parse("2026-06-08T10:30:00Z"))),
            0,
            100,
            1,
            1);

    mockMvc
        .perform(
            get("/api/v1/datasets/0f6d90c2-7410-4db2-86be-8adfd3140f63/test-cases")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .queryParam("status", "ACTIVE")
                .queryParam("page", "0")
                .queryParam("size", "100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].publicId").value("b4788db3-6cf3-47df-8ae1-4c73dbb7d0a8"))
        .andExpect(
            jsonPath("$.items[0].datasetPublicId")
                .value("0f6d90c2-7410-4db2-86be-8adfd3140f63"))
        .andExpect(jsonPath("$.items[0].externalId").value("HEALTH_001"))
        .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(100))
        .andExpect(jsonPath("$.totalItems").value(1))
        .andExpect(jsonPath("$.totalPages").value(1));

    assertThat(RecordingTestCaseService.datasetPublicId)
        .isEqualTo(UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"));
    assertThat(RecordingTestCaseService.status).isEqualTo(TestCaseStatus.ACTIVE);
    assertThat(RecordingTestCaseService.pageable.getPageSize()).isEqualTo(100);
    assertThat(RecordingTestCaseService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void listTestCasesReturnsValidationProblemDetailsForInvalidStatus() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/datasets/0f6d90c2-7410-4db2-86be-8adfd3140f63/test-cases")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .queryParam("status", "UNKNOWN"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(
            jsonPath("$.instance")
                .value("/api/v1/datasets/0f6d90c2-7410-4db2-86be-8adfd3140f63/test-cases"));

    assertThat(RecordingTestCaseService.testCasePageResponse).isNull();
  }

  @Test
  void updateTestCaseReturnsUpdatedTestCase() throws Exception {
    RecordingTestCaseService.testCaseResponse =
        new TestCaseResponse(
            UUID.fromString("b4788db3-6cf3-47df-8ae1-4c73dbb7d0a8"),
            UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"),
            "HEALTH_002",
            "How many active calories today?",
            Map.of("calories", 500),
            "The user burned 500 active calories today.",
            Map.of("userId", "demo-user-1"),
            TestCaseStatus.INACTIVE,
            2,
            OffsetDateTime.parse("2026-06-08T10:30:00Z"),
            OffsetDateTime.parse("2026-06-08T10:35:00Z"));

    mockMvc
        .perform(
            patch("/api/v1/test-cases/b4788db3-6cf3-47df-8ae1-4c73dbb7d0a8")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "externalId": "HEALTH_002",
                      "question": "How many active calories today?",
                      "precondition": {
                        "calories": 500
                      },
                      "groundTruth": "The user burned 500 active calories today.",
                      "metadata": {
                        "userId": "demo-user-1"
                      },
                      "status": "INACTIVE",
                      "sortOrder": 2
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value("b4788db3-6cf3-47df-8ae1-4c73dbb7d0a8"))
        .andExpect(jsonPath("$.externalId").value("HEALTH_002"))
        .andExpect(jsonPath("$.question").value("How many active calories today?"))
        .andExpect(jsonPath("$.status").value("INACTIVE"))
        .andExpect(jsonPath("$.sortOrder").value(2));

    assertThat(RecordingTestCaseService.testCasePublicId)
        .isEqualTo(UUID.fromString("b4788db3-6cf3-47df-8ae1-4c73dbb7d0a8"));
    assertThat(RecordingTestCaseService.updateTestCaseRequest.status())
        .isEqualTo(TestCaseStatus.INACTIVE);
    assertThat(RecordingTestCaseService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void updateTestCaseReturnsValidationProblemDetails() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/test-cases/b4788db3-6cf3-47df-8ae1-4c73dbb7d0a8")
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
        .andExpect(jsonPath("$.instance").value("/api/v1/test-cases/b4788db3-6cf3-47df-8ae1-4c73dbb7d0a8"))
        .andExpect(jsonPath("$.errors[*].field", hasItem("question")));

    assertThat(RecordingTestCaseService.updateTestCaseRequest).isNull();
  }

  @Test
  void deleteTestCaseReturnsNoContent() throws Exception {
    mockMvc
        .perform(
            delete("/api/v1/test-cases/b4788db3-6cf3-47df-8ae1-4c73dbb7d0a8")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isNoContent());

    assertThat(RecordingTestCaseService.testCasePublicId)
        .isEqualTo(UUID.fromString("b4788db3-6cf3-47df-8ae1-4c73dbb7d0a8"));
    assertThat(RecordingTestCaseService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void deleteTestCaseReturnsValidationProblemDetailsForInvalidPublicId() throws Exception {
    mockMvc
        .perform(
            delete("/api/v1/test-cases/not-a-uuid")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.instance").value("/api/v1/test-cases/not-a-uuid"));

    assertThat(RecordingTestCaseService.testCasePublicId).isNull();
  }

  @Test
  void importTestCasesReturnsImportResult() throws Exception {
    RecordingTestCaseImportService.importResponse =
        new ImportTestCaseResponse(3, 2, 1, List.of(new ImportError(2, "question", "Question is required.")));

    MockMultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv", "question\nQ1\n\nQ3\n".getBytes());

    mockMvc
        .perform(
            multipart("/api/v1/datasets/0f6d90c2-7410-4db2-86be-8adfd3140f63/test-cases/import")
                .file(file)
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalRows").value(3))
        .andExpect(jsonPath("$.importedCount").value(2))
        .andExpect(jsonPath("$.skippedCount").value(1))
        .andExpect(jsonPath("$.errors[0].row").value(2))
        .andExpect(jsonPath("$.errors[0].column").value("question"))
        .andExpect(jsonPath("$.errors[0].message").value("Question is required."));

    assertThat(RecordingTestCaseImportService.datasetPublicId)
        .isEqualTo(UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"));
    assertThat(RecordingTestCaseImportService.username).isEqualTo("qc.demo@example.com");
    assertThat(RecordingTestCaseImportService.file).isNotNull();
  }

  @Test
  void importTestCasesReturnsValidationProblemDetailsForInvalidDatasetPublicId() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "test.csv", "text/csv", "question\nQ1\n".getBytes());

    mockMvc
        .perform(
            multipart("/api/v1/datasets/not-a-uuid/test-cases/import")
                .file(file)
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    assertThat(RecordingTestCaseImportService.file).isNull();
  }

  @TestConfiguration
  static class MockBeans {

    @Bean
    TestCaseService testCaseService() {
      return new RecordingTestCaseService();
    }

    @Bean
    TestCaseImportService testCaseImportService() {
      return new RecordingTestCaseImportService();
    }
  }

  static class RecordingTestCaseService implements TestCaseService {

    static UUID datasetPublicId;
    static UUID testCasePublicId;
    static CreateTestCaseRequest createTestCaseRequest;
    static UpdateTestCaseRequest updateTestCaseRequest;
    static TestCaseStatus status;
    static Pageable pageable;
    static String username;
    static TestCaseResponse testCaseResponse;
    static TestCasePageResponse testCasePageResponse;

    static void reset() {
      datasetPublicId = null;
      testCasePublicId = null;
      createTestCaseRequest = null;
      updateTestCaseRequest = null;
      status = null;
      pageable = null;
      username = null;
      testCaseResponse = null;
      testCasePageResponse = null;
    }

    @Override
    public TestCaseResponse createTestCase(
        UUID datasetPublicId, CreateTestCaseRequest request, String username) {
      RecordingTestCaseService.datasetPublicId = datasetPublicId;
      RecordingTestCaseService.createTestCaseRequest = request;
      RecordingTestCaseService.username = username;
      return testCaseResponse;
    }

    @Override
    public TestCasePageResponse listTestCases(
        UUID datasetPublicId, TestCaseStatus status, Pageable pageable, String username) {
      RecordingTestCaseService.datasetPublicId = datasetPublicId;
      RecordingTestCaseService.status = status;
      RecordingTestCaseService.pageable = pageable;
      RecordingTestCaseService.username = username;
      return testCasePageResponse;
    }

    @Override
    public TestCaseResponse updateTestCase(
        UUID testCasePublicId, UpdateTestCaseRequest request, String username) {
      RecordingTestCaseService.testCasePublicId = testCasePublicId;
      RecordingTestCaseService.updateTestCaseRequest = request;
      RecordingTestCaseService.username = username;
      return testCaseResponse;
    }

    @Override
    public void deleteTestCase(UUID testCasePublicId, String username) {
      RecordingTestCaseService.testCasePublicId = testCasePublicId;
      RecordingTestCaseService.username = username;
    }
  }

  static class RecordingTestCaseImportService implements TestCaseImportService {

    static UUID datasetPublicId;
    static MultipartFile file;
    static String username;
    static ImportTestCaseResponse importResponse;

    static void reset() {
      datasetPublicId = null;
      file = null;
      username = null;
      importResponse = null;
    }

    @Override
    public ImportTestCaseResponse importTestCases(
        UUID datasetPublicId, MultipartFile file, String username) {
      RecordingTestCaseImportService.datasetPublicId = datasetPublicId;
      RecordingTestCaseImportService.file = file;
      RecordingTestCaseImportService.username = username;
      return importResponse;
    }
  }
}
