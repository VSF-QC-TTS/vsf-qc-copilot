package me.nghlong3004.vqc.api.targetconnector.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
@Schema(name = "TargetApiConnectorResponse", description = "Target connector detail payload")
public record TargetApiConnectorResponse(
    @Schema(description = "Public connector identifier.") UUID publicId,
    @Schema(description = "Public project identifier.") UUID projectPublicId,
    @Schema(description = "Connector name.") String name,
    @Schema(description = "Connector description.", nullable = true) String description,
    @Schema(description = "Connector protocol.", example = "HTTP") ConnectorProtocol protocol,
    @Schema(description = "HTTP method.", example = "POST") HttpMethodType method,
    @Schema(description = "Base URL.", nullable = true) String baseUrl,
    @Schema(description = "Request path.", nullable = true) String path,
    @Schema(description = "Full target URL.") String url,
    @Schema(description = "Header templates with secret placeholders.", nullable = true)
        Map<String, Object> headers,
    @Schema(description = "Query parameter templates.", nullable = true) Map<String, Object> queryParams,
    @Schema(description = "Path parameter templates.", nullable = true) Map<String, Object> pathParams,
    @Schema(description = "Body type.", example = "RAW_JSON") BodyType bodyType,
    @Schema(description = "JSON body template.", nullable = true) Map<String, Object> bodyTemplate,
    @Schema(description = "Text body template.", nullable = true) String bodyTemplateText,
    @Schema(description = "Authentication type.", example = "BEARER") AuthType authType,
    @Schema(description = "Authentication config with secret placeholders.", nullable = true)
        Map<String, Object> authConfig,
    @Schema(description = "Masked secret references.") List<SecretRefResponse> secretRefs,
    @Schema(description = "Response format.", example = "JSON") ResponseFormat responseFormat,
    @Schema(description = "Response selector.", example = "$.answer") String responseSelector,
    @Schema(description = "Inferred response schema with values replaced by JSON types.", nullable = true)
        Map<String, Object> responseSchema,
    @Schema(description = "Whether the target streams responses.") Boolean isStreaming,
    @Schema(description = "Streaming type.", nullable = true) StreamingType streamingType,
    @Schema(description = "Streaming event selector.", nullable = true) String streamingEventSelector,
    @Schema(description = "Timeout in seconds.", example = "60") Integer timeoutSeconds,
    @Schema(description = "Retry count.", example = "1") Integer retryCount,
    @Schema(description = "Whether this connector is active.", example = "true") Boolean active,
    @Schema(description = "Connector creation time.") OffsetDateTime createdAt,
    @Schema(description = "Last connector update time.") OffsetDateTime updatedAt) {}
