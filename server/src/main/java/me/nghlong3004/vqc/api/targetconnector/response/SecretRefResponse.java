package me.nghlong3004.vqc.api.targetconnector.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "SecretRefResponse", description = "Masked connector secret reference")
public record SecretRefResponse(
    @Schema(description = "Secret key.", example = "CHATBOT_API_TOKEN") String secretKey,
    @Schema(description = "Masked secret value.", example = "****value") String maskedValue) {}
