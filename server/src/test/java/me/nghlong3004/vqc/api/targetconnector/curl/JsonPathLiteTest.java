package me.nghlong3004.vqc.api.targetconnector.curl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonPathLiteTest {

  @Test
  void extractsNestedArrayScalar() {
    Map<String, Object> response =
        Map.of(
            "candidates",
            List.of(Map.of("content", Map.of("parts", List.of(Map.of("text", "Hello QC"))))));

    assertThat(JsonPathLite.extractString(response, "$.candidates[0].content.parts[0].text"))
        .isEqualTo("Hello QC");
  }

  @Test
  void returnsNullForMissingPath() {
    assertThat(JsonPathLite.extractString(Map.of("answer", "ok"), "$.data.answer")).isNull();
  }

  @Test
  void convertsSelectorToSafeJavascriptExpression() {
    assertThat(JsonPathLite.toJavascriptExpression("$.candidates[0].content.parts[0].text"))
        .isEqualTo("json?.candidates?.[0]?.content?.parts?.[0]?.text");
  }

  @Test
  void rejectsUnsafeSelector() {
    assertThatThrownBy(() -> JsonPathLite.toJavascriptExpression("$.answer;process.exit()"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
