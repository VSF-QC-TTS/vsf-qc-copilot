package me.nghlong3004.vqc.api.targetconnector.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "TestTargetConnectorRequest", description = "Target connector test-run payload")
public record TestTargetConnectorRequest(
    @Schema(description = "Question rendered into connector templates.")
        @NotBlank(message = "Question is required.")
        String question,
    @Schema(description = "Optional precondition/context object.", nullable = true)
        Map<String, Object> precondition,
    @Schema(description = "Optional metadata object.", nullable = true) Map<String, Object> metadata) {}
