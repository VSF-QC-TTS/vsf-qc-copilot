package me.nghlong3004.vqc.api.evaluation.promptfoo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;
import me.nghlong3004.vqc.api.evaluation.executor.PromptfooResult;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
@Component
@RequiredArgsConstructor
public class PromptfooResultParser {

  private final ObjectMapper objectMapper;

  public List<PromptfooResult> parse(Path resultsPath) {
    try {
      JsonNode root = objectMapper.readTree(resultsPath.toFile());
      JsonNode outputs = resultRows(root);
      if (!outputs.isArray()) {
        throw new PromptfooExecutionException("Promptfoo results array is missing or malformed.");
      }
      List<PromptfooResult> results = new ArrayList<>();
      for (JsonNode output : outputs) {
        results.add(toResult(output));
      }
      return results;
    } catch (IOException ex) {
      throw new PromptfooExecutionException("Failed to parse promptfoo results.", ex);
    }
  }

  private JsonNode resultRows(JsonNode root) {
    JsonNode outputs = root.path("results").path("outputs");
    if (outputs.isArray()) {
      return outputs;
    }
    return root.path("results").path("results");
  }

  private PromptfooResult toResult(JsonNode output) throws IOException {
    JsonNode grading = output.path("gradingResult");
    BigDecimal score = decimalValue(firstPresent(grading.path("score"), output.path("score")));
    String error = errorValue(firstPresent(output.path("error"), output.path("failureReason")));
    return new PromptfooResult(
        testCaseId(output),
        actualAnswer(output),
        score,
        judgeStatus(output, grading, score, error),
        textValue(firstPresent(grading.path("reason"), output.path("reason"))),
        intValue(firstPresent(output.path("latencyMs"), output.path("response").path("latencyMs"))),
        error,
        objectMapper.writeValueAsString(output));
  }

  private Long testCaseId(JsonNode output) {
    JsonNode vars =
        firstPresent(
            output.path("vars"),
            output.path("test").path("vars"),
            output.path("testCase").path("vars"),
            output.path("metadata").path("vars"));
    JsonNode id = firstPresent(vars.path("vqcTestCaseId"), vars.path("testCaseId"));
    if (id.isNumber()) {
      return id.longValue();
    }
    if (id.isTextual()) {
      try {
        return Long.parseLong(id.asText());
      } catch (NumberFormatException ex) {
        return null;
      }
    }
    return null;
  }

  private String actualAnswer(JsonNode output) throws IOException {
    JsonNode actual =
        firstPresent(
            output.path("response").path("output"),
            output.path("output"),
            output.path("result"),
            output.path("response"));
    if (actual.isNull() || actual.isMissingNode()) {
      return null;
    }
    if (actual.isValueNode()) {
      return actual.asText();
    }
    return objectMapper.writeValueAsString(actual);
  }

  private JudgeStatus judgeStatus(
      JsonNode output, JsonNode grading, BigDecimal score, String error) {
    if (error != null && !error.isBlank()) {
      return JudgeStatus.ERROR;
    }
    JsonNode pass = firstPresent(grading.path("pass"), output.path("success"), output.path("pass"));
    if (pass.isBoolean()) {
      return pass.asBoolean() ? JudgeStatus.PASS : JudgeStatus.FAIL;
    }
    if (score == null) {
      return JudgeStatus.WARNING;
    }
    if (score.compareTo(BigDecimal.valueOf(0.80)) >= 0) {
      return JudgeStatus.PASS;
    }
    if (score.compareTo(BigDecimal.valueOf(0.50)) >= 0) {
      return JudgeStatus.WARNING;
    }
    return JudgeStatus.FAIL;
  }

  private JsonNode firstPresent(JsonNode... nodes) {
    for (JsonNode node : nodes) {
      if (node != null && !node.isMissingNode() && !node.isNull()) {
        return node;
      }
    }
    return objectMapper.nullNode();
  }

  private BigDecimal decimalValue(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    if (node.isNumber()) {
      return node.decimalValue();
    }
    if (node.isTextual()) {
      try {
        return new BigDecimal(node.asText());
      } catch (NumberFormatException ex) {
        return null;
      }
    }
    return null;
  }

  private Integer intValue(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    if (node.isNumber()) {
      return node.intValue();
    }
    if (node.isTextual()) {
      try {
        return Integer.parseInt(node.asText());
      } catch (NumberFormatException ex) {
        return null;
      }
    }
    return null;
  }

  private String textValue(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    return node.isTextual() ? node.asText() : node.toString();
  }

  private String errorValue(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    if (node.isNumber() && node.asInt() == 0) {
      return null;
    }
    if (node.isBoolean() && !node.asBoolean()) {
      return null;
    }
    return textValue(node);
  }
}
