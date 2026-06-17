package me.nghlong3004.vqc.api.oauth.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import me.nghlong3004.vqc.api.auth.token.JwtTokenService;
import me.nghlong3004.vqc.api.oauth.AuthProvider;
import me.nghlong3004.vqc.api.oauth.profile.OAuth2UserProfile;
import me.nghlong3004.vqc.api.oauth.profile.OAuth2UserProfileService;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.enums.Role;
import me.nghlong3004.vqc.api.user.enums.UserStatus;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class OAuth2LoginSuccessHandlerTest {

  private static final String WEB_BASE_URL = "http://localhost:5173";
  private static final String ACCESS_TOKEN = "access-jwt-token";
  private static final String REFRESH_TOKEN = "refresh-jwt-token";
  private static final String HASHED_PASSWORD = "$2a$10$hashed";

  // ---------------------------------------------------------------------------
  // No email → redirect to error
  // ---------------------------------------------------------------------------

  @Test
  void redirectsWithErrorWhenProviderHasNoEmail() throws Exception {
    var profile = new OAuth2UserProfile(AuthProvider.GITHUB, null, "Long", "Nguyen", null);
    var handler = buildHandler(profile, Optional.empty());
    var response = new MockHttpServletResponse();

    handler.onAuthenticationSuccess(
        new MockHttpServletRequest(), response, authToken());

    assertThat(response.getRedirectedUrl())
        .isEqualTo(WEB_BASE_URL + "/login?error=oauth_no_email");
    assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
  }

  // ---------------------------------------------------------------------------
  // New user → created with correct fields
  // ---------------------------------------------------------------------------

  @Test
  void createsNewUserOnFirstOAuthLogin() throws Exception {
    var profile = new OAuth2UserProfile(
        AuthProvider.GOOGLE,
        "qc.demo@example.com",
        "Long",
        "Nguyen",
        "https://cdn.example.com/avatar.png");

    var userRepo = mock(UserRepository.class);
    when(userRepo.findByUsername("qc.demo@example.com")).thenReturn(Optional.empty());
    when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    var handler = buildHandler(profile, userRepo);
    var request = new MockHttpServletRequest();
    var session = new MockHttpSession();
    request.setSession(session);
    var response = new MockHttpServletResponse();

    handler.onAuthenticationSuccess(request, response, authToken());

    // Verify user was created with correct fields
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepo).save(userCaptor.capture());
    User savedUser = userCaptor.getValue();

    assertThat(savedUser.getUsername()).isEqualTo("qc.demo@example.com");
    assertThat(savedUser.getDisplayName()).isEqualTo("Long Nguyen");
    assertThat(savedUser.getAvatarUrl()).isEqualTo("https://cdn.example.com/avatar.png");
    assertThat(savedUser.getRole()).isEqualTo(Role.QC_MEMBER);
    assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(savedUser.getPasswordHash()).isEqualTo(HASHED_PASSWORD);
    assertThat(savedUser.getLastLoginAt()).isNotNull();

    // Verify redirect + cookie + session
    assertThat(response.getRedirectedUrl()).isEqualTo(WEB_BASE_URL + "/dashboard");
    assertThat(session.isInvalid()).isTrue();
    assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
        .contains("refresh_token=" + REFRESH_TOKEN);
  }

  // ---------------------------------------------------------------------------
  // New user with missing names → uses email prefix as displayName
  // ---------------------------------------------------------------------------

  @Test
  void usesEmailPrefixAsDisplayNameWhenNamesAreMissing() throws Exception {
    var profile = new OAuth2UserProfile(
        AuthProvider.GITHUB, "john.doe@example.com", null, null, null);

    var userRepo = mock(UserRepository.class);
    when(userRepo.findByUsername("john.doe@example.com")).thenReturn(Optional.empty());
    when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    var handler = buildHandler(profile, userRepo);
    var response = new MockHttpServletResponse();

    handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authToken());

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepo).save(userCaptor.capture());
    assertThat(userCaptor.getValue().getDisplayName()).isEqualTo("john.doe");
  }

  // ---------------------------------------------------------------------------
  // Existing user → avatar + lastLoginAt updated
  // ---------------------------------------------------------------------------

  @Test
  void updatesExistingUserOnReLogin() throws Exception {
    var profile = new OAuth2UserProfile(
        AuthProvider.GOOGLE,
        "qc.demo@example.com",
        "Long",
        "Nguyen",
        "https://cdn.example.com/new-avatar.png");

    User existingUser = User.builder()
        .username("qc.demo@example.com")
        .passwordHash(HASHED_PASSWORD)
        .displayName("Long Nguyen")
        .avatarUrl("https://cdn.example.com/old-avatar.png")
        .role(Role.QC_MEMBER)
        .status(UserStatus.ACTIVE)
        .build();

    var userRepo = mock(UserRepository.class);
    when(userRepo.findByUsername("qc.demo@example.com")).thenReturn(Optional.of(existingUser));
    when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    var handler = buildHandler(profile, userRepo);
    var response = new MockHttpServletResponse();

    handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authToken());

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepo).save(userCaptor.capture());
    User updatedUser = userCaptor.getValue();

    assertThat(updatedUser.getAvatarUrl()).isEqualTo("https://cdn.example.com/new-avatar.png");
    assertThat(updatedUser.getLastLoginAt()).isNotNull();
    assertThat(response.getRedirectedUrl()).isEqualTo(WEB_BASE_URL + "/dashboard");
  }

  // ---------------------------------------------------------------------------
  // Cookie contains proper attributes
  // ---------------------------------------------------------------------------

  @Test
  void refreshCookieHasCorrectAttributes() throws Exception {
    var profile = new OAuth2UserProfile(
        AuthProvider.GOOGLE, "qc.demo@example.com", "Long", "Nguyen", null);

    var handler = buildHandler(profile, Optional.empty());
    var request = new MockHttpServletRequest();
    var session = new MockHttpSession();
    request.setSession(session);
    var response = new MockHttpServletResponse();

    handler.onAuthenticationSuccess(request, response, authToken());

    assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
        .contains("refresh_token=" + REFRESH_TOKEN)
        .contains("HttpOnly")
        .contains("Secure")
        .contains("Path=/api/v1/auth/refresh-token")
        .contains("Max-Age=1800")
        .contains("SameSite=Strict");
    assertThat(session.isInvalid()).isTrue();
  }

  // ---------------------------------------------------------------------------
  // Test helpers
  // ---------------------------------------------------------------------------

  /** Builds handler with a stub UserRepository that returns the given Optional. */
  private OAuth2LoginSuccessHandler buildHandler(
      OAuth2UserProfile profile, Optional<User> existingUser) {
    var userRepo = mock(UserRepository.class);
    when(userRepo.findByUsername(any())).thenReturn(existingUser);
    when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    return buildHandler(profile, userRepo);
  }

  /** Builds handler with a real (mock) UserRepository for fine-grained control. */
  private OAuth2LoginSuccessHandler buildHandler(
      OAuth2UserProfile profile, UserRepository userRepo) {
    var jwtTokenService = mock(JwtTokenService.class);
    when(jwtTokenService.createAccessToken(any())).thenReturn(ACCESS_TOKEN);
    when(jwtTokenService.createRefreshToken(any())).thenReturn(REFRESH_TOKEN);

    var passwordEncoder = mock(PasswordEncoder.class);
    when(passwordEncoder.encode(any())).thenReturn(HASHED_PASSWORD);

    var handler = new OAuth2LoginSuccessHandler(
        new StubProfileService(profile), userRepo, jwtTokenService, passwordEncoder);

    ReflectionTestUtils.setField(handler, "webBaseUrl", WEB_BASE_URL);
    ReflectionTestUtils.setField(handler, "refreshExpirationMinutes", 30);
    ReflectionTestUtils.setField(handler, "cookieSecure", true);
    ReflectionTestUtils.setField(handler, "sameSite", "Strict");
    return handler;
  }

  private static Authentication authToken() {
    return new TestingAuthenticationToken("user", null);
  }

  private static class StubProfileService extends OAuth2UserProfileService {
    private final OAuth2UserProfile profile;

    private StubProfileService(OAuth2UserProfile profile) {
      super(List.of());
      this.profile = profile;
    }

    @Override
    public OAuth2UserProfile extract(Authentication authentication) {
      return profile;
    }
  }
}
