package me.nghlong3004.vqc.api.auth.cookie;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Component
public class RefreshTokenCookieFactory implements AuthCookieFactory {

  private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

  @Value("${vqc.security.cookie.secure}")
  private boolean cookieSecure;

  @Value("${vqc.security.cookie.same-site:Lax}")
  private String sameSite;

  @Override
  public ResponseCookie refreshTokenCookie(String refreshToken, long maxAgeSeconds) {
    return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
        .httpOnly(true)
        .secure(cookieSecure)
        .path("/api/v1/auth")
        .maxAge(maxAgeSeconds)
        .sameSite(sameSite)
        .build();
  }

  @Override
  public ResponseCookie clearRefreshTokenCookie() {
    return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
        .httpOnly(true)
        .secure(cookieSecure)
        .path("/api/v1/auth")
        .maxAge(0)
        .sameSite(sameSite)
        .build();
  }
}
