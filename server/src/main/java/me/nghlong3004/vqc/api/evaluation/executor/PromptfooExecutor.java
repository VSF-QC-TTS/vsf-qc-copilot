package me.nghlong3004.vqc.api.evaluation.executor;

import java.util.List;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public interface PromptfooExecutor {

  /**
   * Evaluates test cases against a target connector and rubric version.
   *
   * @param run evaluation run context
   * @param testCases active test cases to evaluate
   * @return promptfoo-compatible result list
   */
  List<PromptfooResult> evaluate(EvaluationRun run, List<TestCase> testCases);
}
