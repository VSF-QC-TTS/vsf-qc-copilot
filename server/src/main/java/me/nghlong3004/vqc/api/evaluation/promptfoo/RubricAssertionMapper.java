package me.nghlong3004.vqc.api.evaluation.promptfoo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.nghlong3004.vqc.api.rubric.entity.RubricCriterion;
import me.nghlong3004.vqc.api.rubric.entity.RubricVersion;
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
    return toAssertions(null, criteria);
  }

  /**
   * Converts a list of rubric criteria into Promptfoo assertion config maps with shared rubric
   * context from the owning version.
   *
   * @param rubricVersion the rubric version that owns the criteria
   * @param criteria the published rubric criteria, ordered by sort order
   * @return list of assertion maps suitable for inclusion in a Promptfoo test's {@code assert}
   *     array; empty list if criteria is null or empty
   */
  public List<Map<String, Object>> toAssertions(
      RubricVersion rubricVersion, List<RubricCriterion> criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return List.of();
    }
    String rubricContext =
        rubricVersion == null || rubricVersion.getContent() == null
            ? ""
            : rubricVersion.getContent().trim();
    return criteria.stream().map(criterion -> toAssertion(rubricContext, criterion)).toList();
  }

  private Map<String, Object> toAssertion(String rubricContext, RubricCriterion criterion) {
    Map<String, Object> assertion = new LinkedHashMap<>();
    assertion.put("type", "llm-rubric");
    assertion.put("value", buildJudgePrompt(rubricContext, criterion));
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
  private String buildJudgePrompt(String rubricContext, RubricCriterion criterion) {
    StringBuilder prompt = new StringBuilder();
    prompt
        .append("Overall rubric context:\n")
        .append(rubricContext == null || rubricContext.isBlank() ? "(none)" : rubricContext)
        .append("\n\n")
        .append("Evaluate this single criterion for the chatbot answer. ")
        .append("Use semantic equivalence with the expected answer; do not require exact wording ")
        .append("unless the criterion explicitly requires it.\n\n")
        .append("Question: {{question}}\n")
        .append("Expected answer: {{groundTruth}}\n")
        .append("Preconditions: {{precondition}}\n")
        .append("Metadata: {{metadata}}\n\n");

    appendIfPresent(prompt, "Criterion", criterion.getName());
    appendIfPresent(prompt, "Description", criterion.getDescription());
    prompt.append("Judge instruction: ").append(criterion.getJudgeInstruction().trim());
    if (criterion.getPassCondition() != null && !criterion.getPassCondition().isBlank()) {
      prompt.append("\n\nPASS condition: ").append(criterion.getPassCondition().trim());
    }
    if (criterion.getFailCondition() != null && !criterion.getFailCondition().isBlank()) {
      prompt.append("\n\nFAIL condition: ").append(criterion.getFailCondition().trim());
    }
    return prompt.toString();
  }

  private void appendIfPresent(StringBuilder prompt, String label, String value) {
    if (value != null && !value.isBlank()) {
      prompt.append(label).append(": ").append(value.trim()).append('\n');
    }
  }
}
