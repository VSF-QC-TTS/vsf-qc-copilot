package me.nghlong3004.vqc.api.dataset.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.dataset.enums.DatasetSourceType;
import me.nghlong3004.vqc.api.dataset.enums.DatasetStatus;
import me.nghlong3004.vqc.api.dataset.request.CreateDatasetRequest;
import me.nghlong3004.vqc.api.dataset.response.DatasetResponse;
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
  }

  @Test
  void createDatasetReturnsCreatedDataset() throws Exception {
    RecordingDatasetService.datasetResponse =
        new DatasetResponse(
            UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"),
            UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"),
            UUID.fromString("ebd7f0f0-4924-4e81-9795-d1f060bec2f2"),
            "Health Demo Dataset",
            "Sample dataset for Week 4 demo.",
            1,
            DatasetSourceType.SAMPLE_DEMO,
            DatasetStatus.DRAFT,
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
                      "requirementPublicId": "ebd7f0f0-4924-4e81-9795-d1f060bec2f2",
                      "sourceType": "SAMPLE_DEMO",
                      "name": "Health Demo Dataset",
                      "description": "Sample dataset for Week 4 demo."
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.publicId").value("0f6d90c2-7410-4db2-86be-8adfd3140f63"))
        .andExpect(
            jsonPath("$.projectPublicId").value("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"))
        .andExpect(
            jsonPath("$.requirementPublicId")
                .value("ebd7f0f0-4924-4e81-9795-d1f060bec2f2"))
        .andExpect(jsonPath("$.name").value("Health Demo Dataset"))
        .andExpect(jsonPath("$.description").value("Sample dataset for Week 4 demo."))
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.sourceType").value("SAMPLE_DEMO"))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.totalCases").value(0));

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

  @TestConfiguration
  static class MockBeans {

    @Bean
    DatasetService datasetService() {
      return new RecordingDatasetService();
    }
  }

  static class RecordingDatasetService implements DatasetService {

    static UUID projectPublicId;
    static CreateDatasetRequest createDatasetRequest;
    static String username;
    static DatasetResponse datasetResponse;

    static void reset() {
      projectPublicId = null;
      createDatasetRequest = null;
      username = null;
      datasetResponse = null;
    }

    @Override
    public DatasetResponse createDataset(
        UUID projectPublicId, CreateDatasetRequest request, String username) {
      RecordingDatasetService.projectPublicId = projectPublicId;
      RecordingDatasetService.createDatasetRequest = request;
      RecordingDatasetService.username = username;
      return datasetResponse;
    }
  }
}
