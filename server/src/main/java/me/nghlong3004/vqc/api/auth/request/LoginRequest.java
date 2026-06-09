package me.nghlong3004.vqc.api.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Schema(name = "LoginRequest", description = "Local account login request")
public record LoginRequest(
    @Schema(description = "User email address.", example = "qc.demo@example.com")
        @NotBlank(message = "Email is required.")
        @Email(message = "Email must be a valid email address.")
        @Size(max = 100, message = "Email must not exceed 100 characters.")
        String email,
    @Schema(
            description = "Plain-text password.",
            example = "password123",
            minLength = 8,
            maxLength = 72,
            accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters.")
        String password) {}
