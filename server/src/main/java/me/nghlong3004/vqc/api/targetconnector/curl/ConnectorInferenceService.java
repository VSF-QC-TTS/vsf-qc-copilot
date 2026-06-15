package me.nghlong3004.vqc.api.targetconnector.curl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConnectorInferenceService {

  private static final String QUESTION_PLACEHOLDER = "{{question}}";
  private static final List<String> PROMPT_KEYS =
      List.of("question", "prompt", "message", "text", "content", "input", "query");

  private final ObjectMapper objectMapper;
  private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
  private final ResponseSelectorDetector responseSelectorDetector;

  public ConnectorInferenceService(
      ObjectMapper objectMapper, ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
    this.objectMapper = objectMapper;
    this.chatClientBuilderProvider = chatClientBuilderProvider;
    this.responseSelectorDetector = new ResponseSelectorDetector();
  }

  public static ConnectorInferenceService deterministicOnly() {
    return new ConnectorInferenceService(JsonMapper.builder().findAndAddModules().build(), null);
  }

  public ConnectorInferenceResult infer(
      CurlParseResult parsed, Map<String, Object> rawResponse, String requestedSelector) {
    Map<String, Object> responseSchema = ResponseSchemaBuilder.schema(rawResponse);
    String selector = requestedSelector == null || requestedSelector.isBlank()
        ? responseSelectorDetector.detect(rawResponse)
        : requestedSelector.trim();
    Map<String, Object> bodyTemplate = parsed.isJsonBody() ? inferBodyTemplate(parsed.bodyJson()) : parsed.bodyJson();
    String bodyTemplateText = parsed.isJsonBody() ? null : inferBodyTemplateText(parsed.bodyRaw());

    if (isValid(rawResponse, selector)) {
      return new ConnectorInferenceResult(bodyTemplate, bodyTemplateText, selector, responseSchema);
    }

    ConnectorInferenceResult aiResult = inferWithAi(parsed, responseSchema, selector);
    if (aiResult != null && isValid(rawResponse, aiResult.responseSelector())) {
      return new ConnectorInferenceResult(
          aiResult.bodyTemplate() == null ? bodyTemplate : aiResult.bodyTemplate(),
          aiResult.bodyTemplateText() == null ? bodyTemplateText : aiResult.bodyTemplateText(),
          aiResult.responseSelector(),
          responseSchema);
    }

    return new ConnectorInferenceResult(bodyTemplate, bodyTemplateText, selector, responseSchema);
  }

  private boolean isValid(Map<String, Object> rawResponse, String selector) {
    try {
      String extracted = JsonPathLite.extractString(rawResponse, selector);
      return extracted != null && !extracted.isBlank();
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> inferBodyTemplate(Map<String, Object> bodyJson) {
    if (bodyJson == null || bodyJson.isEmpty()) {
      return bodyJson == null ? Map.of() : bodyJson;
    }
    ReplacementFlag flag = new ReplacementFlag();
    Object inferred = replacePromptValue(bodyJson, flag, null);
    return inferred instanceof Map<?, ?> map ? (Map<String, Object>) map : bodyJson;
  }

  private String inferBodyTemplateText(String bodyRaw) {
    return bodyRaw == null || bodyRaw.isBlank() ? bodyRaw : QUESTION_PLACEHOLDER;
  }

  private Object replacePromptValue(Object value, ReplacementFlag flag, String parentKey) {
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> copy = new LinkedHashMap<>();
      String role = map.get("role") == null ? null : map.get("role").toString();
      map.forEach((key, item) -> copy.put(String.valueOf(key), replacePromptValue(item, flag, String.valueOf(key))));
      if (!flag.replaced() && "user".equalsIgnoreCase(role) && copy.get("content") instanceof String) {
        copy.put("content", QUESTION_PLACEHOLDER);
        flag.markReplaced();
      }
      return copy;
    }
    if (value instanceof List<?> list) {
      return list.stream().map(item -> replacePromptValue(item, flag, parentKey)).toList();
    }
    if (!flag.replaced() && value instanceof String && isPromptKey(parentKey)) {
      flag.markReplaced();
      return QUESTION_PLACEHOLDER;
    }
    return value;
  }

  private boolean isPromptKey(String key) {
    return key != null && PROMPT_KEYS.contains(key.toLowerCase());
  }

  private ConnectorInferenceResult inferWithAi(
      CurlParseResult parsed, Map<String, Object> responseSchema, String attemptedSelector) {
    if (chatClientBuilderProvider == null) {
      return null;
    }
    ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
    if (builder == null) {
      return null;
    }
    try {
      String userPrompt =
          "Infer connector fields from this sanitized API shape. Return JSON only with "
              + "bodyTemplate, bodyTemplateText, responseSelector. Use only JSONPath selectors like $.a[0].b. "
              + "Do not include explanations.\n"
              + "method=" + parsed.method() + "\n"
              + "url=" + parsed.url() + "\n"
              + "isJsonBody=" + parsed.isJsonBody() + "\n"
              + "bodyShape=" + objectMapper.writeValueAsString(ResponseSchemaBuilder.schema(parsed.bodyJson())) + "\n"
              + "responseShape=" + objectMapper.writeValueAsString(responseSchema) + "\n"
              + "attemptedSelector=" + attemptedSelector;
      String content =
          builder.build()
              .prompt()
              .system("You infer API connector templates from sanitized schema shapes. Return strict JSON only.")
              .user(userPrompt)
              .call()
              .content();
      return parseAiResult(content, responseSchema);
    } catch (Exception ex) {
      log.warn("Connector AI inference fallback failed: {}", ex.getMessage());
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private ConnectorInferenceResult parseAiResult(String content, Map<String, Object> responseSchema)
      throws JsonProcessingException {
    if (content == null || content.isBlank()) {
      return null;
    }
    String cleaned = content.trim();
    if (cleaned.startsWith("```")) {
      cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
    }
    Map<String, Object> parsed = objectMapper.readValue(cleaned, Map.class);
    Object bodyTemplate = parsed.get("bodyTemplate");
    return new ConnectorInferenceResult(
        bodyTemplate instanceof Map<?, ?> map ? (Map<String, Object>) map : null,
        parsed.get("bodyTemplateText") == null ? null : parsed.get("bodyTemplateText").toString(),
        parsed.get("responseSelector") == null ? null : parsed.get("responseSelector").toString(),
        responseSchema);
  }

  private static final class ReplacementFlag {
    private boolean replaced;

    boolean replaced() {
      return replaced;
    }

    void markReplaced() {
      replaced = true;
    }
  }
}
