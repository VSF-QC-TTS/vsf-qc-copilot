package me.nghlong3004.vqc.api.evaluation.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;
import me.nghlong3004.vqc.api.rubric.entity.RubricCriterion;
import org.springframework.stereotype.Component;

/**
 * Computes weighted judge scores from per-criterion evaluation results and applies
 * {@code isCritical} override logic.
 *
 * <p>Pure computation — no I/O, no database access. Receives parsed criteria results JSON and
 * rubric criteria metadata, returns a {@link ScoringResult}.
 *
 * <p><strong>Scoring rules:</strong>
 *
 * <ul>
 *   <li>Weighted score = Σ(criterion.weight × criterion.score) / Σ(criterion.weight)
 *   <li>If any criterion with {@code isCritical=true} fails → force {@link JudgeStatus#FAIL}
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
   * @param criteria the rubric criteria with weight and isCritical metadata
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

    // Build critical metric keys set
    Set<String> criticalMetricKeys =
        criteria.stream()
            .filter(c -> Boolean.TRUE.equals(c.getCritical()))
            .map(RubricCriterion::getMetricKey)
            .collect(Collectors.toSet());

    // Build weight lookup
    Map<String, BigDecimal> weightByMetric =
        criteria.stream()
            .collect(
                Collectors.toMap(
                    RubricCriterion::getMetricKey,
                    c -> c.getWeight() != null ? BigDecimal.valueOf(c.getWeight()) : BigDecimal.ONE));

    BigDecimal weightedSum = BigDecimal.ZERO;
    BigDecimal totalWeight = BigDecimal.ZERO;
    boolean anyCriticalFailed = false;

    for (Map<String, Object> result : results) {
      String metricKey = String.valueOf(result.get("metricKey"));
      BigDecimal weight = weightByMetric.getOrDefault(metricKey, BigDecimal.ONE);
      BigDecimal score = toBigDecimal(result.get("score"));
      boolean pass = toBoolean(result.get("pass"));

      weightedSum = weightedSum.add(weight.multiply(score));
      totalWeight = totalWeight.add(weight);

      if (!pass && criticalMetricKeys.contains(metricKey)) {
        anyCriticalFailed = true;
      }
    }

    BigDecimal judgeScore =
        totalWeight.compareTo(BigDecimal.ZERO) > 0
            ? weightedSum.divide(totalWeight, 4, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    JudgeStatus status;
    if (anyCriticalFailed) {
      status = JudgeStatus.FAIL;
    } else {
      status = fallbackStatus;
    }

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
