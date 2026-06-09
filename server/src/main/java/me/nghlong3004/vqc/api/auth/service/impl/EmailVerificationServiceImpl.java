package me.nghlong3004.vqc.api.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.auth.entity.EmailVerificationToken;
import me.nghlong3004.vqc.api.auth.repository.EmailVerificationTokenRepository;
import me.nghlong3004.vqc.api.auth.service.EmailVerificationService;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.enums.UserStatus;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

  private static final int TOKEN_BYTES = 32;
  private static final long TOKEN_TTL_HOURS = 24;

  private final EmailVerificationTokenRepository tokenRepository;
  private final UserRepository userRepository;
  private final SecureRandom secureRandom = new SecureRandom();

  @Override
  @Transactional
  public String createVerificationToken(User user) {
    String rawToken = generateRawToken();
    EmailVerificationToken token = new EmailVerificationToken();
    token.setUser(user);
    token.setTokenHash(hash(rawToken));
    token.setExpiresAt(OffsetDateTime.now().plusHours(TOKEN_TTL_HOURS));
    tokenRepository.save(token);
    return rawToken;
  }

  @Override
  @Transactional
  public User verifyEmail(String rawToken) {
    OffsetDateTime now = OffsetDateTime.now();
    EmailVerificationToken token =
        tokenRepository
            .findByTokenHash(hash(rawToken))
            .orElseThrow(() -> new ResourceException(ErrorCode.INVALID_EMAIL_VERIFICATION_TOKEN));

    if (token.isUsed()) {
      throw new ResourceException(ErrorCode.EMAIL_VERIFICATION_TOKEN_USED);
    }
    if (token.isExpired(now)) {
      throw new ResourceException(ErrorCode.EMAIL_VERIFICATION_TOKEN_EXPIRED);
    }

    User user = token.getUser();
    user.setStatus(UserStatus.ACTIVE);
    token.setUsedAt(now);
    userRepository.save(user);
    tokenRepository.save(token);
    return user;
  }

  private String generateRawToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        builder.append(String.format("%02x", b));
      }
      return builder.toString();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }
}
