package me.nghlong3004.vqc.api.targetconnector.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.targetconnector.enums.AuthType;
import me.nghlong3004.vqc.api.targetconnector.enums.BodyType;
import me.nghlong3004.vqc.api.targetconnector.enums.HttpMethodType;
import me.nghlong3004.vqc.api.targetconnector.enums.ResponseFormat;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class TargetApiConnectorMapperTest {

  private final TargetApiConnectorMapper mapper = new TargetApiConnectorMapper();

  @Test
  void toResponseMapsConnectorAndMaskedSecretRefs() {
    Project project = new Project();
    TargetApiConnector connector = new TargetApiConnector();
    connector.setProject(project);
    connector.setName("Mock Health Chatbot");
    connector.setDescription("Local mock chatbot for demo.");
    connector.setMethod(HttpMethodType.POST);
    connector.setBaseUrl("http://localhost:8080");
    connector.setPath("/mock-chatbot/chat");
    connector.setUrl("http://localhost:8080/mock-chatbot/chat");
    connector.setHeaders(Map.of("Authorization", "Bearer {{secret:CHATBOT_API_TOKEN}}"));
    connector.setQueryParams(Map.of());
    connector.setPathParams(Map.of());
    connector.setBodyType(BodyType.RAW_JSON);
    connector.setBodyTemplate(Map.of("message", "{{question}}"));
    connector.setAuthType(AuthType.BEARER);
    connector.setAuthConfig(Map.of("tokenRef", "{{secret:CHATBOT_API_TOKEN}}"));
    connector.setSecretRefs(
        List.of(Map.of("secretKey", "CHATBOT_API_TOKEN", "maskedValue", "****alue")));
    connector.setResponseFormat(ResponseFormat.JSON);
    connector.setResponseSelector("$.answer");
    connector.setStreaming(false);
    connector.setTimeoutSeconds(60);
    connector.setRetryCount(1);
    connector.setActive(true);
    connector.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"));
    connector.setUpdatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"));

    var response = mapper.toResponse(connector);

    assertThat(response.publicId()).isEqualTo(connector.getPublicId());
    assertThat(response.projectPublicId()).isEqualTo(project.getPublicId());
    assertThat(response.name()).isEqualTo("Mock Health Chatbot");
    assertThat(response.method()).isEqualTo(HttpMethodType.POST);
    assertThat(response.headers()).containsEntry("Authorization", "Bearer {{secret:CHATBOT_API_TOKEN}}");
    assertThat(response.authConfig()).containsEntry("tokenRef", "{{secret:CHATBOT_API_TOKEN}}");
    assertThat(response.secretRefs()).hasSize(1);
    assertThat(response.secretRefs().getFirst().secretKey()).isEqualTo("CHATBOT_API_TOKEN");
    assertThat(response.secretRefs().getFirst().maskedValue()).isEqualTo("****alue");
  }

  @Test
  void toListItemResponseMapsListFields() {
    Project project = new Project();
    TargetApiConnector connector = new TargetApiConnector();
    connector.setProject(project);
    connector.setName("Mock Health Chatbot");
    connector.setMethod(HttpMethodType.POST);
    connector.setUrl("http://localhost:8080/mock-chatbot/chat");
    connector.setResponseSelector("$.answer");
    connector.setStreaming(false);
    connector.setActive(true);
    connector.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"));

    var response = mapper.toListItemResponse(connector);

    assertThat(response.publicId()).isEqualTo(connector.getPublicId());
    assertThat(response.projectPublicId()).isEqualTo(project.getPublicId());
    assertThat(response.name()).isEqualTo("Mock Health Chatbot");
    assertThat(response.method()).isEqualTo(HttpMethodType.POST);
    assertThat(response.url()).isEqualTo("http://localhost:8080/mock-chatbot/chat");
    assertThat(response.responseSelector()).isEqualTo("$.answer");
    assertThat(response.isStreaming()).isFalse();
    assertThat(response.active()).isTrue();
    assertThat(response.createdAt()).isEqualTo(connector.getCreatedAt());
  }
}
