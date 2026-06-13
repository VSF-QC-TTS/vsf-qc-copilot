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
import me.nghlong3004.vqc.api.rubric.request.UpdateRubricRequest;
import me.nghlong3004.vqc.api.rubric.response.RubricListItemResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricPageResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricResponse;
import me.nghlong3004.vqc.api.rubric.service.RubricService;
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
  }

  @Test
  void createRubricReturnsCreatedRubric() throws Exception {
    RecordingRubricService.rubricResponse = rubricResponse();

    mockMvc
        .perform(
            post("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/rubrics")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Health Answer Quality Rubric",
                      "description": "Checks correctness and safety."
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.publicId").value("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"))
        .andExpect(
            jsonPath("$.projectPublicId").value("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"))
        .andExpect(jsonPath("$.name").value("Health Answer Quality Rubric"))
        .andExpect(jsonPath("$.description").value("Checks correctness and safety."))
        .andExpect(jsonPath("$.currentVersion").isEmpty())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    assertThat(RecordingRubricService.projectPublicId)
        .isEqualTo(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"));
    assertThat(RecordingRubricService.createRubricRequest.name())
        .isEqualTo("Health Answer Quality Rubric");
    assertThat(RecordingRubricService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void createRubricReturnsValidationProblemDetails() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/rubrics")
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
        .andExpect(jsonPath("$.errors[*].field", hasItem("name")));

    assertThat(RecordingRubricService.createRubricRequest).isNull();
  }

  @Test
  void listRubricsReturnsRubricPage() throws Exception {
    RecordingRubricService.rubricPageResponse =
        new RubricPageResponse(
            List.of(
                new RubricListItemResponse(
                    UUID.fromString("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"),
                    UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"),
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
            get("/api/v1/projects/5a4edcc1-cd1e-44ef-a144-31f5f3d2f653/rubrics")
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
            UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"),
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
        UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"),
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
    public RubricResponse createRubric(
        UUID projectPublicId, CreateRubricRequest request, String username) {
      RecordingRubricService.projectPublicId = projectPublicId;
      RecordingRubricService.createRubricRequest = request;
      RecordingRubricService.username = username;
      return rubricResponse;
    }

    @Override
    public RubricPageResponse listRubrics(
        UUID projectPublicId, RubricStatus status, Pageable pageable, String username) {
      RecordingRubricService.projectPublicId = projectPublicId;
      RecordingRubricService.status = status;
      RecordingRubricService.pageable = pageable;
      RecordingRubricService.username = username;
      return rubricPageResponse;
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
