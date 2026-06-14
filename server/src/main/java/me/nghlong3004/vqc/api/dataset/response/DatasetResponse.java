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
@Schema(name = "DatasetResponse", description = "Dataset detail payload")
public record DatasetResponse(
    @Schema(
            description = "Public dataset identifier. Internal numeric ids are never exposed.",
            example = "0f6d90c2-7410-4db2-86be-8adfd3140f63")
        UUID publicId,
    @Schema(description = "Owning project public identifier.") UUID projectPublicId,
    @Schema(description = "Dataset name.", example = "Health Demo Dataset") String name,
    @Schema(description = "Dataset description.", nullable = true) String description,
    @Schema(description = "Dataset version.", example = "1") Integer version,
    @Schema(description = "Dataset source type.", example = "MANUAL") DatasetSourceType sourceType,
    @Schema(description = "Dataset lifecycle status.", example = "DRAFT") DatasetStatus status,
    @Schema(description = "Total number of test cases in this dataset.", example = "0") long testCaseCount,
    @Schema(description = "Number of active test cases in this dataset.", example = "0") long activeTestCaseCount,
    @Schema(description = "Dataset creation time.", example = "2026-06-08T10:30:00+07:00")
        OffsetDateTime createdAt,
    @Schema(description = "Last dataset update time.", example = "2026-06-08T10:30:00+07:00")
        OffsetDateTime updatedAt) {}
