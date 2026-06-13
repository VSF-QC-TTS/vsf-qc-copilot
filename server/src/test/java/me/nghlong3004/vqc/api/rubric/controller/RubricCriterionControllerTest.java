package me.nghlong3004.vqc.api.rubric.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import me.nghlong3004.vqc.api.exception.GlobalException;
import me.nghlong3004.vqc.api.rubric.request.CreateRubricCriterionRequest;
import me.nghlong3004.vqc.api.rubric.request.UpdateRubricCriterionRequest;
import me.nghlong3004.vqc.api.rubric.response.RubricCriterionPageResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricCriterionResponse;
import me.nghlong3004.vqc.api.rubric.service.RubricCriterionService;
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
    controllers = RubricCriterionController.class,
    excludeAutoConfiguration = {
      OAuth2ClientAutoConfiguration.class,
      OAuth2ClientWebSecurityAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalException.class, RubricCriterionControllerTest.MockBeans.class})
class RubricCriterionControllerTest {

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void resetTestDoubles() {
    RecordingRubricCriterionService.reset();
  }

  @Test
  void createCriterionReturnsCreatedCriterion() throws Exception {
    RecordingRubricCriterionService.criterionResponse = criterionResponse();

    mockMvc
        .perform(
            post("/api/v1/rubric-versions/5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a/criteria")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Correctness",
                      "description": "Checks factual match.",
                      "weight": 4,
                      "passCondition": "Facts match.",
                      "failCondition": "Facts are wrong.",
                      "judgeInstruction": "Compare with ground truth.",
                      "metricKey": "correctness",
                      "isCritical": true,
                      "sortOrder": 1
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.publicId").value("d10d218f-0e3c-4771-bf80-9815751f6460"))
        .andExpect(jsonPath("$.rubricVersionPublicId").value("5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a"))
        .andExpect(jsonPath("$.name").value("Correctness"))
        .andExpect(jsonPath("$.weight").value(4))
        .andExpect(jsonPath("$.metricKey").value("correctness"))
        .andExpect(jsonPath("$.isCritical").value(true))
        .andExpect(jsonPath("$.sortOrder").value(1));

    assertThat(RecordingRubricCriterionService.rubricVersionPublicId)
        .isEqualTo(UUID.fromString("5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a"));
    assertThat(RecordingRubricCriterionService.createRequest.metricKey()).isEqualTo("correctness");
  }

  @Test
  void createCriterionReturnsValidationProblemDetails() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/rubric-versions/5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a/criteria")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": " ",
                      "weight": 150,
                      "judgeInstruction": " ",
                      "metricKey": "Invalid-Key"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[*].field", hasItem("name")))
        .andExpect(jsonPath("$.errors[*].field", hasItem("weight")))
        .andExpect(jsonPath("$.errors[*].field", hasItem("judgeInstruction")))
        .andExpect(jsonPath("$.errors[*].field", hasItem("metricKey")));

    assertThat(RecordingRubricCriterionService.createRequest).isNull();
  }

  @Test
  void listCriteriaReturnsCriterionPage() throws Exception {
    RecordingRubricCriterionService.criterionPageResponse =
        new RubricCriterionPageResponse(List.of(criterionResponse()), 0, 100, 1, 1);

    mockMvc
        .perform(
            get("/api/v1/rubric-versions/5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a/criteria")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].publicId").value("d10d218f-0e3c-4771-bf80-9815751f6460"))
        .andExpect(jsonPath("$.items[0].metricKey").value("correctness"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(100));

    assertThat(RecordingRubricCriterionService.pageable.getPageSize()).isEqualTo(100);
  }

  @Test
  void updateCriterionReturnsUpdatedCriterion() throws Exception {
    RecordingRubricCriterionService.criterionResponse = criterionResponse();

    mockMvc
        .perform(
            patch("/api/v1/rubric-criteria/d10d218f-0e3c-4771-bf80-9815751f6460")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "weight": 5,
                      "sortOrder": 2
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value("d10d218f-0e3c-4771-bf80-9815751f6460"));

    assertThat(RecordingRubricCriterionService.updateRequest.weight())
        .isEqualTo(5);
  }

  @Test
  void deleteCriterionReturnsNoContent() throws Exception {
    mockMvc
        .perform(
            delete("/api/v1/rubric-criteria/d10d218f-0e3c-4771-bf80-9815751f6460")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isNoContent());

    assertThat(RecordingRubricCriterionService.criterionPublicId)
        .isEqualTo(UUID.fromString("d10d218f-0e3c-4771-bf80-9815751f6460"));
  }

  private RubricCriterionResponse criterionResponse() {
    return new RubricCriterionResponse(
        UUID.fromString("d10d218f-0e3c-4771-bf80-9815751f6460"),
        UUID.fromString("5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a"),
        "Correctness",
        "Checks factual match.",
        4,
        "Facts match.",
        "Facts are wrong.",
        "Compare with ground truth.",
        "correctness",
        true,
        1,
        OffsetDateTime.parse("2026-06-08T10:50:00Z"),
        OffsetDateTime.parse("2026-06-08T10:55:00Z"));
  }

  @TestConfiguration
  static class MockBeans {

    @Bean
    RubricCriterionService rubricCriterionService() {
      return new RecordingRubricCriterionService();
    }
  }

  static class RecordingRubricCriterionService implements RubricCriterionService {

    static UUID rubricVersionPublicId;
    static UUID criterionPublicId;
    static Pageable pageable;
    static String username;
    static CreateRubricCriterionRequest createRequest;
    static UpdateRubricCriterionRequest updateRequest;
    static RubricCriterionResponse criterionResponse;
    static RubricCriterionPageResponse criterionPageResponse;

    static void reset() {
      rubricVersionPublicId = null;
      criterionPublicId = null;
      pageable = null;
      username = null;
      createRequest = null;
      updateRequest = null;
      criterionResponse = null;
      criterionPageResponse = null;
    }

    @Override
    public RubricCriterionResponse createCriterion(
        UUID rubricVersionPublicId, CreateRubricCriterionRequest request, String username) {
      RecordingRubricCriterionService.rubricVersionPublicId = rubricVersionPublicId;
      RecordingRubricCriterionService.createRequest = request;
      RecordingRubricCriterionService.username = username;
      return criterionResponse;
    }

    @Override
    public RubricCriterionPageResponse listCriteria(
        UUID rubricVersionPublicId, Pageable pageable, String username) {
      RecordingRubricCriterionService.rubricVersionPublicId = rubricVersionPublicId;
      RecordingRubricCriterionService.pageable = pageable;
      RecordingRubricCriterionService.username = username;
      return criterionPageResponse;
    }

    @Override
    public RubricCriterionResponse updateCriterion(
        UUID criterionPublicId, UpdateRubricCriterionRequest request, String username) {
      RecordingRubricCriterionService.criterionPublicId = criterionPublicId;
      RecordingRubricCriterionService.updateRequest = request;
      RecordingRubricCriterionService.username = username;
      return criterionResponse;
    }

    @Override
    public void deleteCriterion(UUID criterionPublicId, String username) {
      RecordingRubricCriterionService.criterionPublicId = criterionPublicId;
      RecordingRubricCriterionService.username = username;
    }
  }
}
