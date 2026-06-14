package me.nghlong3004.vqc.api.targetconnector.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Minimal request for updating a target connector by pasting a raw cURL command.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/14/2026
 */
@Schema(
    name = "UpdateConnectorFromCurlRequest",
    description = "Update a target connector by pasting a raw cURL command")
public record UpdateConnectorFromCurlRequest(
    @Schema(description = "Connector name.", nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "Connector name must not be blank.")
        @Size(max = 255, message = "Connector name must be at most 255 characters.")
        String name,
    @Schema(
            description = "Raw cURL command to parse.",
            example =
                "curl --location 'https://api.example.com/v1/chat'"
                    + " --header 'Authorization: Bearer token'"
                    + " --header 'Content-Type: application/json'"
                    + " --data '{\"input\": \"hello\"}'",
            nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "Raw cURL command must not be blank.")
        @Size(max = 20000, message = "Raw cURL must be at most 20,000 characters.")
        String rawCurl,
    @Schema(description = "Optional connector description.", nullable = true)
        @Size(max = 2000, message = "Connector description must be at most 2,000 characters.")
        String description,
    @Schema(
            description = "Optional response selector override. Auto-detected if omitted.",
            nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "Response selector must not be blank.")
        String responseSelector,
    @Schema(description = "Timeout in seconds.", nullable = true)
        @Min(value = 1, message = "Timeout seconds must be at least 1.")
        @Max(value = 300, message = "Timeout seconds must be at most 300.")
        Integer timeoutSeconds,
    @Schema(description = "Retry count.", nullable = true)
        @Min(value = 0, message = "Retry count must be at least 0.")
        @Max(value = 5, message = "Retry count must be at most 5.")
        Integer retryCount,
    @Schema(description = "Whether this connector is active.", nullable = true) Boolean active) {}
