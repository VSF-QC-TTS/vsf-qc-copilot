package me.nghlong3004.vqc.api.requirement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.exception.GlobalException;
import me.nghlong3004.vqc.api.requirement.enums.RequirementStatus;
import me.nghlong3004.vqc.api.requirement.request.CreateRequirementRequest;
import me.nghlong3004.vqc.api.requirement.response.RequirementResponse;
import me.nghlong3004.vqc.api.requirement.service.RequirementService;
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
    controllers = RequirementController.class,
    excludeAutoConfiguration = {
      OAuth2ClientAutoConfiguration.class,
      OAuth2ClientWebSecurityAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalException.class, RequirementControllerTest.MockBeans.class})
class RequirementControllerTest {

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void resetTestDoubles() {
    RecordingRequirementService.reset();
  }

  @Test
  void createRequirementReturnsCreatedRequirement() throws Exception {
    RecordingRequirementService.requirementResponse =
        new RequirementResponse(
            UUID.fromString("ebd7f0f0-4924-4e81-9795-d1f060bec2f2"),
            UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"),
            "Evaluate Apple Health step-count answers.",
            1,
            RequirementStatus.ACTIVE,
            OffsetDateTime.parse("2026-06-08T10:30:00Z"),
            OffsetDateTime.parse("2026-06-08T10:30:00Z"));

    mockMvc
        .perform(
            post("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/requirements")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "content": "Evaluate Apple Health step-count answers."
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.publicId").value("ebd7f0f0-4924-4e81-9795-d1f060bec2f2"))
        .andExpect(
            jsonPath("$.projectPublicId").value("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"))
        .andExpect(jsonPath("$.content").value("Evaluate Apple Health step-count answers."))
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    assertThat(RecordingRequirementService.projectPublicId)
        .isEqualTo(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"));
    assertThat(RecordingRequirementService.createRequirementRequest.content())
        .isEqualTo("Evaluate Apple Health step-count answers.");
    assertThat(RecordingRequirementService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void createRequirementReturnsValidationProblemDetails() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/requirements")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "content": " "
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(
            jsonPath("$.instance")
                .value("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/requirements"))
        .andExpect(jsonPath("$.errors[*].field", hasItem("content")));

    assertThat(RecordingRequirementService.createRequirementRequest).isNull();
    assertThat(RecordingRequirementService.username).isNull();
  }

  @Test
  void createRequirementReturnsValidationProblemDetailsForInvalidProjectPublicId()
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/projects/not-a-uuid/requirements")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "content": "Evaluate Apple Health step-count answers."
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.instance").value("/api/v1/projects/not-a-uuid/requirements"));

    assertThat(RecordingRequirementService.createRequirementRequest).isNull();
  }

  @TestConfiguration
  static class MockBeans {
    @Bean
    RequirementService requirementService() {
      return new RecordingRequirementService();
    }
  }

  private static class RecordingRequirementService implements RequirementService {
    private static UUID projectPublicId;
    private static CreateRequirementRequest createRequirementRequest;
    private static String username;
    private static RequirementResponse requirementResponse;

    private static void reset() {
      projectPublicId = null;
      createRequirementRequest = null;
      username = null;
      requirementResponse = null;
    }

    @Override
    public RequirementResponse createRequirement(
        UUID projectPublicId, CreateRequirementRequest request, String username) {
      RecordingRequirementService.projectPublicId = projectPublicId;
      RecordingRequirementService.createRequirementRequest = request;
      RecordingRequirementService.username = username;
      return requirementResponse;
    }
  }
}
