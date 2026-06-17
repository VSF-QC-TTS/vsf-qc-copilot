package me.nghlong3004.vqc.api.judge.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import me.nghlong3004.vqc.api.exception.GlobalException;
import me.nghlong3004.vqc.api.judge.enums.JudgeProvider;
import me.nghlong3004.vqc.api.judge.request.CreateJudgeModelRequest;
import me.nghlong3004.vqc.api.judge.request.UpdateJudgeModelRequest;
import me.nghlong3004.vqc.api.judge.response.JudgeModelPageResponse;
import me.nghlong3004.vqc.api.judge.response.JudgeModelResponse;
import me.nghlong3004.vqc.api.judge.service.JudgeModelService;
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
 * @since 6/15/2026
 */
@WebMvcTest(
    controllers = JudgeModelController.class,
    excludeAutoConfiguration = {
      OAuth2ClientAutoConfiguration.class,
      OAuth2ClientWebSecurityAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalException.class, JudgeModelControllerTest.MockBeans.class})
class JudgeModelControllerTest {

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void reset() {
    RecordingJudgeModelService.reset();
  }

  @Test
  void createJudgeModelReturnsMaskedResponse() throws Exception {
    RecordingJudgeModelService.response = response();

    mockMvc
        .perform(
            post("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/judge-models")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Gemini QC Judge",
                      "provider": "GEMINI",
                      "modelName": "gemini-2.5-flash",
                      "apiKey": "sk-secret-1234",
                      "active": true
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.publicId").value("8d2f6a2a-4974-4e9c-83ad-f2e1e58d39f0"))
        .andExpect(jsonPath("$.provider").value("GEMINI"))
        .andExpect(jsonPath("$.apiKeyMasked").value("****1234"));

    assertThat(RecordingJudgeModelService.projectPublicId)
        .isEqualTo(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"));
    assertThat(RecordingJudgeModelService.createRequest.apiKey()).isEqualTo("sk-secret-1234");
    assertThat(RecordingJudgeModelService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void createJudgeModelRequiresApiKey() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/judge-models")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Gemini QC Judge",
                      "provider": "GEMINI",
                      "modelName": "gemini-2.5-flash"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[*].field", hasItem("apiKey")));

    assertThat(RecordingJudgeModelService.createRequest).isNull();
  }

  @Test
  void listJudgeModelsPassesActiveFilter() throws Exception {
    RecordingJudgeModelService.pageResponse =
        new JudgeModelPageResponse(List.of(response()), 0, 20, 1, 1);

    mockMvc
        .perform(
            get("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/judge-models")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .queryParam("active", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].name").value("Gemini QC Judge"))
        .andExpect(jsonPath("$.totalItems").value(1));

    assertThat(RecordingJudgeModelService.active).isTrue();
  }

  private JudgeModelResponse response() {
    return new JudgeModelResponse(
        UUID.fromString("8d2f6a2a-4974-4e9c-83ad-f2e1e58d39f0"),
        UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"),
        "Gemini QC Judge",
        JudgeProvider.GEMINI,
        "gemini-2.5-flash",
        null,
        "****1234",
        null,
        true,
        OffsetDateTime.parse("2026-06-15T10:30:00Z"),
        OffsetDateTime.parse("2026-06-15T10:30:00Z"));
  }

  @TestConfiguration
  static class MockBeans {
    @Bean
    JudgeModelService judgeModelService() {
      return new RecordingJudgeModelService();
    }
  }

  static class RecordingJudgeModelService implements JudgeModelService {

    static UUID projectPublicId;
    static UUID judgeModelPublicId;
    static Boolean active;
    static String username;
    static CreateJudgeModelRequest createRequest;
    static JudgeModelResponse response;
    static JudgeModelPageResponse pageResponse;

    @Override
    public JudgeModelResponse createJudgeModel(
        UUID projectPublicId, CreateJudgeModelRequest request, String username) {
      RecordingJudgeModelService.projectPublicId = projectPublicId;
      RecordingJudgeModelService.createRequest = request;
      RecordingJudgeModelService.username = username;
      return response;
    }

    @Override
    public JudgeModelPageResponse listJudgeModels(
        UUID projectPublicId, Boolean active, Pageable pageable, String username) {
      RecordingJudgeModelService.projectPublicId = projectPublicId;
      RecordingJudgeModelService.active = active;
      RecordingJudgeModelService.username = username;
      return pageResponse;
    }

    @Override
    public JudgeModelResponse updateJudgeModel(
        UUID judgeModelPublicId, UpdateJudgeModelRequest request, String username) {
      RecordingJudgeModelService.judgeModelPublicId = judgeModelPublicId;
      RecordingJudgeModelService.username = username;
      return response;
    }

    @Override
    public void deleteJudgeModel(UUID judgeModelPublicId, String username) {
      RecordingJudgeModelService.judgeModelPublicId = judgeModelPublicId;
      RecordingJudgeModelService.username = username;
    }

    @Override
    public JudgeModelResponse testConnection(UUID judgeModelPublicId, String username) {
      RecordingJudgeModelService.judgeModelPublicId = judgeModelPublicId;
      RecordingJudgeModelService.username = username;
      return response;
    }

    static void reset() {
      projectPublicId = null;
      judgeModelPublicId = null;
      active = null;
      username = null;
      createRequest = null;
      response = null;
      pageResponse = null;
    }
  }
}
