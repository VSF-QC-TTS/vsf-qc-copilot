package me.nghlong3004.vqc.api.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Schema(name = "VerifyEmailRequest", description = "Email verification token request")
public record VerifyEmailRequest(
    @Schema(description = "Raw email verification token from the verification link.")
        @NotBlank(message = "Verification token is required.")
        @Size(max = 255, message = "Verification token must not exceed 255 characters.")
        String token) {}
