package me.nghlong3004.vqc.api.targetconnector.mapper;

import java.util.List;
import java.util.Map;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.targetconnector.response.SecretRefResponse;
import me.nghlong3004.vqc.api.targetconnector.response.TargetApiConnectorResponse;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Component
public class TargetApiConnectorMapper {

  /**
   * Maps an internal {@link TargetApiConnector} to a public response.
   *
   * @param connector internal connector entity
   * @return public connector response with masked secrets only
   */
  public TargetApiConnectorResponse toResponse(TargetApiConnector connector) {
    return new TargetApiConnectorResponse(
        connector.getPublicId(),
        connector.getProject().getPublicId(),
        connector.getName(),
        connector.getDescription(),
        connector.getMethod(),
        connector.getBaseUrl(),
        connector.getPath(),
        connector.getUrl(),
        connector.getHeaders(),
        connector.getQueryParams(),
        connector.getPathParams(),
        connector.getBodyType(),
        connector.getBodyTemplate(),
        connector.getBodyTemplateText(),
        connector.getAuthType(),
        connector.getAuthConfig(),
        secretRefs(connector.getSecretRefs()),
        connector.getResponseFormat(),
        connector.getResponseSelector(),
        connector.getStreaming(),
        connector.getStreamingType(),
        connector.getStreamingEventSelector(),
        connector.getTimeoutSeconds(),
        connector.getRetryCount(),
        connector.getActive(),
        connector.getCreatedAt(),
        connector.getUpdatedAt());
  }

  private List<SecretRefResponse> secretRefs(List<Map<String, Object>> secretRefs) {
    if (secretRefs == null) {
      return List.of();
    }
    return secretRefs.stream()
        .map(ref -> new SecretRefResponse(stringValue(ref.get("secretKey")), stringValue(ref.get("maskedValue"))))
        .toList();
  }

  private String stringValue(Object value) {
    return value == null ? null : value.toString();
  }
}
