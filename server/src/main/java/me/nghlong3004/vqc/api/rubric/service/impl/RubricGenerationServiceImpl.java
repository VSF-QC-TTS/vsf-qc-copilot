package me.nghlong3004.vqc.api.rubric.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.rubric.request.CreateRubricCriterionRequest;
import me.nghlong3004.vqc.api.rubric.request.GenerateRubricPreviewRequest;
import me.nghlong3004.vqc.api.rubric.response.GenerateRubricPreviewResponse;
import me.nghlong3004.vqc.api.rubric.service.RubricGenerationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RubricGenerationServiceImpl implements RubricGenerationService {

  private final ChatClient.Builder chatClientBuilder;
  private final ObjectMapper objectMapper;

  @Override
  public GenerateRubricPreviewResponse generatePreview(
      GenerateRubricPreviewRequest request, String username) {
    String aiResponse = callAi(request);
    GeneratedRubric generated = parseResponse(aiResponse);
    List<CreateRubricCriterionRequest> criteria = toCriterionRequests(generated);
    if (isBlank(generated.content()) || criteria.isEmpty()) {
      throw new ResourceException(ErrorCode.RUBRIC_GENERATION_FAILED);
    }
    log.info("Generated rubric preview for user {}", username);
    return new GenerateRubricPreviewResponse(
        defaultString(generated.name(), request.name()),
        generated.description(),
        generated.content().trim(),
        defaultOutputSchema(generated.outputSchemaJson()),
        criteria);
  }

  String callAi(GenerateRubricPreviewRequest request) {
    String systemPrompt =
        """
        You are a senior QC lead designing chatbot evaluation rubrics.
        Return ONLY one valid JSON object. Do not use markdown.
        The object must contain: name, description, content, outputSchemaJson, criteria.
        criteria must contain 3 to 5 objects with: name, description, weight, passCondition,
        failCondition, judgeInstruction, metricKey, isCritical, sortOrder.
        metricKey must be lowercase snake_case and start with a letter.
        Weights must be integers from 1 to 100.
        Rubric content must be directly usable as shared LLM judge context for Promptfoo llm-rubric assertions.
        Each criterion must be standalone and must instruct an LLM judge to compare the chatbot answer
        semantically against the user question, expected answer, preconditions, and domain context.
        Do not require exact string matching unless the evaluation goal explicitly asks for exact wording.
        """
            .stripIndent();
    String userPrompt =
        """
        Rubric name:
        %s

        Evaluation goal:
        %s

        Domain context:
        %s

        Preferred language:
        %s

        Sample question:
        %s

        Sample expected answer:
        %s

        Extra instructions:
        %s
        """
            .formatted(
                request.name(),
                request.evaluationGoal(),
                blankToFallback(request.domainContext(), "(none)"),
                blankToFallback(request.language(), "same language as user context"),
                blankToFallback(request.sampleQuestion(), "(none)"),
                blankToFallback(request.sampleExpectedAnswer(), "(none)"),
                blankToFallback(request.extraInstructions(), "(none)"));
    ChatClient chatClient = chatClientBuilder.build();
    return chatClient.prompt().system(systemPrompt).user(userPrompt).call().content();
  }

  GeneratedRubric parseResponse(String aiResponse) {
    if (isBlank(aiResponse)) {
      throw new ResourceException(ErrorCode.RUBRIC_GENERATION_FAILED);
    }
    String cleaned = stripMarkdownFence(aiResponse);
    try {
      return objectMapper.readValue(cleaned, GeneratedRubric.class);
    } catch (JsonProcessingException ex) {
      log.warn("Failed to parse rubric generation response: {}", cleaned, ex);
      throw new ResourceException(ErrorCode.RUBRIC_GENERATION_FAILED);
    }
  }

  static List<CreateRubricCriterionRequest> toCriterionRequests(GeneratedRubric generated) {
    if (generated.criteria() == null) {
      return List.of();
    }
    Set<String> usedMetricKeys = new HashSet<>();
    List<GeneratedCriterion> criteria = generated.criteria();
    return java.util.stream.IntStream.range(0, criteria.size())
        .mapToObj(index -> toCriterionRequest(criteria.get(index), index + 1, usedMetricKeys))
        .toList();
  }

  private static CreateRubricCriterionRequest toCriterionRequest(
      GeneratedCriterion criterion, int position, Set<String> usedMetricKeys) {
    String metricKey = uniqueMetricKey(sanitizeMetricKey(criterion.metricKey(), position), usedMetricKeys);
    return new CreateRubricCriterionRequest(
        criterion.name(),
        criterion.description(),
        criterion.weight() == null ? 1 : Math.max(1, Math.min(100, criterion.weight())),
        criterion.passCondition(),
        criterion.failCondition(),
        criterion.judgeInstruction(),
        metricKey,
        criterion.isCritical() != null && criterion.isCritical(),
        criterion.sortOrder() == null ? 0 : criterion.sortOrder());
  }

  private static String sanitizeMetricKey(String metricKey, int position) {
    if (isBlank(metricKey)) {
      return "criterion_" + position;
    }
    String normalized =
        Normalizer.normalize(metricKey.trim().toLowerCase(), Normalizer.Form.NFD)
            .replace("đ", "d")
            .replace("Đ", "d")
            .replaceAll("\\p{M}", "")
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "");
    if (normalized.isBlank()) {
      return "criterion_" + position;
    }
    if (!Character.isLetter(normalized.charAt(0))) {
      return "metric_" + normalized;
    }
    return normalized;
  }

  private static String uniqueMetricKey(String metricKey, Set<String> usedMetricKeys) {
    String candidate = metricKey;
    int suffix = 2;
    while (!usedMetricKeys.add(candidate)) {
      candidate = metricKey + "_" + suffix;
      suffix++;
    }
    return candidate;
  }

  private static String stripMarkdownFence(String value) {
    String cleaned = value.trim();
    if (cleaned.startsWith("```json")) {
      cleaned = cleaned.substring(7);
    } else if (cleaned.startsWith("```")) {
      cleaned = cleaned.substring(3);
    }
    if (cleaned.endsWith("```")) {
      cleaned = cleaned.substring(0, cleaned.length() - 3);
    }
    return cleaned.trim();
  }

  private static String defaultOutputSchema(com.fasterxml.jackson.databind.JsonNode outputSchemaJson) {
    if (outputSchemaJson != null && !outputSchemaJson.isNull() && !outputSchemaJson.isMissingNode()) {
      if (outputSchemaJson.isTextual()) {
        String text = outputSchemaJson.asText().trim();
        if (!isBlank(text)) return text;
      } else if (outputSchemaJson.isObject() && !outputSchemaJson.isEmpty()) {
        return outputSchemaJson.toString();
      }
    }
    return """
        {"type":"object","properties":{"score":{"type":"number"},"status":{"type":"string","enum":["PASS","FAIL","WARNING"]},"reason":{"type":"string"}},"required":["score","status","reason"]}
        """;
  }

  private static String defaultString(String value, String fallback) {
    return isBlank(value) ? fallback.trim() : value.trim();
  }

  private static String blankToFallback(String value, String fallback) {
    return isBlank(value) ? fallback : value.trim();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  record GeneratedRubric(
      String name,
      String description,
      String content,
      com.fasterxml.jackson.databind.JsonNode outputSchemaJson,
      List<GeneratedCriterion> criteria) {}

  record GeneratedCriterion(
      String name,
      String description,
      Integer weight,
      String passCondition,
      String failCondition,
      String judgeInstruction,
      String metricKey,
      Boolean isCritical,
      Integer sortOrder) {}
}
