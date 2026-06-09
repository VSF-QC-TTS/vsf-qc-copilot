package me.nghlong3004.vqc.api.oauth.profile;

import me.nghlong3004.vqc.api.oauth.AuthProvider;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
public record OAuth2UserProfile(
    AuthProvider provider, String email, String firstName, String lastName, String avatarUrl) {}
