package me.nghlong3004.vqc.api.job.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.job.enums.JobStatus;
import me.nghlong3004.vqc.api.job.enums.JobType;
import me.nghlong3004.vqc.api.job.enums.ResourceType;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Schema(name = "JobDetailResponse", description = "Full job detail")
public record JobDetailResponse(
    @Schema(description = "Job public identifier.") UUID publicId,
    @Schema(description = "Job type.", example = "EVALUATION_RUN") JobType jobType,
    @Schema(description = "Job status.", example = "PENDING") JobStatus status,
    @Schema(description = "Resource type.", example = "EVALUATION_RUN") ResourceType resourceType,
    @Schema(description = "Resource public identifier.") UUID resourcePublicId,
    @Schema(description = "Project public identifier.", nullable = true) UUID projectPublicId,
    @Schema(description = "Current progress.", example = "0") int progressCurrent,
    @Schema(description = "Total progress.", example = "10") int progressTotal,
    @Schema(description = "Error message if failed.", nullable = true) String errorMessage,
    @Schema(description = "Creation time.") OffsetDateTime createdAt,
    @Schema(description = "Start time.", nullable = true) OffsetDateTime startedAt,
    @Schema(description = "Completion time.", nullable = true) OffsetDateTime completedAt,
    @Schema(description = "Last update time.") OffsetDateTime updatedAt) {}
