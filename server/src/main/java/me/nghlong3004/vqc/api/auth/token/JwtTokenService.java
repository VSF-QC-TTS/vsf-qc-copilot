package me.nghlong3004.vqc.api.auth.token;

import me.nghlong3004.vqc.api.user.entity.User;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public interface JwtTokenService {

  /**
   * Creates a short-lived access JWT for API authentication by a {@link User}.
   *
   * @param user authenticated {@link User}
   * @return signed access token
   */
  String createAccessToken(User user);

  /**
   * Creates a refresh JWT for session renewal by a {@link User}.
   *
   * @param user authenticated {@link User}
   * @return signed refresh token
   */
  String createRefreshToken(User user);

  /**
   * Returns access token TTL.
   *
   * @return access token lifetime in seconds
   */
  long accessTokenExpiresInSeconds();

  /**
   * Returns refresh token TTL.
   *
   * @return refresh token lifetime in seconds
   */
  long refreshTokenExpiresInSeconds();
}
