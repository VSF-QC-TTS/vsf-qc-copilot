package me.nghlong3004.vqc.api.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.auth.request.ForgotPasswordRequest;
import me.nghlong3004.vqc.api.auth.request.LoginRequest;
import me.nghlong3004.vqc.api.auth.service.EmailVerificationService;
import me.nghlong3004.vqc.api.auth.service.PasswordResetService;
import me.nghlong3004.vqc.api.auth.token.JwtTokenService;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.mail.model.MailRequest;
import me.nghlong3004.vqc.api.mail.model.MailType;
import me.nghlong3004.vqc.api.mail.service.MailService;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.enums.Role;
import me.nghlong3004.vqc.api.user.enums.UserStatus;
import me.nghlong3004.vqc.api.user.mapper.UserMapper;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import me.nghlong3004.vqc.api.user.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.jwt.BadJwtException;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
class AuthServiceImplTest {

  @Test
  void loginNormalizesEmailUpdatesLastLoginAndReturnsRefreshTokenOnlyInResult() {
    User user = new User();
    user.setUsername("qc.demo@example.com");
    user.setDisplayName("QC Demo");
    user.setRole(Role.QC_MEMBER);
    user.setStatus(UserStatus.ACTIVE);
    AtomicReference<String> authenticatedEmail = new AtomicReference<>();
    AtomicReference<User> savedUser = new AtomicReference<>();
    UserResponse userResponse =
        new UserResponse(null, "qc.demo@example.com", "QC Demo", null, Role.QC_MEMBER, UserStatus.ACTIVE, null);
    AuthServiceImpl authService =
        new AuthServiceImpl(
            authenticationManager(authenticatedEmail),
            repository(user, savedUser),
            mapper(userResponse),
            tokenService(),
            ignoredEmailVerificationService(),
            ignoredPasswordResetService(),
            ignoredMailService());

    var result = authService.login(new LoginRequest("  QC.Demo@Example.COM  ", "password123"));

    assertThat(authenticatedEmail).hasValue("qc.demo@example.com");
    assertThat(savedUser.get().getLastLoginAt()).isNotNull();
    assertThat(result.response().accessToken()).isEqualTo("access-token");
    assertThat(result.response().tokenType()).isEqualTo("Bearer");
    assertThat(result.response().expiresInSeconds()).isEqualTo(900);
    assertThat(result.response().user()).isSameAs(userResponse);
    assertThat(result.refreshToken()).isEqualTo("refresh-token");
    assertThat(result.refreshTokenMaxAgeSeconds()).isEqualTo(604800);
  }

  @Test
  void forgotPasswordDoesNotExposeAccountExistenceAndSendsResetLinkWhenUserExists() {
    User user = new User();
    user.setUsername("qc.demo@example.com");
    user.setDisplayName("QC Demo");
    AtomicReference<User> savedUser = new AtomicReference<>();
    RecordingPasswordResetService passwordResetService = new RecordingPasswordResetService();
    RecordingMailService mailService = new RecordingMailService();
    AuthServiceImpl authService =
        new AuthServiceImpl(
            authenticationManager(new AtomicReference<>()),
            repository(user, savedUser),
            mapper(null),
            tokenService(),
            ignoredEmailVerificationService(),
            passwordResetService,
            mailService);

    authService.forgotPassword(new ForgotPasswordRequest(" QC.Demo@Example.COM "));

    assertThat(passwordResetService.user).isSameAs(user);
    assertThat(mailService.type).isEqualTo(MailType.PASSWORD_RESET);
    assertThat(mailService.request.to()).isEqualTo("qc.demo@example.com");
    assertThat(mailService.request.displayName()).isEqualTo("QC Demo");
    assertThat(mailService.request.actionUrl())
        .isEqualTo("http://localhost:5173/reset-password?token=reset-token");
  }

  @Test
  void refreshTokenReturnsNewTokenPairFromRefreshCookie() {
    User user = new User();
    user.setUsername("qc.demo@example.com");
    user.setDisplayName("QC Demo");
    user.setRole(Role.QC_MEMBER);
    user.setStatus(UserStatus.ACTIVE);
    AtomicReference<String> authenticatedEmail = new AtomicReference<>();
    AtomicReference<User> savedUser = new AtomicReference<>();
    UserResponse userResponse =
        new UserResponse(null, "qc.demo@example.com", "QC Demo", null, Role.QC_MEMBER, UserStatus.ACTIVE, null);
    AuthServiceImpl authService =
        new AuthServiceImpl(
            authenticationManager(authenticatedEmail),
            repository(user, savedUser),
            mapper(userResponse),
            tokenService(" QC.Demo@Example.COM "),
            ignoredEmailVerificationService(),
            ignoredPasswordResetService(),
            ignoredMailService());

    var result = authService.refreshToken("old-refresh-token");

    assertThat(authenticatedEmail.get()).isNull();
    assertThat(result.response().accessToken()).isEqualTo("access-token");
    assertThat(result.response().tokenType()).isEqualTo("Bearer");
    assertThat(result.response().expiresInSeconds()).isEqualTo(900);
    assertThat(result.response().user()).isSameAs(userResponse);
    assertThat(result.refreshToken()).isEqualTo("refresh-token");
    assertThat(result.refreshTokenMaxAgeSeconds()).isEqualTo(604800);
  }

  @Test
  void refreshTokenRejectsExpiredRefreshJwt() {
    AuthServiceImpl authService =
        new AuthServiceImpl(
            authenticationManager(new AtomicReference<>()),
            repository(new User(), new AtomicReference<>()),
            mapper(null),
            expiredRefreshTokenService(),
            ignoredEmailVerificationService(),
            ignoredPasswordResetService(),
            ignoredMailService());

    assertThatThrownBy(() -> authService.refreshToken("expired-refresh-token"))
        .isInstanceOfSatisfying(
            ResourceException.class,
            exception ->
                assertThat(exception.getResponse().code())
                    .isEqualTo(ErrorCode.REFRESH_TOKEN_EXPIRED.getCode()));
  }

  private AuthenticationManager authenticationManager(AtomicReference<String> authenticatedEmail) {
    return authentication -> {
      authenticatedEmail.set(authentication.getName());
      return authentication;
    };
  }

  private UserRepository repository(User user, AtomicReference<User> savedUser) {
    return (UserRepository)
        Proxy.newProxyInstance(
            UserRepository.class.getClassLoader(),
            new Class<?>[] {UserRepository.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "findByUsername" -> Optional.of(user);
                case "save" -> {
                  savedUser.set((User) args[0]);
                  yield args[0];
                }
                case "toString" -> "UserRepositoryTestDouble";
                default -> throw new UnsupportedOperationException(method.getName());
              };
            });
  }

  private UserMapper mapper(UserResponse response) {
    return user -> response;
  }

  private JwtTokenService tokenService() {
    return tokenService("qc.demo@example.com");
  }

  private JwtTokenService tokenService(String refreshTokenSubject) {
    return new JwtTokenService() {
      @Override
      public String createAccessToken(User user) {
        return "access-token";
      }

      @Override
      public String createRefreshToken(User user) {
        return "refresh-token";
      }

      @Override
      public String readRefreshTokenSubject(String refreshToken) {
        return refreshTokenSubject;
      }

      @Override
      public long accessTokenExpiresInSeconds() {
        return 900;
      }

      @Override
      public long refreshTokenExpiresInSeconds() {
        return 604800;
      }
    };
  }

  private JwtTokenService expiredRefreshTokenService() {
    return new JwtTokenService() {
      @Override
      public String createAccessToken(User user) {
        throw new AssertionError("Access token should not be created");
      }

      @Override
      public String createRefreshToken(User user) {
        throw new AssertionError("Refresh token should not be created");
      }

      @Override
      public String readRefreshTokenSubject(String refreshToken) {
        throw new BadJwtException("Jwt expired at 2026-06-10T00:00:00Z");
      }

      @Override
      public long accessTokenExpiresInSeconds() {
        return 900;
      }

      @Override
      public long refreshTokenExpiresInSeconds() {
        return 604800;
      }
    };
  }

  private EmailVerificationService ignoredEmailVerificationService() {
    return new EmailVerificationService() {
      @Override
      public String createVerificationToken(User user) {
        throw new AssertionError("Create verification token should not be called");
      }

      @Override
      public User verifyEmail(String rawToken) {
        throw new AssertionError("Verify email should not be called");
      }
    };
  }

  private PasswordResetService ignoredPasswordResetService() {
    return new PasswordResetService() {
      @Override
      public String createResetToken(User user) {
        throw new AssertionError("Create reset token should not be called");
      }

      @Override
      public void resetPassword(String rawToken, String newPassword) {
        throw new AssertionError("Reset password should not be called");
      }
    };
  }

  private MailService ignoredMailService() {
    return (type, request) -> {
      throw new AssertionError("Mail service should not be called");
    };
  }

  private static class RecordingPasswordResetService implements PasswordResetService {
    private User user;

    @Override
    public String createResetToken(User user) {
      this.user = user;
      return "reset-token";
    }

    @Override
    public void resetPassword(String rawToken, String newPassword) {
      throw new AssertionError("Reset password should not be called");
    }
  }

  private static class RecordingMailService implements MailService {
    private MailType type;
    private MailRequest request;

    @Override
    public void send(MailType type, MailRequest request) {
      this.type = type;
      this.request = request;
    }
  }
}
