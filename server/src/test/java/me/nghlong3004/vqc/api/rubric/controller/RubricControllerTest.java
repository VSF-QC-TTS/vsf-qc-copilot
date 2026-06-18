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
import me.nghlong3004.vqc.api.rubric.enums.RubricStatus;
import me.nghlong3004.vqc.api.rubric.request.CreateRubricRequest;
import me.nghlong3004.vqc.api.rubric.request.CreateRubricWithVersionRequest;
import me.nghlong3004.vqc.api.rubric.request.GenerateRubricPreviewRequest;
import me.nghlong3004.vqc.api.rubric.request.UpdateRubricRequest;
import me.nghlong3004.vqc.api.rubric.response.RubricListItemResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricPageResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricVersionResponse;
import me.nghlong3004.vqc.api.rubric.request.CreateRubricCriterionRequest;
import me.nghlong3004.vqc.api.rubric.service.RubricService;
import me.nghlong3004.vqc.api.rubric.service.RubricWorkflowService;
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
    controllers = RubricController.class,
    excludeAutoConfiguration = {
      OAuth2ClientAutoConfiguration.class,
      OAuth2ClientWebSecurityAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalException.class, RubricControllerTest.MockBeans.class})
class RubricControllerTest {

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void resetTestDoubles() {
    RecordingRubricService.reset();
    RecordingRubricWorkflowService.reset();
  }

  @Test
  void generateAndCreateRubricReturnsDraftVersion() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/rubrics/generate")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Healthcare chatbot",
                      "evaluationGoal": "Check medical answer quality",
                      "domainContext": "Vietnamese healthcare chatbot"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.publicId").value("5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a"))
        .andExpect(jsonPath("$.rubricPublicId").value("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"))
        .andExpect(jsonPath("$.rubricName").value("Healthcare chatbot"))
        .andExpect(jsonPath("$.versionNumber").value(1));

    assertThat(RecordingRubricWorkflowService.generateRequest.evaluationGoal())
        .isEqualTo("Check medical answer quality");
    assertThat(RecordingRubricWorkflowService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void generateAndCreateRubricValidatesName() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/rubrics/generate")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "",
                      "evaluationGoal": "Check quality"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field", hasItem("name")));
  }

  @Test
  void generateAndCreateRubricValidatesEvaluationGoal() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/rubrics/generate")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Test Rubric",
                      "evaluationGoal": ""
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field", hasItem("evaluationGoal")));
  }

  @Test
  void createUserScopedRubricReturnsDraftVersion() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/rubrics")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Reusable Rubric",
                      "description": "Shared judge logic.",
                      "content": "Judge correctness and safety.",
                      "outputSchemaJson": "{\\"type\\":\\"object\\"}"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.publicId").value("5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a"))
        .andExpect(jsonPath("$.rubricPublicId").value("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"))
        .andExpect(jsonPath("$.rubricName").value("Reusable Rubric"))
        .andExpect(jsonPath("$.versionNumber").value(1))
        .andExpect(jsonPath("$.content").value("Judge correctness and safety."));

    assertThat(RecordingRubricWorkflowService.createRequest.name()).isEqualTo("Reusable Rubric");
    assertThat(RecordingRubricWorkflowService.createRequest.content())
        .isEqualTo("Judge correctness and safety.");
    assertThat(RecordingRubricWorkflowService.username).isEqualTo("qc.demo@example.com");
  }


  @Test
  void listMyRubricsReturnsRubricPage() throws Exception {
    RecordingRubricService.rubricPageResponse =
        new RubricPageResponse(
            List.of(
                new RubricListItemResponse(
                    UUID.fromString("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"),
                    false,
                    "Health Answer Quality Rubric",
                    null,
                    RubricStatus.ACTIVE,
                    OffsetDateTime.parse("2026-06-08T10:30:00Z"))),
            0,
            20,
            1,
            1);

    mockMvc
        .perform(
            get("/api/v1/rubrics")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .queryParam("status", "ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].publicId").value("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"))
        .andExpect(jsonPath("$.items[0].name").value("Health Answer Quality Rubric"))
        .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.totalItems").value(1));

    assertThat(RecordingRubricService.status).isEqualTo(RubricStatus.ACTIVE);
    assertThat(RecordingRubricService.pageable.getPageSize()).isEqualTo(20);
  }

  @Test
  void getRubricReturnsRubricDetail() throws Exception {
    RecordingRubricService.rubricResponse = rubricResponse();

    mockMvc
        .perform(
            get("/api/v1/rubrics/3c5582c5-96d8-40e4-9aa1-168f6d27c9dc")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"))
        .andExpect(jsonPath("$.name").value("Health Answer Quality Rubric"));

    assertThat(RecordingRubricService.rubricPublicId)
        .isEqualTo(UUID.fromString("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"));
  }

  @Test
  void updateRubricReturnsUpdatedRubric() throws Exception {
    RecordingRubricService.rubricResponse =
        new RubricResponse(
            UUID.fromString("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"),
            false,
            "Updated Rubric",
            "Updated description",
            null,
            RubricStatus.ACTIVE,
            OffsetDateTime.parse("2026-06-08T10:30:00Z"),
            OffsetDateTime.parse("2026-06-08T10:35:00Z"),
            null);

    mockMvc
        .perform(
            patch("/api/v1/rubrics/3c5582c5-96d8-40e4-9aa1-168f6d27c9dc")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Updated Rubric",
                      "description": "Updated description"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Rubric"))
        .andExpect(jsonPath("$.description").value("Updated description"));

    assertThat(RecordingRubricService.updateRubricRequest.name()).isEqualTo("Updated Rubric");
  }

  @Test
  void archiveRubricReturnsNoContent() throws Exception {
    mockMvc
        .perform(
            delete("/api/v1/rubrics/3c5582c5-96d8-40e4-9aa1-168f6d27c9dc")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isNoContent());

    assertThat(RecordingRubricService.rubricPublicId)
        .isEqualTo(UUID.fromString("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"));
    assertThat(RecordingRubricService.username).isEqualTo("qc.demo@example.com");
  }

  private RubricResponse rubricResponse() {
    return new RubricResponse(
        UUID.fromString("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"),
        false,
        "Health Answer Quality Rubric",
        "Checks correctness and safety.",
        null,
        RubricStatus.ACTIVE,
        OffsetDateTime.parse("2026-06-08T10:30:00Z"),
        OffsetDateTime.parse("2026-06-08T10:30:00Z"),
        null);
  }

  @TestConfiguration
  static class MockBeans {

    @Bean
    RubricService rubricService() {
      return new RecordingRubricService();
    }

    @Bean
    RubricWorkflowService rubricWorkflowService() {
      return new RecordingRubricWorkflowService();
    }

  }

  static class RecordingRubricWorkflowService implements RubricWorkflowService {

    static CreateRubricWithVersionRequest createRequest;
    static GenerateRubricPreviewRequest generateRequest;
    static String username;

    @Override
    public RubricVersionResponse createRubricWithVersion(
        CreateRubricWithVersionRequest request, String username) {
      RecordingRubricWorkflowService.createRequest = request;
      RecordingRubricWorkflowService.username = username;
      return versionResponse(request.name(), request.content(), request.outputSchemaJson());
    }

    @Override
    public RubricVersionResponse generateAndCreateRubric(
        GenerateRubricPreviewRequest request, String username) {
      RecordingRubricWorkflowService.generateRequest = request;
      RecordingRubricWorkflowService.username = username;
      return versionResponse(request.name(), "AI-generated content", null);
    }

    private static RubricVersionResponse versionResponse(
        String name, String content, String outputSchemaJson) {
      return new RubricVersionResponse(
          UUID.fromString("5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a"),
          UUID.fromString("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"),
          name,
          1,
          me.nghlong3004.vqc.api.rubric.enums.RubricVersionStatus.DRAFT,
          content,
          outputSchemaJson,
          0,
          OffsetDateTime.parse("2026-06-08T10:30:00Z"),
          null,
          List.of());
    }

    static void reset() {
      createRequest = null;
      generateRequest = null;
      username = null;
    }
  }

  static class RecordingRubricService implements RubricService {

    static UUID projectPublicId;
    static UUID rubricPublicId;
    static RubricStatus status;
    static Pageable pageable;
    static String username;
    static CreateRubricRequest createRubricRequest;
    static UpdateRubricRequest updateRubricRequest;
    static RubricResponse rubricResponse;
    static RubricPageResponse rubricPageResponse;

    static void reset() {
      projectPublicId = null;
      rubricPublicId = null;
      status = null;
      pageable = null;
      username = null;
      createRubricRequest = null;
      updateRubricRequest = null;
      rubricResponse = null;
      rubricPageResponse = null;
    }


    @Override
    public RubricResponse getRubric(UUID rubricPublicId, String username) {
      RecordingRubricService.rubricPublicId = rubricPublicId;
      RecordingRubricService.username = username;
      return rubricResponse;
    }

    @Override
    public RubricResponse updateRubric(
        UUID rubricPublicId, UpdateRubricRequest request, String username) {
      RecordingRubricService.rubricPublicId = rubricPublicId;
      RecordingRubricService.updateRubricRequest = request;
      RecordingRubricService.username = username;
      return rubricResponse;
    }

    @Override
    public void archiveRubric(UUID rubricPublicId, String username) {
      RecordingRubricService.rubricPublicId = rubricPublicId;
      RecordingRubricService.username = username;
    }

    @Override
    public RubricPageResponse listMyRubrics(
        RubricStatus status, Pageable pageable, String username) {
      RecordingRubricService.status = status;
      RecordingRubricService.pageable = pageable;
      RecordingRubricService.username = username;
      return rubricPageResponse;
    }

    @Override
    public RubricPageResponse listTemplates(Pageable pageable, String username) {
      RecordingRubricService.pageable = pageable;
      RecordingRubricService.username = username;
      return rubricPageResponse;
    }

    @Override
    public RubricResponse cloneRubric(UUID rubricPublicId, String username) {
      RecordingRubricService.rubricPublicId = rubricPublicId;
      RecordingRubricService.username = username;
      return rubricResponse;
    }
  }
}
