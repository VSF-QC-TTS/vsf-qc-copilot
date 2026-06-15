package me.nghlong3004.vqc.api.judge.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.judge.enums.JudgeProvider;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Schema(name = "JudgeModelResponse", description = "Judge model response")
public record JudgeModelResponse(
    @Schema(description = "Public judge model identifier.") UUID publicId,
    @Schema(description = "Public project identifier.") UUID projectPublicId,
    @Schema(description = "Judge model display name.") String name,
    @Schema(description = "Judge provider.") JudgeProvider provider,
    @Schema(description = "Provider model name.") String modelName,
    @Schema(description = "Custom/OpenAI-compatible base URL.", nullable = true) String baseUrl,
    @Schema(description = "Masked API key.", nullable = true) String apiKeyMasked,
    @Schema(description = "Additional provider config JSON.", nullable = true) String configJson,
    @Schema(description = "Whether this judge model is active.") Boolean active,
    @Schema(description = "Creation timestamp.") OffsetDateTime createdAt,
    @Schema(description = "Update timestamp.") OffsetDateTime updatedAt) {}
