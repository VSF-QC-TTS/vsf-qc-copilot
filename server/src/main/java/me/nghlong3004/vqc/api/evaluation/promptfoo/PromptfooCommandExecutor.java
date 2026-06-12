package me.nghlong3004.vqc.api.evaluation.promptfoo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.config.PromptfooProperties;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
@Component
@RequiredArgsConstructor
public class PromptfooCommandExecutor {

  static final int EXIT_SUCCESS = 0;
  static final int EXIT_ASSERTION_FAILURE = 100;

  private final PromptfooProperties promptfooProperties;

  public void validate(Path runDir) {
    run(
        List.of(
            promptfooBin(),
            "validate",
            "config",
            "--config",
            runDir.resolve("promptfooconfig.json").toString()),
        runDir,
        runDir.resolve("validate.stdout.log"),
        runDir.resolve("validate.stderr.log"),
        false);
  }

  public void eval(Path runDir) {
    ProcessResult result =
        run(
            evalCommand(runDir),
            runDir,
            runDir.resolve("eval.stdout.log"),
            runDir.resolve("eval.stderr.log"),
            true);
    Path resultsPath = runDir.resolve("results.json");
    if ((result.exitCode() == EXIT_SUCCESS || result.exitCode() == EXIT_ASSERTION_FAILURE)
        && Files.exists(resultsPath)) {
      return;
    }
    if (result.exitCode() == EXIT_ASSERTION_FAILURE) {
      throw new PromptfooExecutionException("Promptfoo eval exited 100 but did not write results.json.");
    }
    throw new PromptfooExecutionException(
        "Promptfoo eval failed with exit code " + result.exitCode() + ". stderr=" + logSnippet(runDir.resolve("eval.stderr.log")));
  }

  List<String> evalCommand(Path runDir) {
    return List.of(
        promptfooBin(),
        "eval",
        "--config",
        runDir.resolve("promptfooconfig.json").toString(),
        "--output",
        runDir.resolve("results.json").toString(),
        "--no-progress-bar",
        "--no-table",
        "--no-cache",
        "--max-concurrency",
        String.valueOf(promptfooProperties.getMaxConcurrency()));
  }

  Map<String, String> environment(Path runDir) {
    return Map.of(
        "FORCE_COLOR", "0",
        "PROMPTFOO_CONFIG_DIR", runDir.resolve(".promptfoo").toString(),
        "PROMPTFOO_LOG_DIR", runDir.resolve("logs").toString(),
        "PROMPTFOO_MAX_EVAL_TIME_MS", String.valueOf(promptfooProperties.getMaxEvalTimeMs()),
        "PROMPTFOO_EVAL_TIMEOUT_MS", String.valueOf(promptfooProperties.getPerTestTimeoutMs()));
  }

  private ProcessResult run(
      List<String> command, Path runDir, Path stdoutPath, Path stderrPath, boolean allowAssertionFailure) {
    try {
      Files.createDirectories(runDir.resolve(".promptfoo"));
      Files.createDirectories(runDir.resolve("logs"));
      ProcessBuilder processBuilder =
          new ProcessBuilder(new ArrayList<>(command))
              .directory(runDir.toFile())
              .redirectOutput(stdoutPath.toFile())
              .redirectError(stderrPath.toFile());
      processBuilder.environment().putAll(environment(runDir));
      Process process = processBuilder.start();
      boolean finished =
          process.waitFor(
              Duration.ofMillis(promptfooProperties.getMaxEvalTimeMs()).toMillis(), TimeUnit.MILLISECONDS);
      if (!finished) {
        process.destroyForcibly();
        throw new PromptfooExecutionException("Promptfoo command timed out.");
      }
      int exitCode = process.exitValue();
      if (exitCode == EXIT_SUCCESS || (allowAssertionFailure && exitCode == EXIT_ASSERTION_FAILURE)) {
        return new ProcessResult(exitCode);
      }
      throw new PromptfooExecutionException(
          "Promptfoo command failed with exit code " + exitCode + ". stderr=" + logSnippet(stderrPath));
    } catch (IOException ex) {
      throw new PromptfooExecutionException("Failed to start promptfoo command.", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new PromptfooExecutionException("Interrupted while waiting for promptfoo command.", ex);
    }
  }

  private String promptfooBin() {
    String binaryPath = promptfooProperties.getBinaryPath();
    if (binaryPath == null || binaryPath.isBlank()) {
      throw new PromptfooExecutionException("Promptfoo binary path is required.");
    }
    Path configured = Path.of(binaryPath);
    if (configured.isAbsolute()) {
      return configured.normalize().toString();
    }
    Path workingDir = Path.of("").toAbsolutePath().normalize();
    Path fromWorkingDir = workingDir.resolve(configured).normalize();
    if (Files.exists(fromWorkingDir)) {
      return fromWorkingDir.toString();
    }
    Path parent = workingDir.getParent();
    if (parent != null) {
      Path fromParent = parent.resolve(configured).normalize();
      if (Files.exists(fromParent)) {
        return fromParent.toString();
      }
    }
    return fromWorkingDir.toString();
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

  record ProcessResult(int exitCode) {}
}
