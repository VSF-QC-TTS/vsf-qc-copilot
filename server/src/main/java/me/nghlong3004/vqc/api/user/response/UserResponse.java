package me.nghlong3004.vqc.api.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.user.enums.Role;
import me.nghlong3004.vqc.api.user.enums.UserStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Schema(name = "UserResponse", description = "Public user payload")
public record UserResponse(
    @Schema(
            description = "Public user identifier. Internal numeric ids are never exposed.",
            example = "7b7b7d42-5f42-4c5a-9281-8d1d36f6f59d")
        UUID publicId,
    @Schema(description = "User email address.", example = "qc.demo@example.com") String email,
    @Schema(description = "User display name.", example = "QC Demo") String displayName,
    @Schema(description = "Application role.", example = "QC_MEMBER") Role role,
    @Schema(description = "User account status.", example = "ACTIVE") UserStatus status,
    @Schema(description = "Last successful login time.", example = "2026-06-09T10:00:00Z", nullable = true)
        OffsetDateTime lastLoginAt) {}
