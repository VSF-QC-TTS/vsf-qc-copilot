package me.nghlong3004.vqc.api.dataset.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import me.nghlong3004.vqc.api.dataset.enums.DatasetStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "UpdateDatasetRequest", description = "Update dataset payload")
public record UpdateDatasetRequest(
    @Schema(description = "Dataset name.", example = "Health Demo Dataset v2", nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "Dataset name must not be blank.")
        @Size(max = 255, message = "Dataset name must be at most 255 characters.")
        String name,
    @Schema(description = "Optional dataset description.", example = "Updated description", nullable = true)
        @Size(max = 2000, message = "Dataset description must be at most 2000 characters.")
        String description,
    @Schema(description = "Dataset lifecycle status.", example = "APPROVED", nullable = true)
        DatasetStatus status) {}
