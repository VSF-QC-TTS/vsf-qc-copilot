package me.nghlong3004.vqc.api.auth.token.impl;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.auth.token.JwtTokenService;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Service
@RequiredArgsConstructor
public class JwtTokenServiceImpl implements JwtTokenService {

  private static final String ISSUER = "vqc-api";
  private static final String ACCESS_TOKEN_TYPE = "access";
  private static final String REFRESH_TOKEN_TYPE = "refresh";

  @Value("${vqc.security.access-expiration}")
  private long accessExpirationMinutes;

  @Value("${vqc.security.refresh-expiration}")
  private long refreshExpirationMinutes;

  private final JwtEncoder jwtEncoder;

  @Override
  public String createAccessToken(User user) {
    return createToken(user, ACCESS_TOKEN_TYPE, accessTokenExpiresInSeconds());
  }

  @Override
  public String createRefreshToken(User user) {
    return createToken(user, REFRESH_TOKEN_TYPE, refreshTokenExpiresInSeconds());
  }

  @Override
  public long accessTokenExpiresInSeconds() {
    return accessExpirationMinutes * 60;
  }

  @Override
  public long refreshTokenExpiresInSeconds() {
    return refreshExpirationMinutes * 60;
  }

  private String createToken(User user, String tokenType, long expiresInSeconds) {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(ISSUER)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(expiresInSeconds))
            .subject(user.getUsername())
            .claim("scope", user.getRole().getAuthority())
            .claim("token_type", tokenType)
            .claim("user_public_id", user.getPublicId().toString())
            .build();
    JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
  }
}
