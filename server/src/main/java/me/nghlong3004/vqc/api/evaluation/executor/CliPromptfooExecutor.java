package me.nghlong3004.vqc.api.evaluation.executor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.evaluation.promptfoo.PromptfooCommandExecutor;
import me.nghlong3004.vqc.api.evaluation.promptfoo.PromptfooConfigGenerator;
import me.nghlong3004.vqc.api.evaluation.promptfoo.PromptfooResultParser;
import me.nghlong3004.vqc.api.evaluation.promptfoo.PromptfooRunDirectoryResolver;
import me.nghlong3004.vqc.api.targetconnector.service.ConnectorSecretService;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Component
@ConditionalOnProperty(name = "vqc.promptfoo.mode", havingValue = "cli")
@RequiredArgsConstructor
@Slf4j
public class CliPromptfooExecutor implements PromptfooExecutor {

  private final PromptfooRunDirectoryResolver runDirectoryResolver;
  private final PromptfooConfigGenerator configGenerator;
  private final PromptfooCommandExecutor commandExecutor;
  private final PromptfooResultParser resultParser;
  private final ConnectorSecretService connectorSecretService;

  @Override
  public List<PromptfooResult> evaluate(EvaluationRun run, List<TestCase> testCases) {
    var runDir = runDirectoryResolver.resolve(run);
    configGenerator.generate(run, testCases, runDir);
    commandExecutor.validate(runDir);
    Map<String, String> secretEnvVars = buildSecretEnvVars(run);
    commandExecutor.eval(runDir, secretEnvVars);
    log.info("Completed promptfoo CLI run {} at {}", run.getPublicId(), runDir);
    return resultParser.parse(runDir.resolve("results.json"));
  }

  private Map<String, String> buildSecretEnvVars(EvaluationRun run) {
    Map<String, String> decrypted =
        connectorSecretService.decryptSecrets(run.getTargetApiConnector());
    if (decrypted.isEmpty()) {
      return Map.of();
    }
    Map<String, String> envVars = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : decrypted.entrySet()) {
      envVars.put("VQC_SECRET_" + entry.getKey(), entry.getValue());
    }
    return envVars;
  }
}

