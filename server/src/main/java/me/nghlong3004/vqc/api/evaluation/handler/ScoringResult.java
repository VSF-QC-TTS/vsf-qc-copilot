package me.nghlong3004.vqc.api.evaluation.handler;

import java.math.BigDecimal;
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;

/**
 * Immutable result of criteria-based score computation.
 *
 * @param judgeScore weighted aggregate score (0.0000–1.0000)
 * @param judgeStatus effective status after applying critical-criterion override
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
public record ScoringResult(BigDecimal judgeScore, JudgeStatus judgeStatus) {}
