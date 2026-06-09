package me.nghlong3004.vqc.api.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
@Configuration
public class JwtConfig {
  @Value("${vqc.security.jwt.secret-key}")
  private String secret;

  @Bean
  public SecretKey secretKey() {
    return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  @Bean
  public JwtEncoder jwtEncoder(SecretKey secretKey) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
  }

  @Bean
  public JwtDecoder jwtDecoder(SecretKey secretKey) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey).build();
    decoder.setJwtValidator(tokenTypeValidator("access", "Only access tokens can authenticate API requests."));
    return decoder;
  }

  @Bean("refreshTokenJwtDecoder")
  public JwtDecoder refreshTokenJwtDecoder(SecretKey secretKey) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey).build();
    decoder.setJwtValidator(tokenTypeValidator("refresh", "Only refresh tokens can renew sessions."));
    return decoder;
  }

  private OAuth2TokenValidator<Jwt> tokenTypeValidator(String expectedType, String errorDescription) {
    OAuth2TokenValidator<Jwt> validator =
        jwt -> {
          if (expectedType.equals(jwt.getClaimAsString("token_type"))) {
            return OAuth2TokenValidatorResult.success();
          }
          return OAuth2TokenValidatorResult.failure(
              new OAuth2Error("invalid_token", errorDescription, null));
        };
    return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), validator);
  }
}
