package me.nghlong3004.vqc.api.evaluation.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;
import me.nghlong3004.vqc.api.rubric.entity.RubricCriterion;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
class CriteriaScoreCalculatorTest {

  private final CriteriaScoreCalculator calculator =
      new CriteriaScoreCalculator(JsonMapper.builder().build());

  @Test
  void returnsNullScoreAndFallbackWhenNoCriteriaResults() {
    ScoringResult result = calculator.computeScore(null, List.of(), JudgeStatus.PASS);

    assertThat(result.judgeScore()).isNull();
    assertThat(result.judgeStatus()).isEqualTo(JudgeStatus.PASS);
  }

  @Test
  void returnsNullScoreAndFallbackWhenBlankJson() {
    ScoringResult result =
        calculator.computeScore("  ", List.of(criterion("a", 5, false)), JudgeStatus.WARNING);

    assertThat(result.judgeScore()).isNull();
    assertThat(result.judgeStatus()).isEqualTo(JudgeStatus.WARNING);
  }

  @Test
  void returnsNullScoreAndFallbackWhenMalformedJson() {
    ScoringResult result =
        calculator.computeScore(
            "not json", List.of(criterion("a", 5, false)), JudgeStatus.PASS);

    assertThat(result.judgeScore()).isNull();
    assertThat(result.judgeStatus()).isEqualTo(JudgeStatus.PASS);
  }

  @Test
  void computesWeightedScoreForSingleCriterion() {
    String json =
        """
        [{"metricKey":"accuracy","pass":true,"score":0.8,"reason":"Good"}]
        """;
    List<RubricCriterion> criteria = List.of(criterion("accuracy", 10, false));

    ScoringResult result = calculator.computeScore(json, criteria, JudgeStatus.PASS);

    assertThat(result.judgeScore()).isEqualByComparingTo("0.8000");
    assertThat(result.judgeStatus()).isEqualTo(JudgeStatus.PASS);
  }

  @Test
  void computesWeightedScoreForMultipleCriteria() {
    String json =
        """
        [
          {"metricKey":"accuracy","pass":true,"score":1.0,"reason":"Perfect"},
          {"metricKey":"tone","pass":true,"score":0.5,"reason":"OK"}
        ]
        """;
    List<RubricCriterion> criteria =
        List.of(criterion("accuracy", 6, false), criterion("tone", 4, false));

    ScoringResult result = calculator.computeScore(json, criteria, JudgeStatus.PASS);

    // (0.6 * 1.0 + 0.4 * 0.5) / (0.6 + 0.4) = 0.8 / 1.0 = 0.8000
    assertThat(result.judgeScore()).isEqualByComparingTo("0.8000");
    assertThat(result.judgeStatus()).isEqualTo(JudgeStatus.PASS);
  }

  @Test
  void criticalCriterionFailureForcesFailStatus() {
    String json =
        """
        [
          {"metricKey":"accuracy","pass":true,"score":1.0,"reason":"Perfect"},
          {"metricKey":"safety","pass":false,"score":0.0,"reason":"Unsafe content"}
        ]
        """;
    List<RubricCriterion> criteria =
        List.of(
            criterion("accuracy", 7, false),
            criterion("safety", 3, true));

    ScoringResult result = calculator.computeScore(json, criteria, JudgeStatus.PASS);

    // Score still computed: (0.7 * 1.0 + 0.3 * 0.0) / (0.7 + 0.3) = 0.7000
    assertThat(result.judgeScore()).isEqualByComparingTo("0.7000");
    // But status forced to FAIL because critical "safety" failed
    assertThat(result.judgeStatus()).isEqualTo(JudgeStatus.FAIL);
  }

  @Test
  void nonCriticalCriterionFailureDoesNotOverrideStatus() {
    String json =
        """
        [
          {"metricKey":"accuracy","pass":true,"score":1.0,"reason":"Perfect"},
          {"metricKey":"tone","pass":false,"score":0.2,"reason":"Rude"}
        ]
        """;
    List<RubricCriterion> criteria =
        List.of(
            criterion("accuracy", 6, false),
            criterion("tone", 4, false));

    ScoringResult result = calculator.computeScore(json, criteria, JudgeStatus.PASS);

    // (0.6 * 1.0 + 0.4 * 0.2) / (0.6 + 0.4) = 0.68 → 0.6800
    assertThat(result.judgeScore()).isEqualByComparingTo("0.6800");
    // Non-critical failure does not override → keeps fallback PASS
    assertThat(result.judgeStatus()).isEqualTo(JudgeStatus.PASS);
  }

  @Test
  void unknownMetricKeyDefaultsToWeightOne() {
    String json =
        """
        [{"metricKey":"unknown_metric","pass":true,"score":0.9,"reason":"Fine"}]
        """;
    // No matching criterion for "unknown_metric"
    List<RubricCriterion> criteria = List.of(criterion("other", 5, false));

    ScoringResult result = calculator.computeScore(json, criteria, JudgeStatus.PASS);

    // Weight defaults to 1.0 for unknown metric: (1.0 * 0.9) / 1.0 = 0.9000
    assertThat(result.judgeScore()).isEqualByComparingTo("0.9000");
  }

  @Test
  void allCriticalPassKeepsFallbackStatus() {
    String json =
        """
        [
          {"metricKey":"safety","pass":true,"score":1.0,"reason":"Safe"},
          {"metricKey":"compliance","pass":true,"score":0.9,"reason":"Compliant"}
        ]
        """;
    List<RubricCriterion> criteria =
        List.of(
            criterion("safety", 5, true),
            criterion("compliance", 5, true));

    ScoringResult result = calculator.computeScore(json, criteria, JudgeStatus.PASS);

    assertThat(result.judgeScore()).isEqualByComparingTo("0.9500");
    assertThat(result.judgeStatus()).isEqualTo(JudgeStatus.PASS);
  }

  private RubricCriterion criterion(String metricKey, int weight, boolean critical) {
    return RubricCriterion.builder()
        .metricKey(metricKey)
        .judgeInstruction("Judge " + metricKey)
        .weight(weight)
        .critical(critical)
        .build();
  }
}
