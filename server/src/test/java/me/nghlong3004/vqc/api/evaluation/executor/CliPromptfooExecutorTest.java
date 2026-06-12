package me.nghlong3004.vqc.api.evaluation.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.nghlong3004.vqc.api.config.PromptfooProperties;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;
import me.nghlong3004.vqc.api.evaluation.promptfoo.PromptfooCommandExecutor;
import me.nghlong3004.vqc.api.evaluation.promptfoo.PromptfooConfigGenerator;
import me.nghlong3004.vqc.api.evaluation.promptfoo.PromptfooExecutionException;
import me.nghlong3004.vqc.api.evaluation.promptfoo.PromptfooResultParser;
import me.nghlong3004.vqc.api.evaluation.promptfoo.PromptfooRunDirectoryResolver;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.rubric.entity.RubricVersion;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.targetconnector.enums.HttpMethodType;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
class CliPromptfooExecutorTest {

  private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

  @Test
  void evaluateValidatesRunsEvalAndParsesResults(@TempDir Path tempDir) throws Exception {
    UUID runPublicId = UUID.randomUUID();
    CliPromptfooExecutor executor = executor(tempDir, fakePromptfoo(tempDir, FakeMode.SUCCESS));

    List<PromptfooResult> results =
        executor.evaluate(run(runPublicId, "$.answer", Map.of("question", "{{question}}")), testCases());

    Path runDir = tempDir.resolve("runs").resolve(runPublicId.toString());
    JsonNode config = objectMapper.readTree(runDir.resolve("promptfooconfig.json").toFile());
    assertThat(config.path("providers").get(0).path("config").path("transformResponse").asText())
        .isEqualTo("json.answer");
    assertThat(objectMapper.readTree(runDir.resolve("tests.json").toFile()).size()).isEqualTo(2);
    assertThat(Files.readString(runDir.resolve("logs").resolve("args.log")))
        .contains("validate config")
        .contains("eval")
        .contains("--no-cache")
        .contains("--max-concurrency 3");
    assertThat(Files.readString(runDir.resolve("logs").resolve("env.log")))
        .contains("FORCE_COLOR=0")
        .contains("PROMPTFOO_CONFIG_DIR=" + runDir.resolve(".promptfoo"))
        .contains("PROMPTFOO_LOG_DIR=" + runDir.resolve("logs"))
        .contains("PROMPTFOO_MAX_EVAL_TIME_MS=120000")
        .contains("PROMPTFOO_EVAL_TIMEOUT_MS=30000");
    assertThat(Files.exists(runDir.resolve("validate.stdout.log"))).isTrue();
    assertThat(Files.exists(runDir.resolve("eval.stdout.log"))).isTrue();
    assertThat(results).hasSize(1);
    assertThat(results.getFirst().testCaseId()).isEqualTo(1L);
    assertThat(results.getFirst().actualAnswer()).isEqualTo("Actual answer");
    assertThat(results.getFirst().judgeScore()).isEqualByComparingTo("0.91");
    assertThat(results.getFirst().judgeStatus()).isEqualTo(JudgeStatus.PASS);
    assertThat(results.getFirst().latencyMs()).isEqualTo(123);
    assertThat(results.getFirst().rawPromptfooResultJson()).contains("Actual answer");
  }

  @Test
  void evaluateMapsDataAnswerSelector(@TempDir Path tempDir) throws Exception {
    UUID runPublicId = UUID.randomUUID();
    CliPromptfooExecutor executor = executor(tempDir, fakePromptfoo(tempDir, FakeMode.SUCCESS));

    executor.evaluate(run(runPublicId, "$.data.answer", Map.of("question", "{{question}}")), testCases());

    Path configPath = tempDir.resolve("runs").resolve(runPublicId.toString()).resolve("promptfooconfig.json");
    JsonNode config = objectMapper.readTree(configPath.toFile());
    assertThat(config.path("providers").get(0).path("config").path("transformResponse").asText())
        .isEqualTo("json.data.answer");
  }

  @Test
  void evaluateFailsFastForUnsupportedSelector(@TempDir Path tempDir) throws Exception {
    CliPromptfooExecutor executor = executor(tempDir, fakePromptfoo(tempDir, FakeMode.SUCCESS));

    assertThatThrownBy(
            () -> executor.evaluate(run(UUID.randomUUID(), "$.items[0].answer", Map.of()), testCases()))
        .isInstanceOf(PromptfooExecutionException.class)
        .hasMessageContaining("Unsupported response selector");
  }

  @Test
  void evaluateFailsFastForSecretPlaceholder(@TempDir Path tempDir) throws Exception {
    CliPromptfooExecutor executor = executor(tempDir, fakePromptfoo(tempDir, FakeMode.SUCCESS));

    assertThatThrownBy(
            () ->
                executor.evaluate(
                    run(UUID.randomUUID(), "$.answer", Map.of("Authorization", "Bearer {{secret:API_TOKEN}}")),
                    testCases()))
        .isInstanceOf(PromptfooExecutionException.class)
        .hasMessageContaining("secret placeholders");
  }

  @Test
  void evaluateFailsWhenValidationFails(@TempDir Path tempDir) throws Exception {
    CliPromptfooExecutor executor = executor(tempDir, fakePromptfoo(tempDir, FakeMode.VALIDATION_FAIL));

    assertThatThrownBy(
            () -> executor.evaluate(run(UUID.randomUUID(), "$.answer", Map.of()), testCases()))
        .isInstanceOf(PromptfooExecutionException.class)
        .hasMessageContaining("exit code 1");
  }

  @Test
  void evaluateParsesExitCode100WhenResultsExist(@TempDir Path tempDir) throws Exception {
    CliPromptfooExecutor executor = executor(tempDir, fakePromptfoo(tempDir, FakeMode.EXIT_100));

    List<PromptfooResult> results =
        executor.evaluate(run(UUID.randomUUID(), "$.answer", Map.of()), testCases());

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().judgeStatus()).isEqualTo(JudgeStatus.PASS);
  }

  @Test
  void evaluateParsesLegacyOutputsShape(@TempDir Path tempDir) throws Exception {
    CliPromptfooExecutor executor = executor(tempDir, fakePromptfoo(tempDir, FakeMode.OUTPUTS_SHAPE));

    List<PromptfooResult> results =
        executor.evaluate(run(UUID.randomUUID(), "$.answer", Map.of()), testCases());

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().testCaseId()).isEqualTo(1L);
    assertThat(results.getFirst().judgeStatus()).isEqualTo(JudgeStatus.PASS);
  }


  @Test
  void evaluateFailsExitCode100WithoutResults(@TempDir Path tempDir) throws Exception {
    CliPromptfooExecutor executor = executor(tempDir, fakePromptfoo(tempDir, FakeMode.EXIT_100_NO_RESULTS));

    assertThatThrownBy(
            () -> executor.evaluate(run(UUID.randomUUID(), "$.answer", Map.of()), testCases()))
        .isInstanceOf(PromptfooExecutionException.class)
        .hasMessageContaining("did not write results.json");
  }

  @Test
  void evaluateFailsMalformedResults(@TempDir Path tempDir) throws Exception {
    CliPromptfooExecutor executor = executor(tempDir, fakePromptfoo(tempDir, FakeMode.MALFORMED_RESULTS));

    assertThatThrownBy(
            () -> executor.evaluate(run(UUID.randomUUID(), "$.answer", Map.of()), testCases()))
        .isInstanceOf(PromptfooExecutionException.class)
        .hasMessageContaining("results array");
  }

  private CliPromptfooExecutor executor(Path tempDir, Path fakePromptfoo) {
    PromptfooProperties properties = new PromptfooProperties();
    properties.setBinaryPath(fakePromptfoo.toString());
    properties.setWorkDir(tempDir.resolve("runs").toString());
    properties.setMaxConcurrency(3);
    properties.setMaxEvalTimeMs(120000);
    properties.setPerTestTimeoutMs(30000);
    return new CliPromptfooExecutor(
        new PromptfooRunDirectoryResolver(properties),
        new PromptfooConfigGenerator(objectMapper),
        new PromptfooCommandExecutor(properties),
        new PromptfooResultParser(objectMapper));
  }

  private EvaluationRun run(UUID publicId, String responseSelector, Map<String, Object> headers) {
    return EvaluationRun.builder()
        .id(1L)
        .publicId(publicId)
        .dataset(Dataset.builder().id(1L).build())
        .rubricVersion(RubricVersion.builder().id(1L).build())
        .targetApiConnector(
            TargetApiConnector.builder()
                .id(1L)
                .publicId(UUID.randomUUID())
                .project(Project.builder().id(1L).build())
                .name("Mock chatbot")
                .method(HttpMethodType.POST)
                .url("https://example.test/chat")
                .headers(headers)
                .bodyTemplate(Map.of("question", "{{question}}"))
                .responseSelector(responseSelector)
                .build())
        .build();
  }

  private List<TestCase> testCases() {
    return List.of(
        TestCase.builder()
            .id(1L)
            .publicId(UUID.randomUUID())
            .dataset(Dataset.builder().id(1L).build())
            .question("Question 1")
            .groundTruth("Answer")
            .build(),
        TestCase.builder()
            .id(2L)
            .publicId(UUID.randomUUID())
            .dataset(Dataset.builder().id(1L).build())
            .question("Question 2")
            .build());
  }

  private Path fakePromptfoo(Path tempDir, FakeMode mode) throws IOException {
    Path fakeBin = tempDir.resolve("promptfoo-" + mode.name().toLowerCase());
    String script =
        """
        #!/usr/bin/env bash
        set -euo pipefail
        mkdir -p "$PROMPTFOO_LOG_DIR"
        printf '%s\\n' "$*" >> "$PROMPTFOO_LOG_DIR/args.log"
        {
          echo "FORCE_COLOR=$FORCE_COLOR"
          echo "PROMPTFOO_CONFIG_DIR=$PROMPTFOO_CONFIG_DIR"
          echo "PROMPTFOO_LOG_DIR=$PROMPTFOO_LOG_DIR"
          echo "PROMPTFOO_MAX_EVAL_TIME_MS=$PROMPTFOO_MAX_EVAL_TIME_MS"
          echo "PROMPTFOO_EVAL_TIMEOUT_MS=$PROMPTFOO_EVAL_TIMEOUT_MS"
        } >> "$PROMPTFOO_LOG_DIR/env.log"
        if [ "$1" = "validate" ]; then
          if [ "__MODE__" = "VALIDATION_FAIL" ]; then
            echo "invalid config" >&2
            exit 1
          fi
          echo "valid config"
          exit 0
        fi
        if [ "$1" = "eval" ]; then
          output=""
          while [ "$#" -gt 0 ]; do
            if [ "$1" = "--output" ]; then
              shift
              output="$1"
            fi
            shift || true
          done
          if [ "__MODE__" = "EXIT_100_NO_RESULTS" ]; then
            exit 100
          fi
          if [ "__MODE__" = "MALFORMED_RESULTS" ]; then
            printf '{"results":{}}' > "$output"
            exit 0
          fi
          if [ "__MODE__" = "OUTPUTS_SHAPE" ]; then
            cat > "$output" <<'JSON'
        {
          "results": {
            "outputs": [
              {
                "vars": {
                  "vqcTestCaseId": 1
                },
                "response": {
                  "output": "Actual answer",
                  "latencyMs": 123
                },
                "gradingResult": {
                  "pass": true,
                  "score": 0.91,
                  "reason": "matched"
                }
              }
            ]
          }
        }
        JSON
            exit 0
          fi
          cat > "$output" <<'JSON'
        {
          "results": {
            "results": [
              {
                "testCase": {
                  "vars": {
                    "vqcTestCaseId": 1
                  }
                },
                "response": {
                  "output": "Actual answer",
                  "latencyMs": 123
                },
                "gradingResult": {
                  "pass": true,
                  "score": 0.91,
                  "reason": "matched"
                },
                "failureReason": 0
              }
            ]
          }
        }
        JSON
          if [ "__MODE__" = "EXIT_100" ]; then
            exit 100
          fi
          exit 0
        fi
        echo "unknown command" >&2
        exit 1
        """
            .stripLeading()
            .replace("__MODE__", mode.name());
    Files.writeString(fakeBin, script);
    assertThat(fakeBin.toFile().setExecutable(true)).isTrue();
    return fakeBin;
  }

  private enum FakeMode {
    SUCCESS,
    VALIDATION_FAIL,
    EXIT_100,
    EXIT_100_NO_RESULTS,
    MALFORMED_RESULTS,
    OUTPUTS_SHAPE
  }
}
