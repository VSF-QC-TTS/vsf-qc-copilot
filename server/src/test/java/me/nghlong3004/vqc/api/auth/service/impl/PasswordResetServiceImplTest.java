package me.nghlong3004.vqc.api.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.auth.entity.PasswordResetToken;
import me.nghlong3004.vqc.api.auth.repository.PasswordResetTokenRepository;
import me.nghlong3004.vqc.api.auth.token.OpaqueTokenService;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
class PasswordResetServiceImplTest {

  @Test
  void resetPasswordStoresTokenHashAndUpdatesPassword() {
    User user = new User();
    user.setUsername("qc.demo@example.com");
    user.setPasswordHash("old-password");
    AtomicReference<PasswordResetToken> savedToken = new AtomicReference<>();
    AtomicReference<User> savedUser = new AtomicReference<>();
    PasswordResetServiceImpl service =
        new PasswordResetServiceImpl(
            tokenRepository(savedToken),
            userRepository(savedUser),
            passwordEncoder(),
            opaqueTokenService());

    String rawToken = service.createResetToken(user);

    assertThat(rawToken).isEqualTo("raw-reset-token");
    assertThat(savedToken.get().getTokenHash()).isEqualTo("b".repeat(64));
    assertThat(savedToken.get().getUser()).isSameAs(user);

    service.resetPassword(rawToken, "newPassword123");

    assertThat(savedUser.get().getPasswordHash()).isEqualTo("encoded-newPassword123");
    assertThat(savedToken.get().getUsedAt()).isNotNull();
  }

  private PasswordResetTokenRepository tokenRepository(
      AtomicReference<PasswordResetToken> savedToken) {
    return (PasswordResetTokenRepository)
        Proxy.newProxyInstance(
            PasswordResetTokenRepository.class.getClassLoader(),
            new Class<?>[] {PasswordResetTokenRepository.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "save" -> {
                  savedToken.set((PasswordResetToken) args[0]);
                  yield args[0];
                }
                case "findByTokenHash" -> {
                  PasswordResetToken token = savedToken.get();
                  if (token != null && token.getTokenHash().equals(args[0])) {
                    yield Optional.of(token);
                  }
                  yield Optional.empty();
                }
                case "toString" -> "PasswordResetTokenRepositoryTestDouble";
                default -> throw new UnsupportedOperationException(method.getName());
              };
            });
  }

  private UserRepository userRepository(AtomicReference<User> savedUser) {
    return (UserRepository)
        Proxy.newProxyInstance(
            UserRepository.class.getClassLoader(),
            new Class<?>[] {UserRepository.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "save" -> {
                  savedUser.set((User) args[0]);
                  yield args[0];
                }
                case "toString" -> "UserRepositoryTestDouble";
                default -> throw new UnsupportedOperationException(method.getName());
              };
            });
  }

  private PasswordEncoder passwordEncoder() {
    return new PasswordEncoder() {
      @Override
      public String encode(CharSequence rawPassword) {
        return "encoded-" + rawPassword;
      }

      @Override
      public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return false;
      }
    };
  }

  private OpaqueTokenService opaqueTokenService() {
    return new OpaqueTokenService() {
      @Override
      public String generateRawToken() {
        return "raw-reset-token";
      }

      @Override
      public String hash(String rawToken) {
        return "b".repeat(64);
      }
    };
  }
}
