package me.nghlong3004.vqc.api.requirement.controller;

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
import me.nghlong3004.vqc.api.requirement.enums.RequirementStatus;
import me.nghlong3004.vqc.api.requirement.request.CreateRequirementRequest;
import me.nghlong3004.vqc.api.requirement.response.RequirementListItemResponse;
import me.nghlong3004.vqc.api.requirement.response.RequirementPageResponse;
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
import org.springframework.data.domain.Pageable;
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

  @Test
  void listRequirementsReturnsRequirementPage() throws Exception {
    RecordingRequirementService.requirementPageResponse =
        new RequirementPageResponse(
            List.of(
                new RequirementListItemResponse(
                    UUID.fromString("ebd7f0f0-4924-4e81-9795-d1f060bec2f2"),
                    UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"),
                    "Evaluate Apple Health step-count answers.",
                    1,
                    RequirementStatus.ACTIVE,
                    OffsetDateTime.parse("2026-06-08T10:30:00Z"))),
            0,
            20,
            1,
            1);

    mockMvc
        .perform(
            get("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/requirements")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .queryParam("status", "ACTIVE")
                .queryParam("page", "0")
                .queryParam("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].publicId").value("ebd7f0f0-4924-4e81-9795-d1f060bec2f2"))
        .andExpect(
            jsonPath("$.items[0].projectPublicId")
                .value("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"))
        .andExpect(jsonPath("$.items[0].content").value("Evaluate Apple Health step-count answers."))
        .andExpect(jsonPath("$.items[0].version").value(1))
        .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalItems").value(1))
        .andExpect(jsonPath("$.totalPages").value(1));

    assertThat(RecordingRequirementService.projectPublicId)
        .isEqualTo(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"));
    assertThat(RecordingRequirementService.status).isEqualTo(RequirementStatus.ACTIVE);
    assertThat(RecordingRequirementService.pageable.getPageNumber()).isZero();
    assertThat(RecordingRequirementService.pageable.getPageSize()).isEqualTo(20);
    assertThat(RecordingRequirementService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void listRequirementsReturnsValidationProblemDetailsForInvalidStatus() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/requirements")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .queryParam("status", "UNKNOWN"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(
            jsonPath("$.instance")
                .value("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/requirements"));

    assertThat(RecordingRequirementService.requirementPageResponse).isNull();
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
    private static RequirementStatus status;
    private static Pageable pageable;
    private static RequirementResponse requirementResponse;
    private static RequirementPageResponse requirementPageResponse;

    private static void reset() {
      projectPublicId = null;
      createRequirementRequest = null;
      username = null;
      status = null;
      pageable = null;
      requirementResponse = null;
      requirementPageResponse = null;
    }

    @Override
    public RequirementResponse createRequirement(
        UUID projectPublicId, CreateRequirementRequest request, String username) {
      RecordingRequirementService.projectPublicId = projectPublicId;
      RecordingRequirementService.createRequirementRequest = request;
      RecordingRequirementService.username = username;
      return requirementResponse;
    }

    @Override
    public RequirementPageResponse listRequirements(
        UUID projectPublicId, RequirementStatus status, Pageable pageable, String username) {
      RecordingRequirementService.projectPublicId = projectPublicId;
      RecordingRequirementService.status = status;
      RecordingRequirementService.pageable = pageable;
      RecordingRequirementService.username = username;
      return requirementPageResponse;
    }
  }
}
