package me.nghlong3004.vqc.api.evaluation.promptfoo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.rubric.entity.RubricCriterion;
import me.nghlong3004.vqc.api.targetconnector.curl.JsonPathLite;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
@Component
@RequiredArgsConstructor
public class PromptfooConfigGenerator {

  private static final String SECRET_PLACEHOLDER_PREFIX = "{{secret:";

  private final ObjectMapper objectMapper;
  private final RubricAssertionMapper rubricAssertionMapper;

  public void generate(EvaluationRun run, List<TestCase> testCases, Path runDir) {
    generate(run, testCases, List.of(), null, runDir);
  }

  public void generate(
      EvaluationRun run,
      List<TestCase> testCases,
      List<RubricCriterion> criteria,
      String gradingProvider,
      Path runDir) {
    try {
      Files.createDirectories(runDir);
      List<Map<String, Object>> rubricAssertions =
          new ArrayList<>(rubricAssertionMapper.toAssertions(run.getRubricVersion(), criteria));
      if (rubricAssertions.isEmpty()
          && run.getRubricVersion() != null
          && run.getRubricVersion().getContent() != null
          && !run.getRubricVersion().getContent().isBlank()) {
        Map<String, Object> assertion = new LinkedHashMap<>();
        assertion.put("type", "llm-rubric");
        assertion.put("value", run.getRubricVersion().getContent().trim());
        assertion.put("metric", "overall");
        rubricAssertions.add(assertion);
      }
      objectMapper.writeValue(
          runDir.resolve("tests.json").toFile(), tests(testCases, rubricAssertions));
      objectMapper.writeValue(
          runDir.resolve("promptfooconfig.json").toFile(),
          config(run, rubricAssertions, gradingProvider));
    } catch (IOException ex) {
      throw new PromptfooExecutionException("Failed to write promptfoo run files.", ex);
    }
  }

  Map<String, Object> config(
      EvaluationRun run,
      List<Map<String, Object>> rubricAssertions,
      String gradingProvider) {
    TargetApiConnector connector = run.getTargetApiConnector();
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("description", "VQC evaluation run " + run.getPublicId());
    config.put("prompts", List.of("{{question}}"));
    config.put("providers", List.of(provider(connector)));
    config.put("tests", "file://tests.json");
    if (connector.getTimeoutSeconds() != null && connector.getTimeoutSeconds() > 0) {
      config.put(
          "evaluateOptions",
          Map.of("timeoutMs", connector.getTimeoutSeconds() * 1_000L));
    }
    if (!rubricAssertions.isEmpty() && gradingProvider != null && !gradingProvider.isBlank()) {
      config.put(
          "defaultTest", Map.of("options", Map.of("provider", gradingProvider)));
    }
    return config;
  }

  private Map<String, Object> provider(TargetApiConnector connector) {
    Map<String, Object> provider = new LinkedHashMap<>();
    provider.put("id", providerId(connector));
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("url", resolveSecretsToEnvRefs(connector.getUrl()));
    config.put("method", connector.getMethod().name());
    putIfNotEmpty(config, "headers", connector.getHeaders());
    putIfNotEmpty(config, "queryParams", connector.getQueryParams());
    Object body = connector.getBodyTemplate();
    if (body == null && connector.getBodyTemplateText() != null && !connector.getBodyTemplateText().isBlank()) {
      body = connector.getBodyTemplateText();
    }
    if (body != null) {
      config.put("body", resolveSecretsToEnvRefs(body));
    }
    config.put("transformResponse", transformResponse(connector.getResponseSelector()));
    if (connector.getRetryCount() != null && connector.getRetryCount() > 0) {
      config.put("maxRetries", connector.getRetryCount());
    }
    provider.put("config", config);
    return provider;
  }

  private void putIfNotEmpty(Map<String, Object> target, String key, Map<String, Object> value) {
    if (value != null && !value.isEmpty()) {
      target.put(key, resolveSecretsToEnvRefs(value));
    }
  }

  private String providerId(TargetApiConnector connector) {
    String url = connector.getUrl() == null ? "" : connector.getUrl().trim().toLowerCase();
    return url.startsWith("https://") ? "https" : "http";
  }

  String transformResponse(String responseSelector) {
    if (responseSelector == null || responseSelector.isBlank()) {
      return null;
    }
    try {
      return JsonPathLite.toJavascriptExpression(responseSelector);
    } catch (IllegalArgumentException ex) {
      throw new PromptfooExecutionException("Unsupported response selector: " + responseSelector, ex);
    }
  }

  List<Map<String, Object>> tests(
      List<TestCase> testCases, List<Map<String, Object>> rubricAssertions) {
    return testCases.stream().map(tc -> test(tc, rubricAssertions)).toList();
  }

  private Map<String, Object> test(
      TestCase testCase, List<Map<String, Object>> rubricAssertions) {
    Map<String, Object> test = new LinkedHashMap<>();
    Map<String, Object> vars = new LinkedHashMap<>();
    vars.put("vqcTestCaseId", testCase.getId());
    vars.put("vqcTestCasePublicId", String.valueOf(testCase.getPublicId()));
    vars.put("question", testCase.getQuestion());
    vars.put("groundTruth", testCase.getGroundTruth() == null ? "" : testCase.getGroundTruth());
    vars.put("precondition", testCase.getPrecondition() == null ? Map.of() : testCase.getPrecondition());
    vars.put("metadata", testCase.getMetadata() == null ? Map.of() : testCase.getMetadata());
    if (testCase.getExternalId() != null && !testCase.getExternalId().isBlank()) {
      vars.put("externalId", testCase.getExternalId());
    }
    test.put("vars", vars);

    List<Map<String, Object>> assertions = new ArrayList<>();
    if (rubricAssertions.isEmpty()
        && testCase.getGroundTruth() != null
        && !testCase.getGroundTruth().isBlank()) {
      assertions.add(Map.of("type", "contains", "value", testCase.getGroundTruth()));
    }
    assertions.addAll(rubricAssertions);
    if (!assertions.isEmpty()) {
      test.put("assert", assertions);
    }

    return test;
  }

  @SuppressWarnings("unchecked")
  private Object resolveSecretsToEnvRefs(Object value) {
    if (value instanceof String text) {
      return resolveSecretStringToEnvRef(text);
    }
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> safe = new LinkedHashMap<>();
      map.forEach((key, item) -> safe.put(String.valueOf(key), resolveSecretsToEnvRefs(item)));
      return safe;
    }
    if (value instanceof List<?> list) {
      return list.stream().map(this::resolveSecretsToEnvRefs).toList();
    }
    return value;
  }

  private String resolveSecretStringToEnvRef(String text) {
    if (text == null || !text.contains(SECRET_PLACEHOLDER_PREFIX)) {
      return text;
    }
    return text.replaceAll(
        "\\{\\{secret:([^}]+)}}",
        "{{env.VQC_SECRET_$1}}");
  }
}
