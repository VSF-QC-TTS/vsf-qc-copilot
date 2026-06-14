package me.nghlong3004.vqc.api.targetconnector.curl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Detects sensitive values in connector headers (e.g., Bearer tokens, API keys) and replaces them
 * with {@code {{secret:KEY}}} placeholders.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/14/2026
 */
@Component
public class ConnectorSecretDetector {

  private static final Set<String> SKIP_HEADERS =
      Set.of(
          "content-type",
          "accept",
          "user-agent",
          "content-length",
          "cache-control",
          "connection",
          "host",
          "origin",
          "referer",
          "user-id",
          "user-name");

  private static final Set<String> SENSITIVE_HEADER_PATTERNS =
      Set.of("api-key", "apikey", "x-api-key", "api_key", "token", "secret", "password", "auth");

  /**
   * Scans headers for sensitive values and creates secret placeholders.
   *
   * @param headers raw headers from parsed cURL
   * @return detection result with sanitized headers and extracted secret values
   */
  public SecretDetectionResult detect(Map<String, String> headers) {
    if (headers == null || headers.isEmpty()) {
      return new SecretDetectionResult(Map.of(), Map.of());
    }

    Map<String, String> sanitized = new LinkedHashMap<>();
    Map<String, String> secretValues = new LinkedHashMap<>();

    headers.forEach(
        (name, value) -> {
          if (isSkipped(name)) {
            sanitized.put(name, value);
            return;
          }

          String lowerName = name.toLowerCase();

          if (lowerName.equals("authorization")) {
            handleAuthorization(name, value, sanitized, secretValues);
          } else if (isSensitiveHeader(lowerName)) {
            String secretKey = toSecretKey(name);
            secretValues.put(secretKey, value);
            sanitized.put(name, "{{secret:" + secretKey + "}}");
          } else {
            sanitized.put(name, value);
          }
        });

    return new SecretDetectionResult(sanitized, secretValues);
  }

  private void handleAuthorization(
      String name,
      String value,
      Map<String, String> sanitized,
      Map<String, String> secretValues) {
    if (value.toLowerCase().startsWith("bearer ")) {
      String token = value.substring(7).trim();
      secretValues.put("AUTH_TOKEN", token);
      sanitized.put(name, "Bearer {{secret:AUTH_TOKEN}}");
    } else if (value.toLowerCase().startsWith("basic ")) {
      String encoded = value.substring(6).trim();
      secretValues.put("AUTH_BASIC", encoded);
      sanitized.put(name, "Basic {{secret:AUTH_BASIC}}");
    } else {
      secretValues.put("AUTH_VALUE", value);
      sanitized.put(name, "{{secret:AUTH_VALUE}}");
    }
  }

  private boolean isSkipped(String headerName) {
    return SKIP_HEADERS.contains(headerName.toLowerCase());
  }

  private boolean isSensitiveHeader(String lowerName) {
    for (String pattern : SENSITIVE_HEADER_PATTERNS) {
      if (lowerName.contains(pattern)) {
        return true;
      }
    }
    return false;
  }

  private String toSecretKey(String headerName) {
    return headerName.toUpperCase().replaceAll("[^A-Z0-9]", "_");
  }
}
