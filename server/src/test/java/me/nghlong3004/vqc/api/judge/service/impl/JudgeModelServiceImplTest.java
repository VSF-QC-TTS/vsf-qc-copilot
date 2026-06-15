package me.nghlong3004.vqc.api.judge.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.common.crypto.AesGcmEncryptor;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.judge.entity.JudgeModel;
import me.nghlong3004.vqc.api.judge.enums.JudgeProvider;
import me.nghlong3004.vqc.api.judge.mapper.JudgeModelMapper;
import me.nghlong3004.vqc.api.judge.repository.JudgeModelRepository;
import me.nghlong3004.vqc.api.judge.request.CreateJudgeModelRequest;
import me.nghlong3004.vqc.api.judge.response.JudgeModelResponse;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.repository.ProjectRepository;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
class JudgeModelServiceImplTest {

  private static final String TEST_HEX_KEY =
      "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

  @Test
  void createJudgeModelEncryptsApiKeyAndReturnsMaskedKeyOnly() {
    User user = user();
    Project project = project(user);
    AtomicReference<JudgeModel> savedRef = new AtomicReference<>();
    JudgeModelServiceImpl service =
        service(userRepository(user), projectRepository(project), savingJudgeRepository(savedRef));

    JudgeModelResponse response =
        service.createJudgeModel(
            project.getPublicId(),
            new CreateJudgeModelRequest(
                " Gemini Judge ",
                JudgeProvider.GEMINI,
                " gemini-2.5-flash ",
                " ",
                " sk-secret-1234 ",
                " ",
                null),
            "QC.Demo@example.com");

    JudgeModel saved = savedRef.get();
    assertThat(saved.getName()).isEqualTo("Gemini Judge");
    assertThat(saved.getModelName()).isEqualTo("gemini-2.5-flash");
    assertThat(saved.getBaseUrl()).isNull();
    assertThat(saved.getConfigJson()).isNull();
    assertThat(saved.getEncryptedApiKey()).isNotBlank().isNotEqualTo("sk-secret-1234");
    assertThat(saved.getApiKeyMasked()).isEqualTo("****1234");
    assertThat(response.apiKeyMasked()).isEqualTo("****1234");
  }

  @Test
  void testConnectionRejectsInactiveJudgeModel() {
    User user = user();
    JudgeModel judgeModel =
        JudgeModel.builder().createdBy(user).project(project(user)).active(false).build();
    JudgeModelServiceImpl service =
        service(userRepository(user), projectRepository(judgeModel.getProject()), findingJudgeRepository(judgeModel));

    assertThatThrownBy(
            () -> service.testConnection(judgeModel.getPublicId(), "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting("response.code")
        .isEqualTo("JUDGE_MODEL_INACTIVE");
  }

  private JudgeModelServiceImpl service(
      UserRepository userRepository,
      ProjectRepository projectRepository,
      JudgeModelRepository judgeModelRepository) {
    return new JudgeModelServiceImpl(
        judgeModelRepository,
        projectRepository,
        userRepository,
        new AesGcmEncryptor(TEST_HEX_KEY),
        new JudgeModelMapper());
  }

  private UserRepository userRepository(User user) {
    return proxy(
        UserRepository.class,
        (proxy, method, args) -> {
          if (method.getName().equals("findByUsername")) {
            return Optional.of(user);
          }
          return unsupported(method);
        });
  }

  private ProjectRepository projectRepository(Project project) {
    return proxy(
        ProjectRepository.class,
        (proxy, method, args) -> {
          if (method.getName().equals("findByPublicIdAndCreatedBy")) {
            return Optional.of(project);
          }
          return unsupported(method);
        });
  }

  private JudgeModelRepository savingJudgeRepository(AtomicReference<JudgeModel> savedRef) {
    return proxy(
        JudgeModelRepository.class,
        (proxy, method, args) -> {
          if (method.getName().equals("save")) {
            JudgeModel saved = (JudgeModel) args[0];
            savedRef.set(saved);
            return saved;
          }
          return unsupported(method);
        });
  }

  private JudgeModelRepository findingJudgeRepository(JudgeModel judgeModel) {
    return proxy(
        JudgeModelRepository.class,
        (proxy, method, args) -> {
          if (method.getName().equals("findByPublicIdAndCreatedBy")) {
            return Optional.of(judgeModel);
          }
          return unsupported(method);
        });
  }

  @SuppressWarnings("unchecked")
  private <T> T proxy(Class<T> type, InvocationHandler handler) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
  }

  private Object unsupported(Method method) {
    throw new UnsupportedOperationException(method.getName());
  }

  private User user() {
    return User.builder()
        .id(1L)
        .username("qc.demo@example.com")
        .displayName("QC Demo")
        .passwordHash("hash")
        .build();
  }

  private Project project(User user) {
    return Project.builder().id(1L).name("Health Project").createdBy(user).build();
  }
}
