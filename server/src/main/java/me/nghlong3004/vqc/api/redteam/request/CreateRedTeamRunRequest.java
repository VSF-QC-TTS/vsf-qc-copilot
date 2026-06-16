package me.nghlong3004.vqc.api.redteam.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Schema(name = "CreateRedTeamRunRequest", description = "Request to queue a Promptfoo red-team run")
public record CreateRedTeamRunRequest(
    @Schema(description = "Run name.", example = "Default safety scan")
        @Size(max = 255)
        String name,
    @Schema(description = "Target connector public identifier.")
        @NotNull
        UUID targetConnectorPublicId,
    @Schema(description = "Judge/generation model public identifier.", nullable = true)
        UUID judgeModelPublicId,
    @Schema(description = "Application purpose for red-team probe generation.")
        @Size(max = 4000)
        String purpose,
    @Schema(description = "Promptfoo red-team plugins. Defaults to a compact safety set.")
        List<String> plugins,
    @Schema(description = "Promptfoo red-team strategies. Defaults to basic.")
        List<String> strategies,
    @Schema(description = "Number of tests per plugin.", example = "1")
        @Min(1)
        @Max(10)
        Integer numTests) {}
