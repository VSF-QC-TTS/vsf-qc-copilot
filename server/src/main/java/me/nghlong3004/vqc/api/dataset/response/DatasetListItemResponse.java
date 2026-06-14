package me.nghlong3004.vqc.api.dataset.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.dataset.enums.DatasetSourceType;
import me.nghlong3004.vqc.api.dataset.enums.DatasetStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "DatasetListItemResponse", description = "Dataset list item payload")
public record DatasetListItemResponse(
    @Schema(description = "Public dataset identifier.") UUID publicId,
    @Schema(description = "Owning project public identifier.") UUID projectPublicId,
    @Schema(description = "Dataset name.") String name,
    @Schema(description = "Dataset source type.") DatasetSourceType sourceType,
    @Schema(description = "Dataset lifecycle status.") DatasetStatus status,
    @Schema(description = "Number of test cases in this dataset.") long testCaseCount,
    @Schema(description = "Dataset creation time.") OffsetDateTime createdAt) {}
