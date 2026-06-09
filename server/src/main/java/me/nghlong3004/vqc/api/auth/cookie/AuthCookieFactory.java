package me.nghlong3004.vqc.api.auth.cookie;

import org.springframework.http.ResponseCookie;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public interface AuthCookieFactory {

  ResponseCookie refreshTokenCookie(String refreshToken, long maxAgeSeconds);
}
