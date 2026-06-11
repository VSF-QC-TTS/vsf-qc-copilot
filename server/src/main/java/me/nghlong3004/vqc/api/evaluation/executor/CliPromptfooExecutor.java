package me.nghlong3004.vqc.api.evaluation.executor;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.config.PromptfooProperties;
import me.nghlong3004.vqc.api.rubric.entity.RubricVersion;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
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

  private final PromptfooProperties promptfooProperties;

  @Override
  public List<PromptfooResult> evaluate(
      List<TestCase> testCases, RubricVersion rubricVersion, TargetApiConnector connector) {
    log.warn(
        "Promptfoo CLI mode is not implemented yet. command={} workDir={}",
        promptfooProperties.getCommand(),
        promptfooProperties.getWorkDir());
    return List.of();
  }
}
