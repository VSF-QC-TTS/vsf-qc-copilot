package me.nghlong3004.vqc.api.auth.token;

import me.nghlong3004.vqc.api.user.entity.User;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public interface JwtTokenService {

  String createAccessToken(User user);

  String createRefreshToken(User user);

  long accessTokenExpiresInSeconds();

  long refreshTokenExpiresInSeconds();
}
