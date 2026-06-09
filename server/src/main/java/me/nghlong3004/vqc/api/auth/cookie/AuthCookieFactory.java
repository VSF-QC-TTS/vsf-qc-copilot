package me.nghlong3004.vqc.api.auth.cookie;

import org.springframework.http.ResponseCookie;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public interface AuthCookieFactory {

  /**
   * Builds the HttpOnly refresh-token {@link ResponseCookie} returned after successful login.
   *
   * @param refreshToken raw refresh token value to store in the cookie
   * @param maxAgeSeconds cookie max age in seconds
   * @return configured refresh token {@link ResponseCookie}
   */
  ResponseCookie refreshTokenCookie(String refreshToken, long maxAgeSeconds);

  /**
   * Builds the HttpOnly refresh-token {@link ResponseCookie} used to clear the browser cookie.
   *
   * @return configured expired refresh token {@link ResponseCookie}
   */
  ResponseCookie clearRefreshTokenCookie();
}
