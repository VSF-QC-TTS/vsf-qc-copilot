package me.nghlong3004.vqc.api.export.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import me.nghlong3004.vqc.api.exception.GlobalException;
import me.nghlong3004.vqc.api.export.enums.ExportFileStatus;
import me.nghlong3004.vqc.api.export.enums.ExportFileType;
import me.nghlong3004.vqc.api.export.request.CreateExportRequest;
import me.nghlong3004.vqc.api.export.response.CreateExportResponse;
import me.nghlong3004.vqc.api.export.service.ExportService;
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
    controllers = ExportController.class,
    excludeAutoConfiguration = {
      OAuth2ClientAutoConfiguration.class,
      OAuth2ClientWebSecurityAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalException.class, ExportControllerTest.MockBeans.class})
class ExportControllerTest {

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void reset() {
    RecordingExportService.reset();
  }

  @Test
  void createExportReturnsAccepted() throws Exception {
    RecordingExportService.response =
        new CreateExportResponse(
            UUID.fromString("e1e2e3e4-e5f6-7890-abcd-ef1234567890"),
            UUID.fromString("j1e2e3e4-e5f6-7890-abcd-ef1234567890".replace('j', 'a')),
            ExportFileStatus.PENDING,
            "Export job accepted.");

    mockMvc
        .perform(
            post("/api/v1/evaluation-runs/a1b2c3d4-e5f6-7890-abcd-ef1234567890/exports")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "fileType": "EXCEL"
                    }
                    """))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.exportPublicId").value("e1e2e3e4-e5f6-7890-abcd-ef1234567890"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.message").value("Export job accepted."));

    assertThat(RecordingExportService.runPublicId)
        .isEqualTo(UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
    assertThat(RecordingExportService.request.fileType()).isEqualTo(ExportFileType.EXCEL);
    assertThat(RecordingExportService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void createExportReturnsValidationForMissingFileType() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/evaluation-runs/a1b2c3d4-e5f6-7890-abcd-ef1234567890/exports")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    assertThat(RecordingExportService.request).isNull();
  }

  @Test
  void createExportReturnsValidationForInvalidRunId() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/evaluation-runs/not-a-uuid/exports")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "fileType": "JSON"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    assertThat(RecordingExportService.runPublicId).isNull();
  }

  @TestConfiguration
  static class MockBeans {
    @Bean
    ExportService exportService() {
      return new RecordingExportService();
    }
  }

  static class RecordingExportService implements ExportService {
    static UUID runPublicId;
    static CreateExportRequest request;
    static String username;
    static CreateExportResponse response;

    static void reset() {
      runPublicId = null;
      request = null;
      username = null;
      response = null;
    }

    @Override
    public CreateExportResponse createExport(
        UUID runPublicId, CreateExportRequest request, String username) {
      RecordingExportService.runPublicId = runPublicId;
      RecordingExportService.request = request;
      RecordingExportService.username = username;
      return response;
    }
  }
}
