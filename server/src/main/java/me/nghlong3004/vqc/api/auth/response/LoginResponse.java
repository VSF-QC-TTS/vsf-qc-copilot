package me.nghlong3004.vqc.api.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;
import me.nghlong3004.vqc.api.user.response.UserResponse;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Schema(name = "LoginResponse", description = "Local login response")
public record LoginResponse(
    @Schema(description = "JWT access token.", accessMode = Schema.AccessMode.READ_ONLY)
        String accessToken,
    @Schema(description = "Access token type.", example = "Bearer") String tokenType,
    @Schema(description = "Access token lifetime in seconds.", example = "900")
        long expiresInSeconds,
    @Schema(description = "Authenticated user.") UserResponse user) {}
