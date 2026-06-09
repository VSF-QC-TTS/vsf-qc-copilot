package me.nghlong3004.vqc.api.oauth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.oauth.profile.OAuth2UserProfile;
import me.nghlong3004.vqc.api.oauth.profile.OAuth2UserProfileService;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private static final int MAX_FIRST_NAME_LENGTH = 35;
  private static final int MAX_LAST_NAME_LENGTH = 20;

  @Value("${vqc.client.base-url}")
  private String webBaseUrl;

  @Value("${vqc.security.refresh-expiration}")
  private int refreshExpirationMinutes;

  @Value("${vqc.security.cookie.secure}")
  private boolean cookieSecure;

  @Value("${vqc.security.cookie.same-site:Lax}")
  private String sameSite;

  private final OAuth2UserProfileService oAuth2UserProfileService;

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

    // User user = getOrCreateUser(profile);
    String refreshToken = UUID.randomUUID().toString();
    // refreshTokenStore.store(user.getEmail(), refreshToken, refreshExpirationMinutes);

    response.addHeader(HttpHeaders.SET_COOKIE, getRefreshCookie(refreshToken).toString());

    var session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }

    log.info("OAuth2 login success for user: {}", email);
    response.sendRedirect(webBaseUrl + "/dashboard");
  }

  //  private User getOrCreateUser(OAuth2UserProfile profile) {
  //    String normalizedEmail = profile.email().toLowerCase();
  //    return userRepository
  //        .findByEmail(normalizedEmail)
  //        .map(existing -> linkOAuthProvider(existing, profile))
  //        .orElseGet(() -> createOAuthUser(normalizedEmail, profile));
  //  }
  //
  //  private User linkOAuthProvider(User user, OAuth2UserProfile profile) {
  //    if (user.getAuthProvider() != AuthProvider.LOCAL) {
  //      return user;
  //    }
  //
  //    user.setAuthProvider(profile.provider());
  //    if (profile.avatarUrl() != null) {
  //      user.setAvatarUrl(profile.avatarUrl());
  //    }
  //    return userRepository.save(user);
  //  }
  //
  //  private User createOAuthUser(String email, OAuth2UserProfile profile) {
  //    String firstName = sanitizeName(profile.firstName(), MAX_FIRST_NAME_LENGTH);
  //    String lastName = sanitizeName(profile.lastName(), MAX_LAST_NAME_LENGTH);
  //    boolean incomplete =
  //        isNameInvalid(profile.firstName(), MAX_FIRST_NAME_LENGTH)
  //            || isNameInvalid(profile.lastName(), MAX_LAST_NAME_LENGTH);
  //    if (firstName.isBlank()) {
  //      firstName = truncate(email.split("@")[0], MAX_FIRST_NAME_LENGTH);
  //    }
  //
  //    User user = User.builder().build();
  //    User saved = userRepository.save(user);
  //    return saved;
  //  }

  /** True if the raw OAuth value is null, blank, or exceeds DB column limit. */
  private boolean isNameInvalid(String raw, int maxLength) {
    return raw == null || raw.isBlank() || raw.trim().length() > maxLength;
  }

  private String sanitizeName(String value, int maxLength) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return truncate(value.trim(), maxLength);
  }

  private String truncate(String value, int maxLength) {
    return value.length() > maxLength ? value.substring(0, maxLength) : value;
  }

  private String buildUsername(String email) {
    return email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 4);
  }

  private ResponseCookie getRefreshCookie(String refreshToken) {
    return ResponseCookie.from("refresh_token", refreshToken)
        .httpOnly(true)
        .secure(cookieSecure)
        .path("/api/v1/auth/refresh-token")
        .maxAge(refreshExpirationMinutes * 60L)
        .sameSite(sameSite)
        .build();
  }
}
