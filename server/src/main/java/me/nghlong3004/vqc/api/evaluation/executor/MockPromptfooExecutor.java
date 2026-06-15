package me.nghlong3004.vqc.api.evaluation.executor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Component
@ConditionalOnProperty(name = "vqc.promptfoo.mode", havingValue = "mock", matchIfMissing = true)
public class MockPromptfooExecutor implements PromptfooExecutor {

  @Override
  public List<PromptfooResult> evaluate(EvaluationRun run, List<TestCase> testCases) {
    return testCases.stream().map(this::evaluateOne).toList();
  }

  private PromptfooResult evaluateOne(TestCase testCase) {
    JudgeStatus status = randomStatus();
    BigDecimal score = randomScoreFor(status);
    Integer latencyMs = ThreadLocalRandom.current().nextInt(50, 501);
    String actualAnswer =
        testCase.getGroundTruth() == null || testCase.getGroundTruth().isBlank()
            ? "Mock answer for: " + testCase.getQuestion()
            : testCase.getGroundTruth();
    return new PromptfooResult(
        testCase.getId(),
        actualAnswer,
        score,
        status,
        "Mock promptfoo evaluation generated for development mode.",
        latencyMs,
        status == JudgeStatus.ERROR ? "Mock evaluation error." : null,
        "{\"mode\":\"mock\"}",
        null);
  }

  private JudgeStatus randomStatus() {
    int value = ThreadLocalRandom.current().nextInt(100);
    if (value < 65) {
      return JudgeStatus.PASS;
    }
    if (value < 85) {
      return JudgeStatus.FAIL;
    }
    if (value < 95) {
      return JudgeStatus.WARNING;
    }
    return JudgeStatus.ERROR;
  }

  private BigDecimal randomScoreFor(JudgeStatus status) {
    double min =
        switch (status) {
          case PASS -> 0.80;
          case WARNING -> 0.50;
          case FAIL, ERROR -> 0.00;
        };
    double max =
        switch (status) {
          case PASS -> 1.00;
          case WARNING -> 0.79;
          case FAIL, ERROR -> 0.49;
        };
    return BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(min, max + 0.0001))
        .setScale(4, RoundingMode.HALF_UP);
  }
}
