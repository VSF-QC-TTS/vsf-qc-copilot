package me.nghlong3004.vqc.api.targetconnector.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import me.nghlong3004.vqc.api.targetconnector.enums.HttpMethodType;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "TargetConnectorRequestPreview", description = "Masked rendered target request preview")
public record TargetConnectorRequestPreview(
    @Schema(description = "HTTP method.", example = "POST") HttpMethodType method,
    @Schema(description = "Target URL.") String url,
    @Schema(description = "Masked request headers.") Map<String, Object> headers,
    @Schema(description = "Rendered request body.", nullable = true) Object body) {}
