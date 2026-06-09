package me.nghlong3004.vqc.api.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Schema(name = "ResetPasswordRequest", description = "Password reset confirmation request")
public record ResetPasswordRequest(
    @Schema(description = "Raw password reset token from the reset link.")
        @NotBlank(message = "Password reset token is required.")
        @Size(max = 255, message = "Password reset token must not exceed 255 characters.")
        String token,
    @Schema(
            description = "New plain-text password. It is hashed before persistence.",
            example = "newPassword123",
            minLength = 8,
            maxLength = 72,
            accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "New password is required.")
        @Size(min = 8, max = 72, message = "New password must be between 8 and 72 characters.")
        String newPassword) {}
