package me.nghlong3004.vqc.api.targetconnector.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.nghlong3004.vqc.api.exception.GlobalException;
import me.nghlong3004.vqc.api.targetconnector.enums.AuthType;
import me.nghlong3004.vqc.api.targetconnector.enums.BodyType;
import me.nghlong3004.vqc.api.targetconnector.enums.HttpMethodType;
import me.nghlong3004.vqc.api.targetconnector.enums.ResponseFormat;
import me.nghlong3004.vqc.api.targetconnector.request.CreateTargetApiConnectorRequest;
import me.nghlong3004.vqc.api.targetconnector.response.SecretRefResponse;
import me.nghlong3004.vqc.api.targetconnector.response.TargetApiConnectorListItemResponse;
import me.nghlong3004.vqc.api.targetconnector.response.TargetApiConnectorPageResponse;
import me.nghlong3004.vqc.api.targetconnector.response.TargetApiConnectorResponse;
import me.nghlong3004.vqc.api.targetconnector.service.TargetApiConnectorService;
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
    controllers = TargetApiConnectorController.class,
    excludeAutoConfiguration = {
      OAuth2ClientAutoConfiguration.class,
      OAuth2ClientWebSecurityAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalException.class, TargetApiConnectorControllerTest.MockBeans.class})
class TargetApiConnectorControllerTest {

  private static final String PROJECT_ID = "5a4edcc1-cd1e-44ef-a144-31f5f3d2f653";

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void resetTestDoubles() {
    RecordingTargetApiConnectorService.reset();
  }

  @Test
  void createConnectorReturnsCreatedConnectorWithMaskedSecretRefs() throws Exception {
    RecordingTargetApiConnectorService.response =
        new TargetApiConnectorResponse(
            UUID.fromString("f5f77e84-b3be-48bb-9081-f1dd190f8c61"),
            UUID.fromString(PROJECT_ID),
            "Mock Health Chatbot",
            "Local mock chatbot for demo.",
            HttpMethodType.POST,
            "http://localhost:8080",
            "/mock-chatbot/chat",
            "http://localhost:8080/mock-chatbot/chat",
            Map.of("Authorization", "Bearer {{secret:CHATBOT_API_TOKEN}}"),
            Map.of(),
            Map.of(),
            BodyType.RAW_JSON,
            Map.of("message", "{{question}}"),
            null,
            AuthType.BEARER,
            Map.of("tokenRef", "{{secret:CHATBOT_API_TOKEN}}"),
            List.of(new SecretRefResponse("CHATBOT_API_TOKEN", "****alue")),
            ResponseFormat.JSON,
            "$.answer",
            false,
            null,
            null,
            60,
            1,
            true,
            null,
            null);

    mockMvc
        .perform(
            post("/api/v1/projects/" + PROJECT_ID + "/target-api-connectors")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Mock Health Chatbot",
                      "method": "POST",
                      "url": "http://localhost:8080/mock-chatbot/chat",
                      "headers": {
                        "Authorization": "Bearer {{secret:CHATBOT_API_TOKEN}}"
                      },
                      "bodyType": "RAW_JSON",
                      "bodyTemplate": {
                        "message": "{{question}}"
                      },
                      "authType": "BEARER",
                      "authConfig": {
                        "tokenRef": "{{secret:CHATBOT_API_TOKEN}}"
                      },
                      "secretValues": {
                        "CHATBOT_API_TOKEN": "write-only-token-value"
                      },
                      "responseFormat": "JSON",
                      "responseSelector": "$.answer"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.publicId").value("f5f77e84-b3be-48bb-9081-f1dd190f8c61"))
        .andExpect(jsonPath("$.projectPublicId").value(PROJECT_ID))
        .andExpect(jsonPath("$.name").value("Mock Health Chatbot"))
        .andExpect(jsonPath("$.headers.Authorization").value("Bearer {{secret:CHATBOT_API_TOKEN}}"))
        .andExpect(jsonPath("$.secretRefs[0].secretKey").value("CHATBOT_API_TOKEN"))
        .andExpect(jsonPath("$.secretRefs[0].maskedValue").value("****alue"))
        .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("write-only-token-value"))));

    assertThat(RecordingTargetApiConnectorService.projectPublicId)
        .isEqualTo(UUID.fromString(PROJECT_ID));
    assertThat(RecordingTargetApiConnectorService.username).isEqualTo("qc.demo@example.com");
    assertThat(RecordingTargetApiConnectorService.request.secretValues())
        .containsEntry("CHATBOT_API_TOKEN", "write-only-token-value");
  }

  @Test
  void createConnectorReturnsValidationProblemDetails() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/projects/" + PROJECT_ID + "/target-api-connectors")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": " ",
                      "url": " ",
                      "responseSelector": " "
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(
            jsonPath("$.errors[*].field", hasItems("name", "method", "url", "responseSelector")));

    assertThat(RecordingTargetApiConnectorService.request).isNull();
  }

  @Test
  void listConnectorsReturnsConnectorPage() throws Exception {
    RecordingTargetApiConnectorService.pageResponse =
        new TargetApiConnectorPageResponse(
            List.of(
                new TargetApiConnectorListItemResponse(
                    UUID.fromString("f5f77e84-b3be-48bb-9081-f1dd190f8c61"),
                    UUID.fromString(PROJECT_ID),
                    "Mock Health Chatbot",
                    HttpMethodType.POST,
                    "http://localhost:8080/mock-chatbot/chat",
                    "$.answer",
                    false,
                    true,
                    null)),
            0,
            20,
            1,
            1);

    mockMvc
        .perform(
            get("/api/v1/projects/" + PROJECT_ID + "/target-api-connectors")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null))
                .queryParam("page", "0")
                .queryParam("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].publicId").value("f5f77e84-b3be-48bb-9081-f1dd190f8c61"))
        .andExpect(jsonPath("$.items[0].projectPublicId").value(PROJECT_ID))
        .andExpect(jsonPath("$.items[0].name").value("Mock Health Chatbot"))
        .andExpect(jsonPath("$.items[0].method").value("POST"))
        .andExpect(jsonPath("$.items[0].url").value("http://localhost:8080/mock-chatbot/chat"))
        .andExpect(jsonPath("$.items[0].responseSelector").value("$.answer"))
        .andExpect(jsonPath("$.items[0].isStreaming").value(false))
        .andExpect(jsonPath("$.items[0].active").value(true))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalItems").value(1))
        .andExpect(jsonPath("$.totalPages").value(1));

    assertThat(RecordingTargetApiConnectorService.projectPublicId)
        .isEqualTo(UUID.fromString(PROJECT_ID));
    assertThat(RecordingTargetApiConnectorService.pageable.getPageNumber()).isZero();
    assertThat(RecordingTargetApiConnectorService.pageable.getPageSize()).isEqualTo(20);
    assertThat(RecordingTargetApiConnectorService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void getConnectorReturnsConnectorDetail() throws Exception {
    RecordingTargetApiConnectorService.response =
        new TargetApiConnectorResponse(
            UUID.fromString("f5f77e84-b3be-48bb-9081-f1dd190f8c61"),
            UUID.fromString(PROJECT_ID),
            "Mock Health Chatbot",
            "Local mock chatbot for demo.",
            HttpMethodType.POST,
            "http://localhost:8080",
            "/mock-chatbot/chat",
            "http://localhost:8080/mock-chatbot/chat",
            Map.of("Authorization", "Bearer {{secret:CHATBOT_API_TOKEN}}"),
            Map.of(),
            Map.of(),
            BodyType.RAW_JSON,
            Map.of("message", "{{question}}"),
            null,
            AuthType.BEARER,
            Map.of("tokenRef", "{{secret:CHATBOT_API_TOKEN}}"),
            List.of(new SecretRefResponse("CHATBOT_API_TOKEN", "****alue")),
            ResponseFormat.JSON,
            "$.answer",
            false,
            null,
            null,
            60,
            1,
            true,
            null,
            null);

    mockMvc
        .perform(
            get("/api/v1/target-api-connectors/f5f77e84-b3be-48bb-9081-f1dd190f8c61")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value("f5f77e84-b3be-48bb-9081-f1dd190f8c61"))
        .andExpect(jsonPath("$.name").value("Mock Health Chatbot"))
        .andExpect(jsonPath("$.headers.Authorization").value("Bearer {{secret:CHATBOT_API_TOKEN}}"))
        .andExpect(jsonPath("$.secretRefs[0].maskedValue").value("****alue"));

    assertThat(RecordingTargetApiConnectorService.connectorPublicId)
        .isEqualTo(UUID.fromString("f5f77e84-b3be-48bb-9081-f1dd190f8c61"));
    assertThat(RecordingTargetApiConnectorService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void getConnectorReturnsValidationProblemDetailsForInvalidPublicId() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/target-api-connectors/not-a-uuid")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.instance").value("/api/v1/target-api-connectors/not-a-uuid"));

    assertThat(RecordingTargetApiConnectorService.connectorPublicId).isNull();
  }

  @TestConfiguration
  static class MockBeans {

    @Bean
    RecordingTargetApiConnectorService targetApiConnectorService() {
      return new RecordingTargetApiConnectorService();
    }
  }

  static class RecordingTargetApiConnectorService implements TargetApiConnectorService {
    private static UUID projectPublicId;
    private static UUID connectorPublicId;
    private static CreateTargetApiConnectorRequest request;
    private static Pageable pageable;
    private static String username;
    private static TargetApiConnectorResponse response;
    private static TargetApiConnectorPageResponse pageResponse;

    @Override
    public TargetApiConnectorResponse createConnector(
        UUID projectPublicId, CreateTargetApiConnectorRequest request, String username) {
      RecordingTargetApiConnectorService.projectPublicId = projectPublicId;
      RecordingTargetApiConnectorService.request = request;
      RecordingTargetApiConnectorService.username = username;
      return response;
    }

    @Override
    public TargetApiConnectorPageResponse listConnectors(
        UUID projectPublicId, Pageable pageable, String username) {
      RecordingTargetApiConnectorService.projectPublicId = projectPublicId;
      RecordingTargetApiConnectorService.pageable = pageable;
      RecordingTargetApiConnectorService.username = username;
      return pageResponse;
    }

    @Override
    public TargetApiConnectorResponse getConnector(UUID connectorPublicId, String username) {
      RecordingTargetApiConnectorService.connectorPublicId = connectorPublicId;
      RecordingTargetApiConnectorService.username = username;
      return response;
    }

    private static void reset() {
      projectPublicId = null;
      connectorPublicId = null;
      request = null;
      pageable = null;
      username = null;
      response = null;
      pageResponse = null;
    }
  }
}
