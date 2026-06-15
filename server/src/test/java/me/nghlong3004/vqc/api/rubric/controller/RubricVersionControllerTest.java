package me.nghlong3004.vqc.api.rubric.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import me.nghlong3004.vqc.api.exception.GlobalException;
import me.nghlong3004.vqc.api.rubric.enums.RubricVersionStatus;
import me.nghlong3004.vqc.api.rubric.request.UpdateRubricVersionRequest;
import me.nghlong3004.vqc.api.rubric.response.RubricVersionListItemResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricVersionPageResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricVersionResponse;
import me.nghlong3004.vqc.api.rubric.service.RubricVersionService;
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
    controllers = RubricVersionController.class,
    excludeAutoConfiguration = {
      OAuth2ClientAutoConfiguration.class,
      OAuth2ClientWebSecurityAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalException.class, RubricVersionControllerTest.MockBeans.class})
class RubricVersionControllerTest {

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void resetTestDoubles() {
    RecordingRubricVersionService.reset();
  }

  @Test
  void createVersionReturnsCreatedDraftVersion() throws Exception {
    RecordingRubricVersionService.versionResponse = versionResponse(RubricVersionStatus.DRAFT);

    mockMvc
        .perform(
            post("/api/v1/rubrics/3c5582c5-96d8-40e4-9aa1-168f6d27c9dc/versions")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.publicId").value("5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a"))
        .andExpect(jsonPath("$.rubricPublicId").value("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"))
        .andExpect(jsonPath("$.versionNumber").value(2))
        .andExpect(jsonPath("$.status").value("DRAFT"));

    assertThat(RecordingRubricVersionService.rubricPublicId)
        .isEqualTo(UUID.fromString("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"));
    assertThat(RecordingRubricVersionService.sourceVersionPublicId).isNull();
    assertThat(RecordingRubricVersionService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void createVersionAcceptsSourceVersionToClone() throws Exception {
    RecordingRubricVersionService.versionResponse = versionResponse(RubricVersionStatus.DRAFT);

    mockMvc
        .perform(
            post("/api/v1/rubrics/3c5582c5-96d8-40e4-9aa1-168f6d27c9dc/versions")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sourceVersionPublicId": "8cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("DRAFT"));

    assertThat(RecordingRubricVersionService.rubricPublicId)
        .isEqualTo(UUID.fromString("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"));
    assertThat(RecordingRubricVersionService.sourceVersionPublicId)
        .isEqualTo(UUID.fromString("8cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a"));
    assertThat(RecordingRubricVersionService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void listVersionsReturnsVersionPage() throws Exception {
    RecordingRubricVersionService.versionPageResponse =
        new RubricVersionPageResponse(
            List.of(
                new RubricVersionListItemResponse(
                    UUID.fromString("5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a"),
                    UUID.fromString("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"),
                    "Health Answer Quality Rubric",
                    2,
                    RubricVersionStatus.DRAFT,
                    3,
                    OffsetDateTime.parse("2026-06-08T10:45:00Z"),
                    null)),
            0,
            20,
            1,
            1);

    mockMvc
        .perform(
            get("/api/v1/rubrics/3c5582c5-96d8-40e4-9aa1-168f6d27c9dc/versions")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .queryParam("status", "DRAFT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].publicId").value("5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a"))
        .andExpect(jsonPath("$.items[0].versionNumber").value(2))
        .andExpect(jsonPath("$.items[0].status").value("DRAFT"))
        .andExpect(jsonPath("$.items[0].criteriaCount").value(3));

    assertThat(RecordingRubricVersionService.status).isEqualTo(RubricVersionStatus.DRAFT);
    assertThat(RecordingRubricVersionService.pageable.getPageSize()).isEqualTo(20);
  }

  @Test
  void getVersionReturnsVersionDetail() throws Exception {
    RecordingRubricVersionService.versionResponse = versionResponse(RubricVersionStatus.PUBLISHED);

    mockMvc
        .perform(
            get("/api/v1/rubric-versions/5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value("5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a"))
        .andExpect(jsonPath("$.status").value("PUBLISHED"));

    assertThat(RecordingRubricVersionService.rubricVersionPublicId)
        .isEqualTo(UUID.fromString("5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a"));
  }

  @Test
  void updateVersionReturnsPublishedVersion() throws Exception {
    RecordingRubricVersionService.versionResponse = versionResponse(RubricVersionStatus.PUBLISHED);

    mockMvc
        .perform(
            patch("/api/v1/rubric-versions/5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "status": "PUBLISHED"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PUBLISHED"));

    assertThat(RecordingRubricVersionService.updateRequest.status())
        .isEqualTo(RubricVersionStatus.PUBLISHED);
  }

  @Test
  void updateVersionContentReturnsUpdatedDraft() throws Exception {
    RecordingRubricVersionService.versionResponse =
        new RubricVersionResponse(
            UUID.fromString("5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a"),
            UUID.fromString("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"),
            "Health Answer Quality Rubric",
            2,
            RubricVersionStatus.DRAFT,
            "Updated rubric content.",
            "{\"type\":\"object\"}",
            0,
            OffsetDateTime.parse("2026-06-08T10:45:00Z"),
            null,
            List.of());

    mockMvc
        .perform(
            patch("/api/v1/rubric-versions/5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "content": "Updated rubric content.",
                      "outputSchemaJson": "{\\"type\\":\\"object\\"}"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("Updated rubric content."))
        .andExpect(jsonPath("$.outputSchemaJson").value("{\"type\":\"object\"}"));

    assertThat(RecordingRubricVersionService.updateRequest.status()).isNull();
    assertThat(RecordingRubricVersionService.updateRequest.content())
        .isEqualTo("Updated rubric content.");
  }

  private RubricVersionResponse versionResponse(RubricVersionStatus status) {
    return new RubricVersionResponse(
        UUID.fromString("5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a"),
        UUID.fromString("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"),
        "Health Answer Quality Rubric",
        2,
        status,
        "Judge actual response against expected answer.",
        "{\"type\":\"object\"}",
        0,
        OffsetDateTime.parse("2026-06-08T10:45:00Z"),
        status == RubricVersionStatus.PUBLISHED
            ? OffsetDateTime.parse("2026-06-08T11:00:00Z")
            : null,
        List.of());
  }

  @TestConfiguration
  static class MockBeans {

    @Bean
    RubricVersionService rubricVersionService() {
      return new RecordingRubricVersionService();
    }
  }

  static class RecordingRubricVersionService implements RubricVersionService {

    static UUID rubricPublicId;
    static UUID sourceVersionPublicId;
    static UUID rubricVersionPublicId;
    static RubricVersionStatus status;
    static Pageable pageable;
    static String username;
    static UpdateRubricVersionRequest updateRequest;
    static RubricVersionResponse versionResponse;
    static RubricVersionPageResponse versionPageResponse;

    static void reset() {
      rubricPublicId = null;
      sourceVersionPublicId = null;
      rubricVersionPublicId = null;
      status = null;
      pageable = null;
      username = null;
      updateRequest = null;
      versionResponse = null;
      versionPageResponse = null;
    }

    @Override
    public RubricVersionResponse createVersion(UUID rubricPublicId, String username) {
      return createVersion(rubricPublicId, null, username);
    }

    @Override
    public RubricVersionResponse createVersion(
        UUID rubricPublicId, UUID sourceVersionPublicId, String username) {
      RecordingRubricVersionService.rubricPublicId = rubricPublicId;
      RecordingRubricVersionService.sourceVersionPublicId = sourceVersionPublicId;
      RecordingRubricVersionService.username = username;
      return versionResponse;
    }

    @Override
    public RubricVersionPageResponse listVersions(
        UUID rubricPublicId, RubricVersionStatus status, Pageable pageable, String username) {
      RecordingRubricVersionService.rubricPublicId = rubricPublicId;
      RecordingRubricVersionService.status = status;
      RecordingRubricVersionService.pageable = pageable;
      RecordingRubricVersionService.username = username;
      return versionPageResponse;
    }

    @Override
    public RubricVersionPageResponse listUserVersions(
        RubricVersionStatus status, Pageable pageable, String username) {
      RecordingRubricVersionService.status = status;
      RecordingRubricVersionService.pageable = pageable;
      RecordingRubricVersionService.username = username;
      return versionPageResponse;
    }

    @Override
    public RubricVersionResponse getVersion(UUID rubricVersionPublicId, String username) {
      RecordingRubricVersionService.rubricVersionPublicId = rubricVersionPublicId;
      RecordingRubricVersionService.username = username;
      return versionResponse;
    }

    @Override
    public RubricVersionResponse updateVersion(
        UUID rubricVersionPublicId, UpdateRubricVersionRequest request, String username) {
      RecordingRubricVersionService.rubricVersionPublicId = rubricVersionPublicId;
      RecordingRubricVersionService.updateRequest = request;
      RecordingRubricVersionService.username = username;
      return versionResponse;
    }
  }
}
