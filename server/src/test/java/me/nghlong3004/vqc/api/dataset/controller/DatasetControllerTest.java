package me.nghlong3004.vqc.api.dataset.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import me.nghlong3004.vqc.api.dataset.enums.DatasetSourceType;
import me.nghlong3004.vqc.api.dataset.enums.DatasetStatus;
import me.nghlong3004.vqc.api.dataset.request.CreateDatasetRequest;
import me.nghlong3004.vqc.api.dataset.request.GenerateDatasetRequest;
import me.nghlong3004.vqc.api.dataset.request.UpdateDatasetRequest;
import me.nghlong3004.vqc.api.dataset.response.DatasetListItemResponse;
import me.nghlong3004.vqc.api.dataset.response.DatasetPageResponse;
import me.nghlong3004.vqc.api.dataset.response.DatasetResponse;
import me.nghlong3004.vqc.api.dataset.response.GenerateDatasetResponse;
import me.nghlong3004.vqc.api.dataset.service.DatasetGenerationService;
import me.nghlong3004.vqc.api.dataset.service.DatasetService;
import me.nghlong3004.vqc.api.exception.GlobalException;
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
 * @since 6/10/2026
 */
@WebMvcTest(
    controllers = DatasetController.class,
    excludeAutoConfiguration = {
      OAuth2ClientAutoConfiguration.class,
      OAuth2ClientWebSecurityAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalException.class, DatasetControllerTest.MockBeans.class})
class DatasetControllerTest {

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void resetTestDoubles() {
    RecordingDatasetService.reset();
    RecordingDatasetGenerationService.reset();
  }

  @Test
  void createDatasetReturnsCreatedDataset() throws Exception {
    RecordingDatasetService.datasetResponse =
        new DatasetResponse(
            UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"),
            UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"),
            "Health Demo Dataset",
            "Sample dataset for Week 4 demo.",
            1,
            DatasetSourceType.SAMPLE_DEMO,
            DatasetStatus.DRAFT,
            0,
            0,
            OffsetDateTime.parse("2026-06-08T10:30:00Z"),
            OffsetDateTime.parse("2026-06-08T10:30:00Z"));

    mockMvc
        .perform(
            post("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/datasets")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sourceType": "SAMPLE_DEMO",
                      "name": "Health Demo Dataset",
                      "description": "Sample dataset for Week 4 demo."
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.publicId").value("0f6d90c2-7410-4db2-86be-8adfd3140f63"))
        .andExpect(
            jsonPath("$.projectPublicId").value("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"))
        .andExpect(jsonPath("$.name").value("Health Demo Dataset"))
        .andExpect(jsonPath("$.description").value("Sample dataset for Week 4 demo."))
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.sourceType").value("SAMPLE_DEMO"))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.testCaseCount").value(0))
        .andExpect(jsonPath("$.activeTestCaseCount").value(0));

    assertThat(RecordingDatasetService.projectPublicId)
        .isEqualTo(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"));
    assertThat(RecordingDatasetService.createDatasetRequest.name())
        .isEqualTo("Health Demo Dataset");
    assertThat(RecordingDatasetService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void createDatasetReturnsValidationProblemDetails() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/datasets")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sourceType": "MANUAL",
                      "name": " "
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(
            jsonPath("$.instance")
                .value("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/datasets"))
        .andExpect(jsonPath("$.errors[*].field", hasItem("name")));

    assertThat(RecordingDatasetService.createDatasetRequest).isNull();
    assertThat(RecordingDatasetService.username).isNull();
  }

  @Test
  void createDatasetReturnsValidationProblemDetailsForInvalidProjectPublicId()
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/projects/not-a-uuid/datasets")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sourceType": "MANUAL",
                      "name": "Health Demo Dataset"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.instance").value("/api/v1/projects/not-a-uuid/datasets"));

    assertThat(RecordingDatasetService.createDatasetRequest).isNull();
  }

  @Test
  void listDatasetsReturnsDatasetPage() throws Exception {
    RecordingDatasetService.datasetPageResponse =
        new DatasetPageResponse(
            List.of(
                new DatasetListItemResponse(
                    UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"),
                    UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"),
                    "Health Demo Dataset",
                    DatasetSourceType.SAMPLE_DEMO,
                    DatasetStatus.DRAFT,
                    0,
                    OffsetDateTime.parse("2026-06-08T10:30:00Z"))),
            0,
            20,
            1,
            1);

    mockMvc
        .perform(
            get("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/datasets")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .queryParam("status", "DRAFT")
                .queryParam("page", "0")
                .queryParam("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].publicId").value("0f6d90c2-7410-4db2-86be-8adfd3140f63"))
        .andExpect(
            jsonPath("$.items[0].projectPublicId")
                .value("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"))
        .andExpect(jsonPath("$.items[0].name").value("Health Demo Dataset"))
        .andExpect(jsonPath("$.items[0].sourceType").value("SAMPLE_DEMO"))
        .andExpect(jsonPath("$.items[0].status").value("DRAFT"))
        .andExpect(jsonPath("$.items[0].testCaseCount").value(0))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalItems").value(1))
        .andExpect(jsonPath("$.totalPages").value(1));

    assertThat(RecordingDatasetService.projectPublicId)
        .isEqualTo(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"));
    assertThat(RecordingDatasetService.status).isEqualTo(DatasetStatus.DRAFT);
    assertThat(RecordingDatasetService.pageable.getPageNumber()).isZero();
    assertThat(RecordingDatasetService.pageable.getPageSize()).isEqualTo(20);
    assertThat(RecordingDatasetService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void listDatasetsReturnsValidationProblemDetailsForInvalidStatus() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/datasets")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .queryParam("status", "UNKNOWN"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(
            jsonPath("$.instance")
                .value("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/datasets"));

    assertThat(RecordingDatasetService.datasetPageResponse).isNull();
  }

  @Test
  void getDatasetReturnsDatasetDetail() throws Exception {
    RecordingDatasetService.datasetResponse =
        new DatasetResponse(
            UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"),
            UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"),
            "Health Demo Dataset",
            "Sample dataset for Week 4 demo.",
            1,
            DatasetSourceType.SAMPLE_DEMO,
            DatasetStatus.DRAFT,
            0,
            0,
            OffsetDateTime.parse("2026-06-08T10:30:00Z"),
            OffsetDateTime.parse("2026-06-08T10:35:00Z"));

    mockMvc
        .perform(
            get("/api/v1/datasets/0f6d90c2-7410-4db2-86be-8adfd3140f63")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value("0f6d90c2-7410-4db2-86be-8adfd3140f63"))
        .andExpect(
            jsonPath("$.projectPublicId").value("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"))
        .andExpect(jsonPath("$.name").value("Health Demo Dataset"))
        .andExpect(jsonPath("$.sourceType").value("SAMPLE_DEMO"))
        .andExpect(jsonPath("$.status").value("DRAFT"));

    assertThat(RecordingDatasetService.datasetPublicId)
        .isEqualTo(UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"));
    assertThat(RecordingDatasetService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void getDatasetReturnsValidationProblemDetailsForInvalidPublicId() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/datasets/not-a-uuid")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.instance").value("/api/v1/datasets/not-a-uuid"));

    assertThat(RecordingDatasetService.datasetPublicId).isNull();
  }

  @Test
  void updateDatasetReturnsUpdatedDataset() throws Exception {
    RecordingDatasetService.datasetResponse =
        new DatasetResponse(
            UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"),
            UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"),
            "Health Demo Dataset v2",
            "Updated description",
            1,
            DatasetSourceType.SAMPLE_DEMO,
            DatasetStatus.APPROVED,
            12,
            12,
            OffsetDateTime.parse("2026-06-08T10:30:00Z"),
            OffsetDateTime.parse("2026-06-08T10:35:00Z"));

    mockMvc
        .perform(
            patch("/api/v1/datasets/0f6d90c2-7410-4db2-86be-8adfd3140f63")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Health Demo Dataset v2",
                      "description": "Updated description",
                      "status": "APPROVED"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value("0f6d90c2-7410-4db2-86be-8adfd3140f63"))
        .andExpect(jsonPath("$.name").value("Health Demo Dataset v2"))
        .andExpect(jsonPath("$.description").value("Updated description"))
        .andExpect(jsonPath("$.status").value("APPROVED"))
        .andExpect(jsonPath("$.testCaseCount").value(12))
        .andExpect(jsonPath("$.activeTestCaseCount").value(12));

    assertThat(RecordingDatasetService.datasetPublicId)
        .isEqualTo(UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"));
    assertThat(RecordingDatasetService.updateDatasetRequest.name())
        .isEqualTo("Health Demo Dataset v2");
    assertThat(RecordingDatasetService.updateDatasetRequest.status())
        .isEqualTo(DatasetStatus.APPROVED);
    assertThat(RecordingDatasetService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void updateDatasetReturnsValidationProblemDetails() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/datasets/0f6d90c2-7410-4db2-86be-8adfd3140f63")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": " "
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.instance").value("/api/v1/datasets/0f6d90c2-7410-4db2-86be-8adfd3140f63"))
        .andExpect(jsonPath("$.errors[*].field", hasItem("name")));

    assertThat(RecordingDatasetService.updateDatasetRequest).isNull();
  }

  @Test
  void generateTestCasesReturnsAcceptedWithJobInfo() throws Exception {
    RecordingDatasetGenerationService.response =
        new GenerateDatasetResponse(
            UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"),
            UUID.fromString("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d"),
            "PENDING",
            "Dataset generation queued successfully.");

    mockMvc
        .perform(
            post("/api/v1/datasets/0f6d90c2-7410-4db2-86be-8adfd3140f63/generate")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "prompt": "The chatbot should answer health-related questions accurately.",
                      "count": 30,
                      "additionalPrompt": "Focus on edge cases"
                    }
                    """))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.datasetPublicId").value("0f6d90c2-7410-4db2-86be-8adfd3140f63"))
        .andExpect(jsonPath("$.jobPublicId").value("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.message").value("Dataset generation queued successfully."));

    assertThat(RecordingDatasetGenerationService.datasetPublicId)
        .isEqualTo(UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"));
    assertThat(RecordingDatasetGenerationService.request.count()).isEqualTo(30);
    assertThat(RecordingDatasetGenerationService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void generateTestCasesReturnsValidationProblemDetailsForMissingFields() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/datasets/0f6d90c2-7410-4db2-86be-8adfd3140f63/generate")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "count": 200
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[*].field", hasItem("prompt")));

    assertThat(RecordingDatasetGenerationService.request).isNull();
  }

  @TestConfiguration
  static class MockBeans {

    @Bean
    DatasetService datasetService() {
      return new RecordingDatasetService();
    }

    @Bean
    DatasetGenerationService datasetGenerationService() {
      return new RecordingDatasetGenerationService();
    }
  }

  static class RecordingDatasetService implements DatasetService {

    static UUID projectPublicId;
    static UUID datasetPublicId;
    static CreateDatasetRequest createDatasetRequest;
    static UpdateDatasetRequest updateDatasetRequest;
    static DatasetStatus status;
    static Pageable pageable;
    static String username;
    static DatasetResponse datasetResponse;
    static DatasetPageResponse datasetPageResponse;

    static void reset() {
      projectPublicId = null;
      datasetPublicId = null;
      createDatasetRequest = null;
      updateDatasetRequest = null;
      status = null;
      pageable = null;
      username = null;
      datasetResponse = null;
      datasetPageResponse = null;
    }

    @Override
    public DatasetResponse createDataset(
        UUID projectPublicId, CreateDatasetRequest request, String username) {
      RecordingDatasetService.projectPublicId = projectPublicId;
      RecordingDatasetService.createDatasetRequest = request;
      RecordingDatasetService.username = username;
      return datasetResponse;
    }

    @Override
    public DatasetPageResponse listDatasets(
        UUID projectPublicId, DatasetStatus status, Pageable pageable, String username) {
      RecordingDatasetService.projectPublicId = projectPublicId;
      RecordingDatasetService.status = status;
      RecordingDatasetService.pageable = pageable;
      RecordingDatasetService.username = username;
      return datasetPageResponse;
    }

    @Override
    public DatasetResponse getDataset(UUID datasetPublicId, String username) {
      RecordingDatasetService.datasetPublicId = datasetPublicId;
      RecordingDatasetService.username = username;
      return datasetResponse;
    }

    @Override
    public DatasetResponse updateDataset(
        UUID datasetPublicId, UpdateDatasetRequest request, String username) {
      RecordingDatasetService.datasetPublicId = datasetPublicId;
      RecordingDatasetService.updateDatasetRequest = request;
      RecordingDatasetService.username = username;
      return datasetResponse;
    }

    @Override
    public void deleteDataset(UUID datasetPublicId, String username) {
      RecordingDatasetService.datasetPublicId = datasetPublicId;
      RecordingDatasetService.username = username;
    }
  }

  static class RecordingDatasetGenerationService implements DatasetGenerationService {

    static UUID datasetPublicId;
    static GenerateDatasetRequest request;
    static String username;
    static GenerateDatasetResponse response;

    static void reset() {
      datasetPublicId = null;
      request = null;
      username = null;
      response = null;
    }

    @Override
    public GenerateDatasetResponse generateTestCases(
        UUID datasetPublicId, GenerateDatasetRequest request, String username) {
      RecordingDatasetGenerationService.datasetPublicId = datasetPublicId;
      RecordingDatasetGenerationService.request = request;
      RecordingDatasetGenerationService.username = username;
      return response;
    }
  }
}
