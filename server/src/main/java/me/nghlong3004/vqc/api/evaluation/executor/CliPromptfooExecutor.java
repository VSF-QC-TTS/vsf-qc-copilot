package me.nghlong3004.vqc.api.evaluation.executor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.config.PromptfooProperties;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.evaluation.promptfoo.PromptfooCommandExecutor;
import me.nghlong3004.vqc.api.evaluation.promptfoo.PromptfooConfigGenerator;
import me.nghlong3004.vqc.api.evaluation.promptfoo.PromptfooResultParser;
import me.nghlong3004.vqc.api.evaluation.promptfoo.PromptfooRunDirectoryResolver;
import me.nghlong3004.vqc.api.rubric.entity.RubricCriterion;
import me.nghlong3004.vqc.api.rubric.repository.RubricCriterionRepository;
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
  private final RubricCriterionRepository rubricCriterionRepository;
  private final PromptfooProperties promptfooProperties;

  @Override
  public List<PromptfooResult> evaluate(EvaluationRun run, List<TestCase> testCases) {
    var runDir = runDirectoryResolver.resolve(run);
    List<RubricCriterion> criteria = loadCriteria(run);
    configGenerator.generate(
        run, testCases, criteria, promptfooProperties.getGradingProvider(), runDir);
    commandExecutor.validate(runDir);
    Map<String, String> envVars = buildEnvVars(run);
    commandExecutor.eval(runDir, envVars);
    log.info("Completed promptfoo CLI run {} at {}", run.getPublicId(), runDir);
    return resultParser.parse(runDir.resolve("results.json"));
  }

  private List<RubricCriterion> loadCriteria(EvaluationRun run) {
    if (run.getRubricVersion() == null) {
      return List.of();
    }
    return rubricCriterionRepository.findByRubricVersionOrderBySortOrderAscIdAsc(
        run.getRubricVersion());
  }

  private Map<String, String> buildEnvVars(EvaluationRun run) {
    Map<String, String> envVars = new LinkedHashMap<>();

    // Connector secrets
    Map<String, String> decrypted =
        connectorSecretService.decryptSecrets(run.getTargetApiConnector());
    for (Map.Entry<String, String> entry : decrypted.entrySet()) {
      envVars.put("VQC_SECRET_" + entry.getKey(), entry.getValue());
    }

    // Grading provider API key
    String gradingApiKey = promptfooProperties.getGradingApiKey();
    if (gradingApiKey != null && !gradingApiKey.isBlank()) {
      envVars.put("GEMINI_API_KEY", gradingApiKey);
    }

    return envVars;
  }
}
