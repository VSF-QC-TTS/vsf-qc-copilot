package me.nghlong3004.vqc.api.redteam.promptfoo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.common.crypto.AesGcmEncryptor;
import me.nghlong3004.vqc.api.config.PromptfooProperties;
import me.nghlong3004.vqc.api.evaluation.promptfoo.PromptfooExecutionException;
import me.nghlong3004.vqc.api.judge.entity.JudgeModel;
import me.nghlong3004.vqc.api.judge.enums.JudgeProvider;
import me.nghlong3004.vqc.api.redteam.entity.RedTeamRun;
import me.nghlong3004.vqc.api.targetconnector.curl.JsonPathLite;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.targetconnector.service.ConnectorSecretService;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Component
@RequiredArgsConstructor
public class RedTeamPromptfooExecutor {

  private static final int EXIT_SUCCESS = 0;
  private static final int EXIT_ASSERTION_FAILURE = 100;
  private static final String SECRET_PLACEHOLDER_PREFIX = "{{secret:";

  private final PromptfooProperties promptfooProperties;
  private final ObjectMapper objectMapper;
  private final ConnectorSecretService connectorSecretService;
  private final AesGcmEncryptor aesGcmEncryptor;

  public RedTeamRunResult execute(RedTeamRun run, Path runDir) {
    try {
      Files.createDirectories(runDir.resolve(".promptfoo"));
      Files.createDirectories(runDir.resolve("logs"));
      Path configPath = runDir.resolve("promptfooconfig.json");
      Path generatedPath = runDir.resolve("redteam.yaml");
      Path resultsPath = runDir.resolve("results.json");
      objectMapper.writeValue(configPath.toFile(), config(run));

      Map<String, String> env = environment(run, runDir);
      run(
          List.of(
              promptfooBin(),
              "redteam",
              "generate",
              "--config",
              configPath.toString(),
              "--output",
              generatedPath.toString(),
              "--injectVar",
              "question",
              "--no-progress-bar",
              "--force",
              "--no-cache",
              "--num-tests",
              String.valueOf(run.getNumTests())),
          runDir,
          runDir.resolve("generate.stdout.log"),
          runDir.resolve("generate.stderr.log"),
          env,
          false);
      run(
          List.of(
              promptfooBin(),
              "redteam",
              "eval",
              "--config",
              configPath.toString(),
              "--tests",
              generatedPath.toString(),
              "--output",
              resultsPath.toString(),
              "--no-progress-bar",
              "--no-table",
              "--no-cache",
              "--no-share",
              "--max-concurrency",
              String.valueOf(promptfooProperties.getMaxConcurrency())),
          runDir,
          runDir.resolve("eval.stdout.log"),
          runDir.resolve("eval.stderr.log"),
          env,
          true);
      return summarize(resultsPath);
    } catch (IOException ex) {
      throw new PromptfooExecutionException("Failed to prepare red-team promptfoo run.", ex);
    }
  }

  private Map<String, Object> config(RedTeamRun run) throws JsonProcessingException {
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("description", "VQC red-team run " + run.getPublicId());
    config.put("prompts", List.of("{{question}}"));
    config.put("providers", List.of(provider(run.getTargetApiConnector())));
    Map<String, Object> redteam = new LinkedHashMap<>();
    if (run.getPurpose() != null && !run.getPurpose().isBlank()) {
      redteam.put("purpose", run.getPurpose());
    }
    redteam.put("plugins", objectMapper.readValue(run.getPluginsJson(), List.class));
    redteam.put("strategies", objectMapper.readValue(run.getStrategiesJson(), List.class));
    redteam.put("numTests", run.getNumTests());
    redteam.put("injectVar", "question");
    config.put("redteam", redteam);
    if (run.getJudgeModel() != null) {
      config.put("defaultTest", Map.of("options", Map.of("provider", gradingProvider(run.getJudgeModel()))));
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
    if (connector.getResponseSelector() != null && !connector.getResponseSelector().isBlank()) {
      config.put("transformResponse", JsonPathLite.toJavascriptExpression(connector.getResponseSelector()));
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
    return text.replaceAll("\\{\\{secret:([^}]+)}}", "{{env.VQC_SECRET_$1}}");
  }

  private Map<String, String> environment(RedTeamRun run, Path runDir) {
    Map<String, String> env = new HashMap<>();
    env.put("FORCE_COLOR", "0");
    env.put("PROMPTFOO_DISABLE_TELEMETRY", "1");
    env.put("PROMPTFOO_CONFIG_DIR", runDir.resolve(".promptfoo").toString());
    env.put("PROMPTFOO_LOG_DIR", runDir.resolve("logs").toString());
    env.put("PROMPTFOO_MAX_EVAL_TIME_MS", String.valueOf(promptfooProperties.getMaxEvalTimeMs()));
    env.put("PROMPTFOO_EVAL_TIMEOUT_MS", String.valueOf(promptfooProperties.getPerTestTimeoutMs()));
    env.putAll(connectorSecretService.decryptSecrets(run.getTargetApiConnector()).entrySet().stream()
        .collect(java.util.stream.Collectors.toMap(e -> "VQC_SECRET_" + e.getKey(), Map.Entry::getValue)));
    JudgeModel judgeModel = run.getJudgeModel();
    if (judgeModel != null && judgeModel.getEncryptedApiKey() != null) {
      putJudgeEnvVars(env, judgeModel, aesGcmEncryptor.decrypt(judgeModel.getEncryptedApiKey()));
    } else if (promptfooProperties.getGradingApiKey() != null && !promptfooProperties.getGradingApiKey().isBlank()) {
      env.put("GEMINI_API_KEY", promptfooProperties.getGradingApiKey());
    }
    return env;
  }

  private String gradingProvider(JudgeModel judgeModel) {
    return switch (judgeModel.getProvider()) {
      case GEMINI -> "google:" + judgeModel.getModelName();
      case OPENAI -> "openai:" + judgeModel.getModelName();
      case ANTHROPIC -> "anthropic:" + judgeModel.getModelName();
      case DEEPSEEK, CUSTOM -> "openai:" + judgeModel.getModelName();
    };
  }

  private void putJudgeEnvVars(Map<String, String> envVars, JudgeModel judgeModel, String apiKey) {
    if (apiKey == null || apiKey.isBlank()) {
      return;
    }
    JudgeProvider provider = judgeModel.getProvider();
    switch (provider) {
      case GEMINI -> envVars.put("GEMINI_API_KEY", apiKey);
      case OPENAI -> envVars.put("OPENAI_API_KEY", apiKey);
      case ANTHROPIC -> envVars.put("ANTHROPIC_API_KEY", apiKey);
      case DEEPSEEK, CUSTOM -> {
        envVars.put("OPENAI_API_KEY", apiKey);
        if (judgeModel.getBaseUrl() != null && !judgeModel.getBaseUrl().isBlank()) {
          envVars.put("OPENAI_BASE_URL", judgeModel.getBaseUrl().trim());
        }
      }
    }
  }

  private void run(
      List<String> command,
      Path runDir,
      Path stdoutPath,
      Path stderrPath,
      Map<String, String> env,
      boolean allowAssertionFailure) {
    try {
      ProcessBuilder processBuilder =
          new ProcessBuilder(new ArrayList<>(command))
              .directory(runDir.toFile())
              .redirectOutput(stdoutPath.toFile())
              .redirectError(stderrPath.toFile());
      processBuilder.environment().putAll(env);
      Process process = processBuilder.start();
      boolean finished =
          process.waitFor(
              Duration.ofMillis(promptfooProperties.getMaxEvalTimeMs()).toMillis(),
              TimeUnit.MILLISECONDS);
      if (!finished) {
        process.destroyForcibly();
        throw new PromptfooExecutionException("Promptfoo red-team command timed out.");
      }
      int exitCode = process.exitValue();
      if (exitCode == EXIT_SUCCESS || (allowAssertionFailure && exitCode == EXIT_ASSERTION_FAILURE)) {
        return;
      }
      throw new PromptfooExecutionException(
          "Promptfoo red-team command failed with exit code " + exitCode + ". stderr=" + logSnippet(stderrPath));
    } catch (IOException ex) {
      throw new PromptfooExecutionException("Failed to start promptfoo red-team command.", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new PromptfooExecutionException("Interrupted while waiting for promptfoo red-team command.", ex);
    }
  }

  private RedTeamRunResult summarize(Path resultsPath) throws IOException {
    if (!Files.exists(resultsPath)) {
      return new RedTeamRunResult(0, 0, 0, 0);
    }
    JsonNode root = objectMapper.readTree(resultsPath.toFile());
    JsonNode stats = root.path("results").path("stats");
    int successes = stats.path("successes").asInt(0);
    int failures = stats.path("failures").asInt(0);
    int errors = stats.path("errors").asInt(0);
    int total = Math.max(root.path("results").path("results").size(), successes + failures + errors);
    return new RedTeamRunResult(total, successes, failures, errors);
  }

  private String promptfooBin() {
    String binaryPath = promptfooProperties.getBinaryPath();
    if (binaryPath == null || binaryPath.isBlank()) {
      throw new PromptfooExecutionException("Promptfoo binary path is required.");
    }
    return Path.of(binaryPath).isAbsolute()
        ? Path.of(binaryPath).normalize().toString()
        : Path.of("").toAbsolutePath().normalize().resolve(binaryPath).normalize().toString();
  }

  private String logSnippet(Path path) {
    try {
      if (!Files.exists(path)) {
        return "";
      }
      String content = Files.readString(path).trim();
      return content.length() <= 2000 ? content : content.substring(0, 2000);
    } catch (IOException ex) {
      return "";
    }
  }

  public record RedTeamRunResult(int total, int passed, int failed, int errors) {}
}
