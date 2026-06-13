package me.nghlong3004.vqc.api.evaluation.promptfoo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.nghlong3004.vqc.api.rubric.entity.RubricCriterion;
import org.springframework.stereotype.Component;

/**
 * Maps {@link RubricCriterion} entities into Promptfoo {@code llm-rubric} assertion config maps.
 *
 * <p>Pure mapping with no I/O or side effects. Each criterion translates to one {@code llm-rubric}
 * assertion that Promptfoo's grading model evaluates at runtime.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
@Component
public class RubricAssertionMapper {

  /**
   * Converts a list of rubric criteria into Promptfoo assertion config maps.
   *
   * @param criteria the published rubric criteria, ordered by sort order
   * @return list of assertion maps suitable for inclusion in a Promptfoo test's {@code assert}
   *     array; empty list if criteria is null or empty
   */
  public List<Map<String, Object>> toAssertions(List<RubricCriterion> criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return List.of();
    }
    return criteria.stream().map(this::toAssertion).toList();
  }

  private Map<String, Object> toAssertion(RubricCriterion criterion) {
    Map<String, Object> assertion = new LinkedHashMap<>();
    assertion.put("type", "llm-rubric");
    assertion.put("value", buildJudgePrompt(criterion));
    assertion.put("metric", criterion.getMetricKey());
    if (criterion.getWeight() != null) {
      assertion.put("weight", criterion.getWeight());
    }
    return assertion;
  }

  /**
   * Builds the judge prompt from criterion fields. Combines {@code judgeInstruction} with optional
   * {@code passCondition} and {@code failCondition} for clearer grading context.
   */
  private String buildJudgePrompt(RubricCriterion criterion) {
    StringBuilder prompt = new StringBuilder(criterion.getJudgeInstruction());
    if (criterion.getPassCondition() != null && !criterion.getPassCondition().isBlank()) {
      prompt.append("\n\nPASS condition: ").append(criterion.getPassCondition().trim());
    }
    if (criterion.getFailCondition() != null && !criterion.getFailCondition().isBlank()) {
      prompt.append("\n\nFAIL condition: ").append(criterion.getFailCondition().trim());
    }
    return prompt.toString();
  }
}
