package me.nghlong3004.vqc.api.project.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.exception.GlobalException;
import me.nghlong3004.vqc.api.project.enums.ProjectStatus;
import me.nghlong3004.vqc.api.project.request.CreateProjectRequest;
import me.nghlong3004.vqc.api.project.response.ProjectCreatorResponse;
import me.nghlong3004.vqc.api.project.response.ProjectResponse;
import me.nghlong3004.vqc.api.project.service.ProjectService;
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
    controllers = ProjectController.class,
    excludeAutoConfiguration = {
      OAuth2ClientAutoConfiguration.class,
      OAuth2ClientWebSecurityAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalException.class, ProjectControllerTest.MockBeans.class})
class ProjectControllerTest {

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void resetTestDoubles() {
    RecordingProjectService.reset();
  }

  @Test
  void createProjectReturnsCreatedProject() throws Exception {
    RecordingProjectService.createProjectResponse =
        new ProjectResponse(
            UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"),
            "AI Health Chatbot Demo",
            "Evaluate health chatbot answers.",
            "Health assistant QA evaluation",
            30,
            ProjectStatus.ACTIVE,
            new ProjectCreatorResponse(
                UUID.fromString("7b7b7d42-5f42-4c5a-9281-8d1d36f6f59d"), "QC Demo"),
            OffsetDateTime.parse("2026-06-08T10:30:00Z"),
            OffsetDateTime.parse("2026-06-08T10:30:00Z"));

    mockMvc
        .perform(
            post("/api/v1/projects")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "AI Health Chatbot Demo",
                      "description": "Evaluate health chatbot answers.",
                      "evaluationScope": "Health assistant QA evaluation",
                      "retentionDays": 30
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.publicId").value("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"))
        .andExpect(jsonPath("$.name").value("AI Health Chatbot Demo"))
        .andExpect(jsonPath("$.description").value("Evaluate health chatbot answers."))
        .andExpect(jsonPath("$.evaluationScope").value("Health assistant QA evaluation"))
        .andExpect(jsonPath("$.retentionDays").value(30))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(
            jsonPath("$.createdBy.publicId").value("7b7b7d42-5f42-4c5a-9281-8d1d36f6f59d"))
        .andExpect(jsonPath("$.createdBy.displayName").value("QC Demo"));

    assertThat(RecordingProjectService.username).isEqualTo("qc.demo@example.com");
    assertThat(RecordingProjectService.createProjectRequest.name())
        .isEqualTo("AI Health Chatbot Demo");
    assertThat(RecordingProjectService.createProjectRequest.retentionDays()).isEqualTo(30);
  }

  @Test
  void createProjectReturnsValidationProblemDetails() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/projects")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": " ",
                      "retentionDays": 0
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.instance").value("/api/v1/projects"))
        .andExpect(jsonPath("$.errors[*].field", hasItems("name", "retentionDays")));

    assertThat(RecordingProjectService.createProjectRequest).isNull();
    assertThat(RecordingProjectService.username).isNull();
  }

  @TestConfiguration
  static class MockBeans {

    @Bean
    RecordingProjectService projectService() {
      return new RecordingProjectService();
    }
  }

  static class RecordingProjectService implements ProjectService {
    private static CreateProjectRequest createProjectRequest;
    private static ProjectResponse createProjectResponse;
    private static String username;

    @Override
    public ProjectResponse createProject(CreateProjectRequest request, String username) {
      RecordingProjectService.createProjectRequest = request;
      RecordingProjectService.username = username;
      return createProjectResponse;
    }

    private static void reset() {
      createProjectRequest = null;
      createProjectResponse = null;
      username = null;
    }
  }
}
