package me.nghlong3004.vqc.api.dataset.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import me.nghlong3004.vqc.api.dataset.enums.DatasetSourceType;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "CreateDatasetRequest", description = "Create dataset payload")
public record CreateDatasetRequest(
    @Schema(description = "Dataset source type.", example = "MANUAL")
        @NotNull(message = "Dataset source type is required.")
        DatasetSourceType sourceType,
    @Schema(description = "Dataset name.", example = "Health Demo Dataset")
        @NotBlank(message = "Dataset name is required.")
        @Size(max = 255, message = "Dataset name must be at most 255 characters.")
        String name,
    @Schema(
            description = "Optional dataset description.",
            example = "Sample dataset for Week 4 demo.",
            nullable = true)
        @Size(max = 2000, message = "Dataset description must be at most 2000 characters.")
        String description) {}
