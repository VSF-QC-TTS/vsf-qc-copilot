package me.nghlong3004.vqc.api.oauth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.auth.token.JwtTokenService;
import me.nghlong3004.vqc.api.oauth.profile.OAuth2UserProfile;
import me.nghlong3004.vqc.api.oauth.profile.OAuth2UserProfileService;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.enums.Role;
import me.nghlong3004.vqc.api.user.enums.UserStatus;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles successful OAuth2 logins by finding or creating a local user, issuing JWT tokens, and
 * redirecting to the client dashboard.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private static final int MAX_DISPLAY_NAME_LENGTH = 255;

  @Value("${vqc.client.base-url}")
  private String webBaseUrl;

  @Value("${vqc.security.refresh-expiration}")
  private int refreshExpirationMinutes;

  @Value("${vqc.security.cookie.secure}")
  private boolean cookieSecure;

  @Value("${vqc.security.cookie.same-site:Lax}")
  private String sameSite;

  private final OAuth2UserProfileService oAuth2UserProfileService;
  private final UserRepository userRepository;
  private final JwtTokenService jwtTokenService;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void onAuthenticationSuccess(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Authentication authentication)
      throws IOException {

    OAuth2UserProfile profile = oAuth2UserProfileService.extract(authentication);
    String email = profile.email();

    if (email == null) {
      log.error("OAuth2 login failed: no email attribute from provider {}", profile.provider());
      response.sendRedirect(webBaseUrl + "/login?error=oauth_no_email");
      return;
    }

    User user = getOrCreateUser(profile);

    String accessToken = jwtTokenService.createAccessToken(user);
    String refreshToken = jwtTokenService.createRefreshToken(user);

    response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshToken).toString());

    var session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }

    log.info("OAuth2 login success for user: {} (provider: {})", email, profile.provider());
    response.sendRedirect(webBaseUrl + "/dashboard");
  }

  // ---------------------------------------------------------------------------
  // User persistence
  // ---------------------------------------------------------------------------

  private User getOrCreateUser(OAuth2UserProfile profile) {
    String normalizedEmail = profile.email().toLowerCase();
    return userRepository
        .findByUsername(normalizedEmail)
        .map(existing -> updateExistingUser(existing, profile))
        .orElseGet(() -> createNewUser(normalizedEmail, profile));
  }

  private User updateExistingUser(User user, OAuth2UserProfile profile) {
    if (profile.avatarUrl() != null) {
      user.setAvatarUrl(profile.avatarUrl());
    }
    user.setLastLoginAt(OffsetDateTime.now());
    return userRepository.save(user);
  }

  private User createNewUser(String email, OAuth2UserProfile profile) {
    String displayName = buildDisplayName(profile, email);
    User user =
        User.builder()
            .username(email)
            .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
            .displayName(displayName)
            .avatarUrl(profile.avatarUrl())
            .role(Role.QC_MEMBER)
            .status(UserStatus.ACTIVE)
            .lastLoginAt(OffsetDateTime.now())
            .build();

    User saved = userRepository.save(user);
    log.info("Created OAuth2 user {} with email {}", saved.getPublicId(), email);
    return saved;
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private String buildDisplayName(OAuth2UserProfile profile, String email) {
    String firstName = profile.firstName() != null ? profile.firstName().trim() : "";
    String lastName = profile.lastName() != null ? profile.lastName().trim() : "";
    String fullName = (firstName + " " + lastName).trim();

    if (fullName.isBlank()) {
      fullName = email.split("@")[0];
    }

    return fullName.length() > MAX_DISPLAY_NAME_LENGTH
        ? fullName.substring(0, MAX_DISPLAY_NAME_LENGTH)
        : fullName;
  }

  private ResponseCookie buildRefreshCookie(String refreshToken) {
    return ResponseCookie.from("refresh_token", refreshToken)
        .httpOnly(true)
        .secure(cookieSecure)
        .path("/api/v1/auth/refresh-token")
        .maxAge(refreshExpirationMinutes * 60L)
        .sameSite(sameSite)
        .build();
  }
}
