package me.nghlong3004.vqc.api.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.auth.request.LoginRequest;
import me.nghlong3004.vqc.api.auth.service.EmailVerificationService;
import me.nghlong3004.vqc.api.auth.token.JwtTokenService;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.enums.Role;
import me.nghlong3004.vqc.api.user.enums.UserStatus;
import me.nghlong3004.vqc.api.user.mapper.UserMapper;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import me.nghlong3004.vqc.api.user.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;

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
        new UserResponse(null, "qc.demo@example.com", "QC Demo", Role.QC_MEMBER, UserStatus.ACTIVE, null);
    AuthServiceImpl authService =
        new AuthServiceImpl(
            authenticationManager(authenticatedEmail),
            repository(user, savedUser),
            mapper(userResponse),
            tokenService(),
            ignoredEmailVerificationService());

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
}
