package me.nghlong3004.vqc.api.targetconnector.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import me.nghlong3004.vqc.api.targetconnector.enums.AuthType;
import me.nghlong3004.vqc.api.targetconnector.enums.BodyType;
import me.nghlong3004.vqc.api.targetconnector.enums.ConnectorProtocol;
import me.nghlong3004.vqc.api.targetconnector.enums.HttpMethodType;
import me.nghlong3004.vqc.api.targetconnector.enums.ResponseFormat;
import me.nghlong3004.vqc.api.targetconnector.enums.StreamingType;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "UpdateTargetApiConnectorRequest", description = "Update target connector payload")
public record UpdateTargetApiConnectorRequest(
    @Schema(description = "Connector name.", nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "Connector name must not be blank.")
        @Size(max = 255, message = "Connector name must be at most 255 characters.")
        String name,
    @Schema(description = "Optional connector description.", nullable = true)
        @Size(max = 2000, message = "Connector description must be at most 2000 characters.")
        String description,
    @Schema(description = "Original cURL command if provided.", nullable = true) String rawCurl,
    @Schema(description = "Connector protocol.", nullable = true) ConnectorProtocol protocol,
    @Schema(description = "HTTP method.", nullable = true) HttpMethodType method,
    @Schema(description = "Base URL.", nullable = true) String baseUrl,
    @Schema(description = "Request path.", nullable = true) String path,
    @Schema(description = "Full target URL.", nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "URL must not be blank.")
        String url,
    @Schema(description = "Header templates with secret placeholders.", nullable = true)
        Map<String, Object> headers,
    @Schema(description = "Query parameter templates.", nullable = true) Map<String, Object> queryParams,
    @Schema(description = "Path parameter templates.", nullable = true) Map<String, Object> pathParams,
    @Schema(description = "Body type.", nullable = true) BodyType bodyType,
    @Schema(description = "JSON body template.", nullable = true) Map<String, Object> bodyTemplate,
    @Schema(description = "Text body template.", nullable = true) String bodyTemplateText,
    @Schema(description = "Authentication type.", nullable = true) AuthType authType,
    @Schema(description = "Authentication config with secret placeholders.", nullable = true)
        Map<String, Object> authConfig,
    @Schema(description = "Write-only raw secret values.", nullable = true) Map<String, String> secretValues,
    @Schema(description = "Response format.", nullable = true) ResponseFormat responseFormat,
    @Schema(description = "Response selector.", nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "Response selector must not be blank.")
        String responseSelector,
    @Schema(description = "Whether the target streams responses.", nullable = true) Boolean isStreaming,
    @Schema(description = "Streaming type.", nullable = true) StreamingType streamingType,
    @Schema(description = "Streaming event selector.", nullable = true) String streamingEventSelector,
    @Schema(description = "Timeout in seconds.", nullable = true)
        @Min(value = 1, message = "Timeout seconds must be at least 1.")
        @Max(value = 300, message = "Timeout seconds must be at most 300.")
        Integer timeoutSeconds,
    @Schema(description = "Retry count.", nullable = true)
        @Min(value = 0, message = "Retry count must be at least 0.")
        @Max(value = 5, message = "Retry count must be at most 5.")
        Integer retryCount,
    @Schema(description = "Whether this connector is active.", nullable = true) Boolean active) {}
