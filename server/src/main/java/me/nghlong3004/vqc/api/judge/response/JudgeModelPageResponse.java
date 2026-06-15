package me.nghlong3004.vqc.api.judge.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Schema(name = "JudgeModelPageResponse", description = "Judge model page response")
public record JudgeModelPageResponse(
    List<JudgeModelResponse> items, int page, int size, long totalItems, int totalPages) {}
