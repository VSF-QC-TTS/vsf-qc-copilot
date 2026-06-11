package me.nghlong3004.vqc.api.evaluation.executor;

import java.util.List;
import me.nghlong3004.vqc.api.rubric.entity.RubricVersion;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public interface PromptfooExecutor {

  /**
   * Evaluates test cases against a target connector and rubric version.
   *
   * @param testCases active test cases to evaluate
   * @param rubricVersion published rubric version
   * @param connector target API connector
   * @return promptfoo-compatible result list
   */
  List<PromptfooResult> evaluate(
      List<TestCase> testCases, RubricVersion rubricVersion, TargetApiConnector connector);
}
