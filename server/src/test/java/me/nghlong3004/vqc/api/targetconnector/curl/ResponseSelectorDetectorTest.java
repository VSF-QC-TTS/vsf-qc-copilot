package me.nghlong3004.vqc.api.targetconnector.curl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/14/2026
 */
class ResponseSelectorDetectorTest {

  private final ResponseSelectorDetector detector = new ResponseSelectorDetector();

  @Test
  void detectsAnswerKey() {
    Map<String, Object> response = Map.of("answer", "The answer is 42.", "status", "ok");
    assertThat(detector.detect(response)).isEqualTo("$.answer");
  }

  @Test
  void detectsContentKey() {
    Map<String, Object> response = Map.of("content", "Hello world", "id", 123);
    assertThat(detector.detect(response)).isEqualTo("$.content");
  }

  @Test
  void detectsMessageKey() {
    Map<String, Object> response = Map.of("message", "Success response", "code", 200);
    assertThat(detector.detect(response)).isEqualTo("$.message");
  }

  @Test
  void detectsNestedDataAnswer() {
    Map<String, Object> response = Map.of("data", Map.of("answer", "nested answer"), "status", 200);
    assertThat(detector.detect(response)).isEqualTo("$.data.answer");
  }

  @Test
  void fallsBackToSingleStringKey() {
    Map<String, Object> response = Map.of("customField", "some value", "count", 5);
    assertThat(detector.detect(response)).isEqualTo("$.customField");
  }

  @Test
  void fallsBackToDollarWhenAmbiguous() {
    Map<String, Object> response = Map.of("field1", "val1", "field2", "val2");
    assertThat(detector.detect(response)).isEqualTo("$");
  }

  @Test
  void fallsBackToDollarForEmptyResponse() {
    assertThat(detector.detect(Map.of())).isEqualTo("$");
  }

  @Test
  void fallsBackToDollarForNullResponse() {
    assertThat(detector.detect(null)).isEqualTo("$");
  }
}
