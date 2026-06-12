package me.nghlong3004.vqc.api.evaluation.executor;

import java.math.BigDecimal;
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public record PromptfooResult(
    Long testCaseId,
    String actualAnswer,
    BigDecimal judgeScore,
    JudgeStatus judgeStatus,
    String judgeReason,
    Integer latencyMs,
    String errorMessage,
    String rawPromptfooResultJson,
    String criteriaResultsJson) {}
