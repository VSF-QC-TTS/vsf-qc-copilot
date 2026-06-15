package me.nghlong3004.vqc.api.judge.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import me.nghlong3004.vqc.api.judge.enums.JudgeProvider;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Schema(name = "UpdateJudgeModelRequest", description = "Update judge model payload")
public record UpdateJudgeModelRequest(
    @Schema(description = "Judge model display name.", nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "Judge model name must not be blank.")
        @Size(max = 255, message = "Judge model name must be at most 255 characters.")
        String name,
    @Schema(description = "Judge provider.", nullable = true) JudgeProvider provider,
    @Schema(description = "Provider model name.", nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "Model name must not be blank.")
        @Size(max = 255, message = "Model name must be at most 255 characters.")
        String modelName,
    @Schema(description = "Custom/OpenAI-compatible base URL.", nullable = true)
        @Size(max = 2000, message = "Base URL must be at most 2000 characters.")
        String baseUrl,
    @Schema(description = "Raw API key. Write-only.", nullable = true)
        @Size(max = 4000, message = "API key must be at most 4000 characters.")
        String apiKey,
    @Schema(description = "Additional provider config JSON.", nullable = true)
        @Size(max = 20000, message = "Config JSON must be at most 20000 characters.")
        String configJson,
    @Schema(description = "Whether this judge model is active.", nullable = true)
        Boolean active) {}
