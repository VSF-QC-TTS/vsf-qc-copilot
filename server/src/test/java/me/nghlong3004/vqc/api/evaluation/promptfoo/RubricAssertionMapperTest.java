package me.nghlong3004.vqc.api.evaluation.promptfoo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import me.nghlong3004.vqc.api.rubric.entity.RubricCriterion;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
class RubricAssertionMapperTest {

  private final RubricAssertionMapper mapper = new RubricAssertionMapper();

  @Test
  void toAssertionsReturnsEmptyListForNullCriteria() {
    assertThat(mapper.toAssertions(null)).isEmpty();
  }

  @Test
  void toAssertionsReturnsEmptyListForEmptyCriteria() {
    assertThat(mapper.toAssertions(List.of())).isEmpty();
  }

  @Test
  void toAssertionsMapsBasicCriterion() {
    RubricCriterion criterion =
        RubricCriterion.builder()
            .metricKey("accuracy")
            .judgeInstruction("Check if the answer is accurate.")
            .weight(6)
            .build();

    List<Map<String, Object>> assertions = mapper.toAssertions(List.of(criterion));

    assertThat(assertions).hasSize(1);
    Map<String, Object> assertion = assertions.getFirst();
    assertThat(assertion.get("type")).isEqualTo("llm-rubric");
    assertThat(assertion.get("value")).isEqualTo("Check if the answer is accurate.");
    assertThat(assertion.get("metric")).isEqualTo("accuracy");
    assertThat((int) assertion.get("weight")).isEqualTo(6);
  }

  @Test
  void toAssertionsIncludesPassAndFailConditions() {
    RubricCriterion criterion =
        RubricCriterion.builder()
            .metricKey("tone")
            .judgeInstruction("Evaluate the tone of the response.")
            .passCondition("Response is professional and empathetic.")
            .failCondition("Response is rude or dismissive.")
            .weight(4)
            .build();

    List<Map<String, Object>> assertions = mapper.toAssertions(List.of(criterion));

    String value = (String) assertions.getFirst().get("value");
    assertThat(value).contains("Evaluate the tone of the response.");
    assertThat(value).contains("PASS condition: Response is professional and empathetic.");
    assertThat(value).contains("FAIL condition: Response is rude or dismissive.");
  }

  @Test
  void toAssertionsSkipsBlankConditions() {
    RubricCriterion criterion =
        RubricCriterion.builder()
            .metricKey("relevance")
            .judgeInstruction("Is the response relevant?")
            .passCondition("   ")
            .failCondition(null)
            .weight(10)
            .build();

    String value = (String) mapper.toAssertions(List.of(criterion)).getFirst().get("value");
    assertThat(value).isEqualTo("Is the response relevant?");
    assertThat(value).doesNotContain("PASS condition");
    assertThat(value).doesNotContain("FAIL condition");
  }

  @Test
  void toAssertionsOmitsWeightWhenNull() {
    RubricCriterion criterion =
        RubricCriterion.builder()
            .metricKey("safety")
            .judgeInstruction("Check safety.")
            .weight(null)
            .build();

    Map<String, Object> assertion = mapper.toAssertions(List.of(criterion)).getFirst();
    assertThat(assertion).doesNotContainKey("weight");
  }

  @Test
  void toAssertionsMapsMultipleCriteriaInOrder() {
    RubricCriterion c1 =
        RubricCriterion.builder()
            .metricKey("accuracy")
            .judgeInstruction("Accurate?")
            .weight(5)
            .build();
    RubricCriterion c2 =
        RubricCriterion.builder()
            .metricKey("completeness")
            .judgeInstruction("Complete?")
            .weight(3)
            .build();
    RubricCriterion c3 =
        RubricCriterion.builder()
            .metricKey("tone")
            .judgeInstruction("Good tone?")
            .weight(2)
            .build();

    List<Map<String, Object>> assertions = mapper.toAssertions(List.of(c1, c2, c3));

    assertThat(assertions).hasSize(3);
    assertThat(assertions.get(0).get("metric")).isEqualTo("accuracy");
    assertThat(assertions.get(1).get("metric")).isEqualTo("completeness");
    assertThat(assertions.get(2).get("metric")).isEqualTo("tone");
  }
}
