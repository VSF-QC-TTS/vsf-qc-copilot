package me.nghlong3004.vqc.api.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Schema(name = "RegisterRequest", description = "Local account registration request")
public record RegisterRequest(
    @Schema(description = "User email address. Stored as username internally.", example = "qc.demo@example.com")
        @NotBlank(message = "Email is required.")
        @Email(message = "Email must be a valid email address.")
        @Size(max = 100, message = "Email must not exceed 100 characters.")
        String email,
    @Schema(
            description = "Plain-text password. It is hashed before persistence.",
            example = "password123",
            minLength = 8,
            maxLength = 72,
            accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters.")
        String password,
    @Schema(
            description = "Display name. Defaults to the email local-part when omitted.",
            example = "QC Demo",
            maxLength = 255,
            nullable = true)
        @Size(max = 255, message = "Display name must not exceed 255 characters.")
        String displayName) {}
