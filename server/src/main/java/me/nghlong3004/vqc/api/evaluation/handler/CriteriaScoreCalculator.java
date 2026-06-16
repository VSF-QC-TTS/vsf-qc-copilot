package me.nghlong3004.vqc.api.evaluation.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;
import me.nghlong3004.vqc.api.rubric.entity.RubricCriterion;
import org.springframework.stereotype.Component;

/**
 * Computes weighted judge scores from per-criterion evaluation results.
 *
 * <p>Pure computation — no I/O, no database access. Receives parsed criteria results JSON and
 * rubric criteria metadata, returns a {@link ScoringResult}.
 *
 * <p><strong>Scoring rules:</strong>
 *
 * <ul>
 *   <li>Weighted score = Σ(criterion.weight × criterion.score) / Σ(criterion.weight)
 *   <li>If any known criterion fails → force {@link JudgeStatus#FAIL}
 *   <li>If all known criteria pass → force {@link JudgeStatus#PASS}
 *   <li>If no criteria results exist, returns the fallback status unchanged
 * </ul>
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CriteriaScoreCalculator {

  private static final TypeReference<List<Map<String, Object>>> CRITERIA_LIST_TYPE =
      new TypeReference<>() {};

  private final ObjectMapper objectMapper;

  /**
   * Compute weighted score and effective judge status from criteria results.
   *
   * @param criteriaResultsJson JSON array of {@code {metricKey, pass, score, reason}} from parser
   * @param criteria the rubric criteria with metric keys and weights
   * @param fallbackStatus the status to use when no criteria results are available
   * @return scoring result with weighted score and effective status
   */
  public ScoringResult computeScore(
      String criteriaResultsJson,
      List<RubricCriterion> criteria,
      JudgeStatus fallbackStatus) {

    if (criteriaResultsJson == null
        || criteriaResultsJson.isBlank()
        || criteria == null
        || criteria.isEmpty()) {
      return new ScoringResult(null, fallbackStatus);
    }

    List<Map<String, Object>> results = parseResults(criteriaResultsJson);
    if (results.isEmpty()) {
      return new ScoringResult(null, fallbackStatus);
    }

    Map<String, BigDecimal> weightByMetric =
        criteria.stream()
            .collect(
                Collectors.toMap(
                    RubricCriterion::getMetricKey,
                    c -> c.getWeight() != null ? BigDecimal.valueOf(c.getWeight()) : BigDecimal.ONE));

    BigDecimal weightedSum = BigDecimal.ZERO;
    BigDecimal totalWeight = BigDecimal.ZERO;
    boolean anyFailed = false;
    boolean anyGraderError = false;

    for (Map<String, Object> result : results) {
      String metricKey = String.valueOf(result.get("metricKey"));
      BigDecimal weight = weightByMetric.get(metricKey);
      if (weight == null) {
        continue;
      }
      BigDecimal score = toBigDecimal(result.get("score"));
      boolean pass = toBoolean(result.get("pass"));
      boolean graderError = toBoolean(result.get("graderError"));

      weightedSum = weightedSum.add(weight.multiply(score));
      totalWeight = totalWeight.add(weight);

      if (graderError) {
        anyGraderError = true;
      }
      if (!pass) {
        anyFailed = true;
      }
    }

    if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
      return new ScoringResult(null, fallbackStatus);
    }

    BigDecimal judgeScore =
        weightedSum.divide(totalWeight, 4, RoundingMode.HALF_UP);
    JudgeStatus status = anyGraderError ? JudgeStatus.ERROR : (anyFailed ? JudgeStatus.FAIL : JudgeStatus.PASS);

    return new ScoringResult(judgeScore, status);
  }

  private List<Map<String, Object>> parseResults(String json) {
    try {
      return objectMapper.readValue(json, CRITERIA_LIST_TYPE);
    } catch (Exception ex) {
      log.warn("Failed to parse criteriaResultsJson, returning empty: {}", ex.getMessage());
      return List.of();
    }
  }

  private BigDecimal toBigDecimal(Object value) {
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    if (value instanceof String text) {
      try {
        return new BigDecimal(text);
      } catch (NumberFormatException ignored) {
        // fall through
      }
    }
    return BigDecimal.ZERO;
  }

  private boolean toBoolean(Object value) {
    if (value instanceof Boolean b) {
      return b;
    }
    if (value instanceof String text) {
      return Boolean.parseBoolean(text);
    }
    return false;
  }
}
