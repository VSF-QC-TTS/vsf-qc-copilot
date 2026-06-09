package me.nghlong3004.vqc.api.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.auth.request.RegisterRequest;
import me.nghlong3004.vqc.api.auth.service.EmailVerificationService;
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
import me.nghlong3004.vqc.api.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final MailService mailService;
  private final EmailVerificationService emailVerificationService;

  @Value("${vqc.client.base-url}")
  private String clientBaseUrl;

  @Override
  @Transactional
  public UserResponse register(RegisterRequest request) {
    String email = normalizeEmail(request.email());
    if (userRepository.existsByUsername(email)) {
      log.warn("Registration rejected because the email is already registered");
      throw new ResourceException(ErrorCode.EMAIL_ALREADY);
    }

    log.debug("Creating local user account");
    User user = new User();
    user.setUsername(email);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setDisplayName(resolveDisplayName(request.displayName(), email));
    user.setStatus(UserStatus.PENDING_EMAIL_VERIFICATION);

    User saved;
    try {
      saved = userRepository.save(user);
    } catch (DataIntegrityViolationException ex) {
      log.warn("Registration rejected because the email is already registered");
      throw new ResourceException(ErrorCode.EMAIL_ALREADY);
    }

    log.info("Registered local user {}", saved.getPublicId());
    UserResponse response = userMapper.toResponse(saved);
    sendEmailVerification(saved);
    return response;
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase();
  }

  private String resolveDisplayName(String displayName, String email) {
    if (displayName != null && !displayName.isBlank()) {
      return displayName.trim();
    }
    return email.substring(0, email.indexOf('@'));
  }

  private void sendEmailVerification(User user) {
    String rawToken = emailVerificationService.createVerificationToken(user);
    try {
      mailService.send(
          MailType.EMAIL_VERIFICATION,
          MailRequest.builder()
              .to(user.getUsername())
              .displayName(user.getDisplayName())
              .actionUrl(buildVerificationUrl(rawToken))
              .build());
    } catch (RuntimeException ex) {
      log.warn("Email verification dispatch failed for user {}", user.getPublicId());
    }
  }

  private String buildVerificationUrl(String rawToken) {
    String baseUrl =
        clientBaseUrl == null || clientBaseUrl.isBlank() ? "http://localhost:5173" : clientBaseUrl;
    return UriComponentsBuilder.fromUriString(baseUrl)
        .path("/verify-email")
        .queryParam("token", rawToken)
        .build()
        .toUriString();
  }
}
