package me.nghlong3004.vqc.api.auth.service.impl;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.auth.request.ForgotPasswordRequest;
import me.nghlong3004.vqc.api.auth.request.LoginRequest;
import me.nghlong3004.vqc.api.auth.request.ResetPasswordRequest;
import me.nghlong3004.vqc.api.auth.request.VerifyEmailRequest;
import me.nghlong3004.vqc.api.auth.response.LoginResponse;
import me.nghlong3004.vqc.api.auth.response.LoginResult;
import me.nghlong3004.vqc.api.auth.service.AuthService;
import me.nghlong3004.vqc.api.auth.service.EmailVerificationService;
import me.nghlong3004.vqc.api.auth.service.PasswordResetService;
import me.nghlong3004.vqc.api.auth.token.JwtTokenService;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.mail.model.MailRequest;
import me.nghlong3004.vqc.api.mail.model.MailType;
import me.nghlong3004.vqc.api.mail.service.MailService;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.enums.UserStatus;
import me.nghlong3004.vqc.api.user.mapper.UserMapper;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import me.nghlong3004.vqc.api.user.response.UserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private static final String TOKEN_TYPE = "Bearer";

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final JwtTokenService jwtTokenService;
  private final EmailVerificationService emailVerificationService;
  private final PasswordResetService passwordResetService;
  private final MailService mailService;

  @Value("${vqc.client.base-url}")
  private String clientBaseUrl;

  @Override
  @Transactional
  public LoginResult login(LoginRequest request) {
    String email = normalizeEmail(request.email());
    User user =
        userRepository
            .findByUsername(email)
            .orElseThrow(() -> new ResourceException(ErrorCode.BAD_CREDENTIALS));
    if (user.getStatus() == UserStatus.PENDING_EMAIL_VERIFICATION) {
      throw new ResourceException(ErrorCode.EMAIL_NOT_VERIFIED);
    }
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new ResourceException(ErrorCode.ACCOUNT_LOCKED);
    }

    authenticationManager.authenticate(
        UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));

    user.setLastLoginAt(OffsetDateTime.now());
    User saved = userRepository.save(user);

    String accessToken = jwtTokenService.createAccessToken(saved);
    String refreshToken = jwtTokenService.createRefreshToken(saved);
    LoginResponse response =
        new LoginResponse(
            accessToken,
            TOKEN_TYPE,
            jwtTokenService.accessTokenExpiresInSeconds(),
            userMapper.toResponse(saved));
    return new LoginResult(response, refreshToken, jwtTokenService.refreshTokenExpiresInSeconds());
  }

  @Override
  @Transactional
  public UserResponse verifyEmail(VerifyEmailRequest request) {
    User verifiedUser = emailVerificationService.verifyEmail(request.token());
    return userMapper.toResponse(verifiedUser);
  }

  @Override
  @Transactional
  public void forgotPassword(ForgotPasswordRequest request) {
    String email = normalizeEmail(request.email());
    userRepository.findByUsername(email).ifPresent(this::sendPasswordReset);
  }

  @Override
  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    passwordResetService.resetPassword(request.token(), request.newPassword());
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase();
  }

  private void sendPasswordReset(User user) {
    String rawToken = passwordResetService.createResetToken(user);
    try {
      mailService.send(
          MailType.PASSWORD_RESET,
          MailRequest.builder()
              .to(user.getUsername())
              .displayName(user.getDisplayName())
              .actionUrl(buildResetPasswordUrl(rawToken))
              .build());
    } catch (RuntimeException ex) {
      log.warn("Password reset email dispatch failed for user {}", user.getPublicId());
    }
  }

  private String buildResetPasswordUrl(String rawToken) {
    String baseUrl =
        clientBaseUrl == null || clientBaseUrl.isBlank() ? "http://localhost:5173" : clientBaseUrl;
    return UriComponentsBuilder.fromUriString(baseUrl)
        .path("/reset-password")
        .queryParam("token", rawToken)
        .build()
        .toUriString();
  }
}
