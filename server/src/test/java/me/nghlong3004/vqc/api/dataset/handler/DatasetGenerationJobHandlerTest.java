package me.nghlong3004.vqc.api.dataset.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/13/2026
 */
class DatasetGenerationJobHandlerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void parseResponseParsesValidJsonArray() {
    DatasetGenerationJobHandler handler =
        new DatasetGenerationJobHandler(null, null, null, null, null, objectMapper);

    String response =
        """
        [{"question": "What is AI?", "ground_truth": "Artificial Intelligence"}]
        """;

    List<Map<String, String>> result = handler.parseResponse(response);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().get("question")).isEqualTo("What is AI?");
    assertThat(result.getFirst().get("ground_truth")).isEqualTo("Artificial Intelligence");
  }

  @Test
  void parseResponseHandlesMarkdownCodeBlock() {
    DatasetGenerationJobHandler handler =
        new DatasetGenerationJobHandler(null, null, null, null, null, objectMapper);

    String response =
        """
        ```json
        [{"question": "Q1", "ground_truth": "A1"}, {"question": "Q2", "ground_truth": "A2"}]
        ```
        """;

    List<Map<String, String>> result = handler.parseResponse(response);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).get("question")).isEqualTo("Q1");
    assertThat(result.get(1).get("question")).isEqualTo("Q2");
  }

  @Test
  void parseResponseReturnsEmptyListForInvalidJson() {
    DatasetGenerationJobHandler handler =
        new DatasetGenerationJobHandler(null, null, null, null, null, objectMapper);

    List<Map<String, String>> result = handler.parseResponse("not valid json");

    assertThat(result).isEmpty();
  }

  @Test
  void parseResponseReturnsEmptyListForNullInput() {
    DatasetGenerationJobHandler handler =
        new DatasetGenerationJobHandler(null, null, null, null, null, objectMapper);

    assertThat(handler.parseResponse(null)).isEmpty();
    assertThat(handler.parseResponse("  ")).isEmpty();
  }

  @Test
  void parseResponseHandlesPlainCodeBlock() {
    DatasetGenerationJobHandler handler =
        new DatasetGenerationJobHandler(null, null, null, null, null, objectMapper);

    String response =
        """
        ```
        [{"question": "Q1", "ground_truth": "A1"}]
        ```
        """;

    List<Map<String, String>> result = handler.parseResponse(response);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().get("question")).isEqualTo("Q1");
  }
}
