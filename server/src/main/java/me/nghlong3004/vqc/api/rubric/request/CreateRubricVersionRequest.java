package me.nghlong3004.vqc.api.rubric.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Creates a draft rubric version, optionally cloning content and criteria from
 * an existing version under the same rubric.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Schema(name = "CreateRubricVersionRequest", description = "Create rubric version payload")
public record CreateRubricVersionRequest(
    @Schema(description = "Optional source rubric version to clone.", nullable = true)
        UUID sourceVersionPublicId) {}
