package me.nghlong3004.vqc.api.auth.response;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public record LoginResult(
    LoginResponse response, String refreshToken, long refreshTokenMaxAgeSeconds) {}
