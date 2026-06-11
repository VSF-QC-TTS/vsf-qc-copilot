package me.nghlong3004.vqc.api.export.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.export.enums.ExportFileStatus;
import me.nghlong3004.vqc.api.export.enums.ExportFileType;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Schema(name = "ExportFileResponse", description = "Export file detail")
public record ExportFileResponse(
    @Schema(description = "Export public identifier.") UUID publicId,
    @Schema(description = "Project public identifier.") UUID projectPublicId,
    @Schema(description = "Evaluation run public identifier.") UUID evaluationRunPublicId,
    @Schema(description = "Job public identifier.", nullable = true) UUID jobPublicId,
    @Schema(description = "Export file type.") ExportFileType fileType,
    @Schema(description = "Export status.") ExportFileStatus status,
    @Schema(description = "Download file name.", nullable = true) String fileName,
    @Schema(description = "Download URL.", nullable = true) String downloadUrl,
    @Schema(description = "Failure reason.", nullable = true) String errorMessage,
    @Schema(description = "Creation time.") OffsetDateTime createdAt,
    @Schema(description = "Ready time.", nullable = true) OffsetDateTime readyAt) {}
