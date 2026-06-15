package me.nghlong3004.vqc.api.judge.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import me.nghlong3004.vqc.api.judge.enums.JudgeProvider;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Schema(name = "CreateJudgeModelRequest", description = "Create judge model payload")
public record CreateJudgeModelRequest(
    @Schema(description = "Judge model display name.", example = "Gemini QC Judge")
        @NotBlank(message = "Judge model name is required.")
        @Size(max = 255, message = "Judge model name must be at most 255 characters.")
        String name,
    @Schema(description = "Judge provider.", example = "GEMINI")
        @NotNull(message = "Judge provider is required.")
        JudgeProvider provider,
    @Schema(description = "Provider model name.", example = "gemini-2.5-flash")
        @NotBlank(message = "Model name is required.")
        @Size(max = 255, message = "Model name must be at most 255 characters.")
        String modelName,
    @Schema(description = "Custom/OpenAI-compatible base URL.", nullable = true)
        @Size(max = 2000, message = "Base URL must be at most 2000 characters.")
        String baseUrl,
    @Schema(description = "Raw API key. Write-only.")
        @NotBlank(message = "API key is required.")
        @Size(max = 4000, message = "API key must be at most 4000 characters.")
        String apiKey,
    @Schema(description = "Additional provider config JSON.", nullable = true)
        @Size(max = 20000, message = "Config JSON must be at most 20000 characters.")
        String configJson,
    @Schema(description = "Whether this judge model is active.", nullable = true)
        Boolean active) {}
