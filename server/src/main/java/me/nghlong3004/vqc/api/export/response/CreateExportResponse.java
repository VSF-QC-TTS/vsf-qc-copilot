package me.nghlong3004.vqc.api.export.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import me.nghlong3004.vqc.api.export.enums.ExportFileStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Schema(name = "CreateExportResponse", description = "Export creation result")
public record CreateExportResponse(
    @Schema(description = "Export public identifier.") UUID exportPublicId,
    @Schema(description = "Job public identifier.") UUID jobPublicId,
    @Schema(description = "Export status.") ExportFileStatus status,
    @Schema(description = "Human-readable message.") String message) {}
