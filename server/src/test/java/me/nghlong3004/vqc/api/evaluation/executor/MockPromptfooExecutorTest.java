package me.nghlong3004.vqc.api.evaluation.executor;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.rubric.entity.RubricVersion;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
class MockPromptfooExecutorTest {

  @Test
  void evaluateReturnsOneResultPerTestCase() {
    MockPromptfooExecutor executor = new MockPromptfooExecutor();
    List<TestCase> testCases =
        List.of(testCase(1L, "Question 1", "Answer 1"), testCase(2L, "Question 2", null));

    List<PromptfooResult> results =
        executor.evaluate(testCases, RubricVersion.builder().id(1L).build(), TargetApiConnector.builder().id(1L).build());

    assertThat(results).hasSize(2);
    assertThat(results).extracting(PromptfooResult::testCaseId).containsExactly(1L, 2L);
    assertThat(results)
        .allSatisfy(
            result -> {
              assertThat(result.judgeStatus()).isNotNull();
              assertThat(result.judgeScore()).isBetween(BigDecimal.ZERO, BigDecimal.ONE);
              assertThat(result.latencyMs()).isBetween(50, 500);
              assertThat(result.actualAnswer()).isNotBlank();
              assertThat(result.rawPromptfooResultJson()).contains("mock");
            });
  }

  private TestCase testCase(Long id, String question, String groundTruth) {
    return TestCase.builder()
        .id(id)
        .dataset(Dataset.builder().id(1L).build())
        .question(question)
        .groundTruth(groundTruth)
        .build();
  }
}
