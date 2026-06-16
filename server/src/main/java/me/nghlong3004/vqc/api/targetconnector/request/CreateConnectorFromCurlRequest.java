package me.nghlong3004.vqc.api.targetconnector.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Minimal request for creating a target connector by pasting a raw cURL command. The backend parses
 * the cURL, auto-detects method/URL/headers/body/auth, calls the target API for verification, and
 * saves the connector only on success.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/14/2026
 */
@Schema(
    name = "CreateConnectorFromCurlRequest",
    description = "Create a target connector by pasting a raw cURL command")
public record CreateConnectorFromCurlRequest(
    @Schema(description = "Connector name.", example = "ToanGPT Chatbot")
        @NotBlank(message = "Connector name is required.")
        @Size(max = 255, message = "Connector name must be at most 255 characters.")
        String name,
    @Schema(
            description = "Raw cURL command to parse.",
            example =
                "curl --location 'https://api.example.com/v1/chat'"
                    + " --header 'Authorization: Bearer token'"
                    + " --header 'Content-Type: application/json'"
                    + " --data '{\"input\": \"hello\"}'")
        @NotBlank(message = "Raw cURL command is required.")
        String rawCurl,
    @Schema(description = "Optional connector description.", nullable = true)
        @Size(max = 2000, message = "Connector description must be at most 2,000 characters.")
        String description,
    @Schema(
            description = "Optional response selector override. Auto-detected if omitted.",
            nullable = true)
        String responseSelector,
    @Schema(description = "Timeout in seconds. Default 60.", nullable = true)
        @Min(value = 1, message = "Timeout seconds must be at least 1.")
        @Max(value = 300, message = "Timeout seconds must be at most 300.")
        Integer timeoutSeconds,
    @Schema(description = "Retry count. Default 1.", nullable = true)
        @Min(value = 0, message = "Retry count must be at least 0.")
        @Max(value = 5, message = "Retry count must be at most 5.")
        Integer retryCount) {}
