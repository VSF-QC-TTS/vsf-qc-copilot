package me.nghlong3004.vqc.api.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.auth.request.RegisterRequest;
import me.nghlong3004.vqc.api.auth.service.EmailVerificationService;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
class UserServiceImplTest {

  @Test
  void registerNormalizesEmailHashesPasswordAndDefaultsDisplayName() {
    RegisterRequest request =
        new RegisterRequest("  QC.Demo@Example.COM  ", "password123", "   ");
    UserResponse mappedResponse =
        new UserResponse(null, "qc.demo@example.com", "qc.demo", Role.QC_MEMBER, null, null);
    AtomicReference<User> savedUser = new AtomicReference<>();
    RecordingMailService mailService = new RecordingMailService();
    RecordingEmailVerificationService emailVerificationService =
        new RecordingEmailVerificationService("raw-token");
    UserServiceImpl userService =
        new UserServiceImpl(
            repository(false, savedUser, null),
            user -> mappedResponse,
            passwordEncoder("encoded-password"),
            mailService,
            emailVerificationService);

    UserResponse response = userService.register(request);

    assertThat(savedUser.get().getUsername()).isEqualTo("qc.demo@example.com");
    assertThat(savedUser.get().getPasswordHash()).isEqualTo("encoded-password");
    assertThat(savedUser.get().getDisplayName()).isEqualTo("qc.demo");
    assertThat(savedUser.get().getRole()).isEqualTo(Role.QC_MEMBER);
    assertThat(savedUser.get().getStatus()).isEqualTo(UserStatus.PENDING_EMAIL_VERIFICATION);
    assertThat(emailVerificationService.user).isSameAs(savedUser.get());
    assertThat(mailService.type).isEqualTo(MailType.EMAIL_VERIFICATION);
    assertThat(mailService.request.to()).isEqualTo("qc.demo@example.com");
    assertThat(mailService.request.displayName()).isEqualTo("qc.demo");
    assertThat(mailService.request.verificationUrl())
        .isEqualTo("http://localhost:5173/verify-email?token=raw-token");
    assertThat(response).isSameAs(mappedResponse);
  }

  @Test
  void registerRejectsExistingEmailBeforeSaving() {
    RegisterRequest request = new RegisterRequest("qc.demo@example.com", "password123", "QC Demo");
    AtomicReference<User> savedUser = new AtomicReference<>();
    UserServiceImpl userService =
        new UserServiceImpl(
            repository(true, savedUser, null),
            ignoredMapper(),
            passwordEncoder("encoded-password"),
            ignoredMailService(),
            ignoredEmailVerificationService());

    assertThatThrownBy(() -> userService.register(request))
        .isInstanceOf(ResourceException.class)
        .extracting("response.code")
        .isEqualTo("EMAIL_ALREADY_EXISTS");

    assertThat(savedUser).hasValue(null);
  }

  @Test
  void registerMapsUniqueConstraintRaceToEmailAlreadyExists() {
    RegisterRequest request = new RegisterRequest("qc.demo@example.com", "password123", "QC Demo");
    UserServiceImpl userService =
        new UserServiceImpl(
            repository(
                false,
                new AtomicReference<>(),
                new DataIntegrityViolationException("duplicate username")),
            ignoredMapper(),
            passwordEncoder("encoded-password"),
            ignoredMailService(),
            ignoredEmailVerificationService());

    assertThatThrownBy(() -> userService.register(request))
        .isInstanceOf(ResourceException.class)
        .extracting("response.code")
        .isEqualTo("EMAIL_ALREADY_EXISTS");
  }

  private UserRepository repository(
      boolean existsByUsername, AtomicReference<User> savedUser, RuntimeException saveException) {
    AtomicBoolean saveCalled = new AtomicBoolean(false);
    return (UserRepository)
        Proxy.newProxyInstance(
            UserRepository.class.getClassLoader(),
            new Class<?>[] {UserRepository.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "existsByUsername" -> existsByUsername;
                case "findByUsername" -> Optional.empty();
                case "save" -> {
                  saveCalled.set(true);
                  if (saveException != null) {
                    throw saveException;
                  }
                  savedUser.set((User) args[0]);
                  yield args[0];
                }
                case "toString" -> "UserRepositoryTestDouble(saveCalled=" + saveCalled.get() + ")";
                default -> throw new UnsupportedOperationException(method.getName());
              };
            });
  }

  private UserMapper ignoredMapper() {
    return user -> {
      throw new AssertionError("Mapper should not be called");
    };
  }

  private PasswordEncoder passwordEncoder(String encodedPassword) {
    return new PasswordEncoder() {
      @Override
      public String encode(CharSequence rawPassword) {
        return encodedPassword;
      }

      @Override
      public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return false;
      }
    };
  }

  private MailService ignoredMailService() {
    return (type, request) -> {
      throw new AssertionError("Mail service should not be called");
    };
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

  private EmailVerificationService ignoredEmailVerificationService() {
    return new EmailVerificationService() {
      @Override
      public String createVerificationToken(User user) {
        throw new AssertionError("Email verification service should not be called");
      }

      @Override
      public User verifyEmail(String rawToken) {
        throw new AssertionError("Email verification service should not be called");
      }
    };
  }

  private static class RecordingEmailVerificationService implements EmailVerificationService {
    private final String rawToken;
    private User user;

    private RecordingEmailVerificationService(String rawToken) {
      this.rawToken = rawToken;
    }

    @Override
    public String createVerificationToken(User user) {
      this.user = user;
      return rawToken;
    }

    @Override
    public User verifyEmail(String rawToken) {
      throw new AssertionError("Verify email should not be called");
    }
  }
}
