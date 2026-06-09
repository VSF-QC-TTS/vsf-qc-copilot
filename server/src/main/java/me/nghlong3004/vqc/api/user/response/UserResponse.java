package me.nghlong3004.vqc.api.user.response;

import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.user.enums.Role;
import me.nghlong3004.vqc.api.user.enums.UserStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public record UserResponse(
    UUID publicId,
    String email,
    String displayName,
    Role role,
    UserStatus status,
    OffsetDateTime lastLoginAt) {}
