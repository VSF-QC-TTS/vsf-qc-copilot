package me.nghlong3004.vqc.api.targetconnector.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * Response for a connector created from a raw cURL command, including the auto-test call result.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/14/2026
 */
@Schema(
    name = "CreateConnectorFromCurlResponse",
    description = "Connector created from cURL with auto-test result")
public record CreateConnectorFromCurlResponse(
    @Schema(description = "Created connector detail.") TargetApiConnectorResponse connector,
    @Schema(description = "Masked request preview of the test call.")
        TargetConnectorRequestPreview testRequestPreview,
    @Schema(description = "Raw response from target API.") Map<String, Object> testRawResponse,
    @Schema(description = "Auto-detected answer from response.", nullable = true)
        String testExtractedAnswer,
    @Schema(description = "Test call latency in milliseconds.") long testLatencyMs) {}
