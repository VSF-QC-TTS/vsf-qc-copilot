package me.nghlong3004.vqc.api.rubric.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "RubricCriterionResponse", description = "Rubric criterion response")
public record RubricCriterionResponse(
    @Schema(description = "Public criterion identifier.") UUID publicId,
    @Schema(description = "Public rubric version identifier.") UUID rubricVersionPublicId,
    @Schema(description = "Criterion name.") String name,
    @Schema(description = "Criterion description.", nullable = true) String description,
    @Schema(description = "Criterion score weight.") BigDecimal weight,
    @Schema(description = "Pass condition.", nullable = true) String passCondition,
    @Schema(description = "Fail condition.", nullable = true) String failCondition,
    @Schema(description = "Judge instruction.") String judgeInstruction,
    @Schema(description = "Stable metric key.") String metricKey,
    @Schema(description = "Whether this criterion is critical.") boolean isCritical,
    @Schema(description = "Display/evaluation order.") int sortOrder,
    @Schema(description = "Criterion creation timestamp.") OffsetDateTime createdAt,
    @Schema(description = "Criterion update timestamp.") OffsetDateTime updatedAt) {}
