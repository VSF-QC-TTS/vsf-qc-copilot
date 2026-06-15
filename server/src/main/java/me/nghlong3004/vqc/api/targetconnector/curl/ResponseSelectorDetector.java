package me.nghlong3004.vqc.api.targetconnector.curl;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Inspects a raw API response and detects the most likely JSONPath selector for extracting the
 * "answer" field.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/14/2026
 */
@Component
public class ResponseSelectorDetector {

  private static final List<String> COMMON_ANSWER_KEYS =
      List.of(
          "answer", "content", "data", "message", "text", "result", "output", "response", "reply");

  /**
   * Inspects the raw response and returns a JSONPath selector.
   *
   * <p>Tries common keys first. If the response has a single string-valued key, uses that. Falls
   * back to {@code $} (entire response).
   *
   * @param rawResponse the raw JSON response from the target API
   * @return best-guess JSONPath selector string
   */
  public String detect(Map<String, Object> rawResponse) {
    if (rawResponse == null || rawResponse.isEmpty()) {
      return "$";
    }

    for (String knownSelector : List.of(
        "$.candidates[0].content.parts[0].text",
        "$.choices[0].message.content",
        "$.choices[0].text",
        "$.content[0].text")) {
      String extracted = JsonPathLite.extractString(rawResponse, knownSelector);
      if (extracted != null && !extracted.isBlank()) {
        return knownSelector;
      }
    }

    // Try common answer keys first; if the value is a nested map, check inside it
    for (String key : COMMON_ANSWER_KEYS) {
      Object value = rawResponse.get(key);
      if (value == null) {
        continue;
      }
      // If the value is a nested map, look for common answer keys inside it
      if (value instanceof Map<?, ?> nestedMap) {
        for (String nestedKey : COMMON_ANSWER_KEYS) {
          if (nestedMap.containsKey(nestedKey) && nestedMap.get(nestedKey) != null) {
            return "$." + key + "." + nestedKey;
          }
        }
      }
      return "$." + key;
    }

    // If only one string-valued key exists, use it
    String singleStringKey = null;
    int stringKeyCount = 0;
    for (Map.Entry<String, Object> entry : rawResponse.entrySet()) {
      if (entry.getValue() instanceof String) {
        singleStringKey = entry.getKey();
        stringKeyCount++;
      }
    }
    if (stringKeyCount == 1 && singleStringKey != null) {
      return "$." + singleStringKey;
    }

    return "$";
  }
}
