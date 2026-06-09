package me.nghlong3004.vqc.api.auth.service.impl;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.auth.entity.PasswordResetToken;
import me.nghlong3004.vqc.api.auth.repository.PasswordResetTokenRepository;
import me.nghlong3004.vqc.api.auth.service.PasswordResetService;
import me.nghlong3004.vqc.api.auth.token.OpaqueTokenService;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

  private static final long TOKEN_TTL_HOURS = 1;

  private final PasswordResetTokenRepository tokenRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final OpaqueTokenService opaqueTokenService;

  @Override
  @Transactional
  public String createResetToken(User user) {
    String rawToken = opaqueTokenService.generateRawToken();
    PasswordResetToken token = new PasswordResetToken();
    token.setUser(user);
    token.setTokenHash(opaqueTokenService.hash(rawToken));
    token.setExpiresAt(OffsetDateTime.now().plusHours(TOKEN_TTL_HOURS));
    tokenRepository.save(token);
    return rawToken;
  }

  @Override
  @Transactional
  public void resetPassword(String rawToken, String newPassword) {
    OffsetDateTime now = OffsetDateTime.now();
    PasswordResetToken token =
        tokenRepository
            .findByTokenHash(opaqueTokenService.hash(rawToken))
            .orElseThrow(() -> new ResourceException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));

    if (token.isUsed()) {
      throw new ResourceException(ErrorCode.PASSWORD_RESET_TOKEN_USED);
    }
    if (token.isExpired(now)) {
      throw new ResourceException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
    }

    User user = token.getUser();
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    token.setUsedAt(now);
    userRepository.save(user);
    tokenRepository.save(token);
  }
}
