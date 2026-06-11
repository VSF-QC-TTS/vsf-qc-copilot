package me.nghlong3004.vqc.api.export.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import me.nghlong3004.vqc.api.export.enums.ExportFileType;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Schema(name = "CreateExportRequest", description = "Create export payload")
public record CreateExportRequest(
    @NotNull(message = "Export file type is required.")
        @Schema(description = "Export file type.", example = "EXCEL")
        ExportFileType fileType) {}
