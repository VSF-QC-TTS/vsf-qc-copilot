package me.nghlong3004.vqc.api.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.auth.cookie.AuthCookieFactory;
import me.nghlong3004.vqc.api.auth.request.ForgotPasswordRequest;
import me.nghlong3004.vqc.api.auth.request.LoginRequest;
import me.nghlong3004.vqc.api.auth.request.RegisterRequest;
import me.nghlong3004.vqc.api.auth.request.ResetPasswordRequest;
import me.nghlong3004.vqc.api.auth.request.VerifyEmailRequest;
import me.nghlong3004.vqc.api.auth.response.LoginResponse;
import me.nghlong3004.vqc.api.auth.response.LoginResult;
import me.nghlong3004.vqc.api.auth.service.AuthService;
import me.nghlong3004.vqc.api.exception.GlobalException;
import me.nghlong3004.vqc.api.user.enums.Role;
import me.nghlong3004.vqc.api.user.enums.UserStatus;
import me.nghlong3004.vqc.api.user.response.UserResponse;
import me.nghlong3004.vqc.api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@WebMvcTest(
    controllers = AuthController.class,
    excludeAutoConfiguration = {
      OAuth2ClientAutoConfiguration.class,
      OAuth2ClientWebSecurityAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalException.class, AuthControllerTest.MockBeans.class})
class AuthControllerTest {

  private static final String AUTH_PATH = "/api/v1/auth";

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void resetTestDoubles() {
    RecordingAuthService.reset();
    RecordingUserService.reset();
    RecordingAuthCookieFactory.reset();
  }

  @Test
  void loginReturnsAccessTokenBodyAndRefreshTokenCookie() throws Exception {
    var user = activeUser();
    var response = new LoginResponse("access-token", "Bearer", 900, user);
    RecordingAuthService.loginResult = new LoginResult(response, "refresh-token", 604800);
    RecordingAuthCookieFactory.cookie =
        ResponseCookie.from("refresh_token", "refresh-token")
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path("/")
            .maxAge(604800)
            .build();

    mockMvc
        .perform(
            post(AUTH_PATH + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "qc.demo@example.com",
                      "password": "password123"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=refresh-token")))
        .andExpect(jsonPath("$.accessToken").value("access-token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresInSeconds").value(900))
        .andExpect(jsonPath("$.user.email").value("qc.demo@example.com"))
        .andExpect(jsonPath("$.user.role").value("QC_MEMBER"));

    assertThat(RecordingAuthService.loginRequest.email()).isEqualTo("qc.demo@example.com");
    assertThat(RecordingAuthCookieFactory.refreshToken).isEqualTo("refresh-token");
    assertThat(RecordingAuthCookieFactory.maxAgeSeconds).isEqualTo(604800);
  }

  @Test
  void refreshTokenReturnsAccessTokenBodyAndRotatesRefreshTokenCookie() throws Exception {
    var user = activeUser();
    var response = new LoginResponse("new-access-token", "Bearer", 900, user);
    RecordingAuthService.refreshTokenResult = new LoginResult(response, "new-refresh-token", 604800);
    RecordingAuthCookieFactory.cookie =
        ResponseCookie.from("refresh_token", "new-refresh-token")
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path("/")
            .maxAge(604800)
            .build();

    mockMvc
        .perform(post(AUTH_PATH + "/refresh-token").cookie(new Cookie("refresh_token", "old-refresh-token")))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=new-refresh-token")))
        .andExpect(jsonPath("$.accessToken").value("new-access-token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresInSeconds").value(900))
        .andExpect(jsonPath("$.user.email").value("qc.demo@example.com"));

    assertThat(RecordingAuthService.refreshToken).isEqualTo("old-refresh-token");
    assertThat(RecordingAuthCookieFactory.refreshToken).isEqualTo("new-refresh-token");
    assertThat(RecordingAuthCookieFactory.maxAgeSeconds).isEqualTo(604800);
  }

  @Test
  void logoutClearsRefreshTokenCookie() throws Exception {
    RecordingAuthCookieFactory.clearCookie =
        ResponseCookie.from("refresh_token", "")
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(0)
            .build();

    mockMvc
        .perform(post(AUTH_PATH + "/logout"))
        .andExpect(status().isNoContent())
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

    assertThat(RecordingAuthCookieFactory.clearCookieCalled).isTrue();
  }


  @Test
  void verifyEmailReturnsActivatedUser() throws Exception {
    RecordingAuthService.verifyEmailResponse = activeUser();

    mockMvc
        .perform(
            post(AUTH_PATH + "/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "token": "raw-email-token"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("qc.demo@example.com"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    assertThat(RecordingAuthService.verifyEmailRequest.token()).isEqualTo("raw-email-token");
  }

  @Test
  void forgotPasswordReturnsNoContent() throws Exception {
    mockMvc
        .perform(
            post(AUTH_PATH + "/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "qc.demo@example.com"
                    }
                    """))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    assertThat(RecordingAuthService.forgotPasswordRequest.email()).isEqualTo("qc.demo@example.com");
  }

  @Test
  void resetPasswordReturnsNoContent() throws Exception {
    mockMvc
        .perform(
            post(AUTH_PATH + "/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "token": "raw-reset-token",
                      "newPassword": "newPassword123"
                    }
                    """))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    assertThat(RecordingAuthService.resetPasswordRequest.token()).isEqualTo("raw-reset-token");
    assertThat(RecordingAuthService.resetPasswordRequest.newPassword()).isEqualTo("newPassword123");
  }

  @Test
  void registerReturnsCreatedUser() throws Exception {
    var user = pendingUser();
    RecordingUserService.registerResponse = user;

    mockMvc
        .perform(
            post(AUTH_PATH + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "qc.demo@example.com",
                      "password": "password123",
                      "displayName": "QC Demo"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("qc.demo@example.com"))
        .andExpect(jsonPath("$.displayName").value("QC Demo"))
        .andExpect(jsonPath("$.role").value("QC_MEMBER"))
        .andExpect(jsonPath("$.status").value("PENDING_EMAIL_VERIFICATION"));

    assertThat(RecordingUserService.registerRequest.email()).isEqualTo("qc.demo@example.com");
  }

  @Test
  void invalidRegisterReturnsProblemDetailsValidationError() throws Exception {
    mockMvc
        .perform(
            post(AUTH_PATH + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "not-an-email",
                      "password": "short"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("https://vqc.nghlong3004.me/errors/validation-error"))
        .andExpect(jsonPath("$.title").value("Validation Error"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.instance").value(AUTH_PATH + "/register"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[*].field").value(hasItems("email", "password")))
        .andExpect(
            jsonPath("$.errors[*].message")
                .value(
                    hasItems(
                        "Email must be a valid email address.",
                        "Password must be between 8 and 72 characters.")));
  }

  private UserResponse activeUser() {
    return new UserResponse(
        UUID.fromString("7b7b7d42-5f42-4c5a-9281-8d1d36f6f59d"),
        "qc.demo@example.com",
        "QC Demo",
        null,
        Role.QC_MEMBER,
        UserStatus.ACTIVE,
        OffsetDateTime.parse("2026-06-09T10:00:00Z"));
  }

  private UserResponse pendingUser() {
    return new UserResponse(
        UUID.fromString("7b7b7d42-5f42-4c5a-9281-8d1d36f6f59d"),
        "qc.demo@example.com",
        "QC Demo",
        null,
        Role.QC_MEMBER,
        UserStatus.PENDING_EMAIL_VERIFICATION,
        null);
  }

  @TestConfiguration
  static class MockBeans {

    @Bean
    RecordingAuthService authService() {
      return new RecordingAuthService();
    }

    @Bean
    RecordingUserService userService() {
      return new RecordingUserService();
    }

    @Bean
    RecordingAuthCookieFactory authCookieFactory() {
      return new RecordingAuthCookieFactory();
    }
  }

  static class RecordingAuthService implements AuthService {
    private static LoginRequest loginRequest;
    private static LoginResult loginResult;
    private static String refreshToken;
    private static LoginResult refreshTokenResult;
    private static VerifyEmailRequest verifyEmailRequest;
    private static UserResponse verifyEmailResponse;
    private static ForgotPasswordRequest forgotPasswordRequest;
    private static ResetPasswordRequest resetPasswordRequest;

    @Override
    public LoginResult login(LoginRequest request) {
      loginRequest = request;
      return loginResult;
    }

    @Override
    public LoginResult refreshToken(String refreshToken) {
      this.refreshToken = refreshToken;
      return refreshTokenResult;
    }

    @Override
    public UserResponse verifyEmail(VerifyEmailRequest request) {
      verifyEmailRequest = request;
      return verifyEmailResponse;
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
      forgotPasswordRequest = request;
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
      resetPasswordRequest = request;
    }

    private static void reset() {
      loginRequest = null;
      loginResult = null;
      refreshToken = null;
      refreshTokenResult = null;
      verifyEmailRequest = null;
      verifyEmailResponse = null;
      forgotPasswordRequest = null;
      resetPasswordRequest = null;
    }
  }

  static class RecordingUserService implements UserService {
    private static RegisterRequest registerRequest;
    private static UserResponse registerResponse;

    @Override
    public UserResponse register(RegisterRequest request) {
      registerRequest = request;
      return registerResponse;
    }

    @Override
    public UserResponse getCurrentUser(String username) {
      throw new AssertionError("Get current user should not be called");
    }

    private static void reset() {
      registerRequest = null;
      registerResponse = null;
    }
  }

  static class RecordingAuthCookieFactory implements AuthCookieFactory {
    private static String refreshToken;
    private static long maxAgeSeconds;
    private static ResponseCookie cookie;
    private static boolean clearCookieCalled;
    private static ResponseCookie clearCookie;

    @Override
    public ResponseCookie refreshTokenCookie(String refreshToken, long maxAgeSeconds) {
      this.refreshToken = refreshToken;
      this.maxAgeSeconds = maxAgeSeconds;
      return cookie;
    }

    @Override
    public ResponseCookie clearRefreshTokenCookie() {
      clearCookieCalled = true;
      return clearCookie;
    }

    private static void reset() {
      refreshToken = null;
      maxAgeSeconds = 0;
      cookie = null;
      clearCookieCalled = false;
      clearCookie = null;
    }
  }
}
