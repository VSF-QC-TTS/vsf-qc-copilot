package me.nghlong3004.vqc.api.targetconnector.curl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/14/2026
 */
class ConnectorSecretDetectorTest {

  private final ConnectorSecretDetector detector = new ConnectorSecretDetector();

  @Test
  void detectsBearerToken() {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("Authorization", "Bearer sk-abc123");
    headers.put("Content-Type", "application/json");

    SecretDetectionResult result = detector.detect(headers);

    assertThat(result.sanitizedHeaders())
        .containsEntry("Authorization", "Bearer {{secret:AUTH_TOKEN}}")
        .containsEntry("Content-Type", "application/json");
    assertThat(result.secretValues()).containsEntry("AUTH_TOKEN", "sk-abc123");
  }

  @Test
  void detectsBasicAuth() {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("Authorization", "Basic dXNlcjpwYXNz");

    SecretDetectionResult result = detector.detect(headers);

    assertThat(result.sanitizedHeaders())
        .containsEntry("Authorization", "Basic {{secret:AUTH_BASIC}}");
    assertThat(result.secretValues()).containsEntry("AUTH_BASIC", "dXNlcjpwYXNz");
  }

  @Test
  void detectsApiKeyHeader() {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("X-Api-Key", "my-secret-key-value");

    SecretDetectionResult result = detector.detect(headers);

    assertThat(result.sanitizedHeaders())
        .containsEntry("X-Api-Key", "{{secret:X_API_KEY}}");
    assertThat(result.secretValues()).containsEntry("X_API_KEY", "my-secret-key-value");
  }

  @Test
  void skipsNonSensitiveHeaders() {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("Accept", "application/json");
    headers.put("user-id", "haint119-sit-2");
    headers.put("user-name", "Hai Nguyen");

    SecretDetectionResult result = detector.detect(headers);

    assertThat(result.sanitizedHeaders())
        .containsEntry("Content-Type", "application/json")
        .containsEntry("Accept", "application/json")
        .containsEntry("user-id", "haint119-sit-2")
        .containsEntry("user-name", "Hai Nguyen");
    assertThat(result.secretValues()).isEmpty();
  }

  @Test
  void handlesEmptyHeaders() {
    SecretDetectionResult result = detector.detect(Map.of());
    assertThat(result.sanitizedHeaders()).isEmpty();
    assertThat(result.secretValues()).isEmpty();
  }

  @Test
  void handlesNullHeaders() {
    SecretDetectionResult result = detector.detect(null);
    assertThat(result.sanitizedHeaders()).isEmpty();
    assertThat(result.secretValues()).isEmpty();
  }

  @Test
  void detectsCustomAuthorizationValue() {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("Authorization", "Token custom-value");

    SecretDetectionResult result = detector.detect(headers);

    assertThat(result.sanitizedHeaders())
        .containsEntry("Authorization", "{{secret:AUTH_VALUE}}");
    assertThat(result.secretValues()).containsEntry("AUTH_VALUE", "Token custom-value");
  }
}
