package me.nghlong3004.vqc.api.testcase.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.dataset.enums.DatasetSourceType;
import me.nghlong3004.vqc.api.dataset.enums.DatasetStatus;
import me.nghlong3004.vqc.api.dataset.repository.DatasetRepository;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;
import me.nghlong3004.vqc.api.testcase.mapper.TestCaseMapper;
import me.nghlong3004.vqc.api.testcase.repository.TestCaseRepository;
import me.nghlong3004.vqc.api.testcase.request.CreateTestCaseRequest;
import me.nghlong3004.vqc.api.testcase.response.TestCaseResponse;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class TestCaseServiceImplTest {

  @Test
  void createTestCaseNormalizesCreatorAndPersistsActiveTestCase() {
    User creator = user();
    Dataset dataset = dataset(project(creator), creator, DatasetStatus.DRAFT);
    AtomicReference<TestCase> savedTestCase = new AtomicReference<>();
    AtomicReference<DatasetLookup> datasetLookup = new AtomicReference<>();
    TestCaseServiceImpl testCaseService =
        new TestCaseServiceImpl(
            testCaseRepository(savedTestCase),
            datasetRepository(datasetLookup, Optional.of(dataset)),
            userRepository(Optional.of(creator)),
            new TestCaseMapper());

    TestCaseResponse response =
        testCaseService.createTestCase(
            dataset.getPublicId(),
            new CreateTestCaseRequest(
                "  HEALTH_001  ",
                "  How many steps did I walk today?  ",
                Map.of("steps", 8200),
                "  The user walked 8,200 steps today.  ",
                Map.of("userId", "demo-user-1"),
                null,
                1),
            "  QC.Demo@Example.COM  ");

    assertThat(datasetLookup.get().publicId()).isEqualTo(dataset.getPublicId());
    assertThat(datasetLookup.get().createdBy()).isSameAs(creator);
    assertThat(savedTestCase.get().getDataset()).isSameAs(dataset);
    assertThat(savedTestCase.get().getExternalId()).isEqualTo("HEALTH_001");
    assertThat(savedTestCase.get().getQuestion()).isEqualTo("How many steps did I walk today?");
    assertThat(savedTestCase.get().getPrecondition()).containsEntry("steps", 8200);
    assertThat(savedTestCase.get().getGroundTruth()).isEqualTo("The user walked 8,200 steps today.");
    assertThat(savedTestCase.get().getMetadata()).containsEntry("userId", "demo-user-1");
    assertThat(savedTestCase.get().getStatus()).isEqualTo(TestCaseStatus.ACTIVE);
    assertThat(savedTestCase.get().getSortOrder()).isEqualTo(1);
    assertThat(response.datasetPublicId()).isEqualTo(dataset.getPublicId());
  }

  @Test
  void createTestCaseRejectsMissingCreator() {
    TestCaseServiceImpl testCaseService =
        new TestCaseServiceImpl(
            ignoredTestCaseRepository(),
            ignoredDatasetRepository(),
            userRepository(Optional.empty()),
            new TestCaseMapper());

    assertThatThrownBy(
            () ->
                testCaseService.createTestCase(
                    UUID.randomUUID(),
                    new CreateTestCaseRequest(null, "Question?", null, null, null, null, null),
                    "missing@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(error -> ((ResourceException) error).getResponse().code())
        .isEqualTo("USER_NOT_FOUND");
  }

  @Test
  void createTestCaseRejectsMissingDataset() {
    User creator = user();
    AtomicReference<DatasetLookup> datasetLookup = new AtomicReference<>();
    TestCaseServiceImpl testCaseService =
        new TestCaseServiceImpl(
            ignoredTestCaseRepository(),
            datasetRepository(datasetLookup, Optional.empty()),
            userRepository(Optional.of(creator)),
            new TestCaseMapper());
    UUID datasetPublicId = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                testCaseService.createTestCase(
                    datasetPublicId,
                    new CreateTestCaseRequest(null, "Question?", null, null, null, null, null),
                    "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(error -> ((ResourceException) error).getResponse().code())
        .isEqualTo("DATASET_NOT_FOUND");
    assertThat(datasetLookup.get().publicId()).isEqualTo(datasetPublicId);
    assertThat(datasetLookup.get().createdBy()).isSameAs(creator);
  }

  @Test
  void createTestCaseRejectsArchivedDataset() {
    User creator = user();
    Dataset dataset = dataset(project(creator), creator, DatasetStatus.ARCHIVED);
    TestCaseServiceImpl testCaseService =
        new TestCaseServiceImpl(
            ignoredTestCaseRepository(),
            datasetRepository(new AtomicReference<>(), Optional.of(dataset)),
            userRepository(Optional.of(creator)),
            new TestCaseMapper());

    assertThatThrownBy(
            () ->
                testCaseService.createTestCase(
                    dataset.getPublicId(),
                    new CreateTestCaseRequest(null, "Question?", null, null, null, null, null),
                    "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(error -> ((ResourceException) error).getResponse().code())
        .isEqualTo("DATASET_ARCHIVED");
  }

  private TestCaseRepository testCaseRepository(AtomicReference<TestCase> savedTestCase) {
    return proxy(
        TestCaseRepository.class,
        (proxy, method, args) -> {
          if ("save".equals(method.getName())) {
            TestCase testCase = (TestCase) args[0];
            savedTestCase.set(testCase);
            return testCase;
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private TestCaseRepository ignoredTestCaseRepository() {
    return proxy(
        TestCaseRepository.class,
        (proxy, method, args) -> {
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private DatasetRepository datasetRepository(
      AtomicReference<DatasetLookup> datasetLookup, Optional<Dataset> dataset) {
    return proxy(
        DatasetRepository.class,
        (proxy, method, args) -> {
          if ("findByPublicIdAndCreatedBy".equals(method.getName())) {
            datasetLookup.set(new DatasetLookup((UUID) args[0], (User) args[1]));
            return dataset;
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private DatasetRepository ignoredDatasetRepository() {
    return proxy(
        DatasetRepository.class,
        (proxy, method, args) -> {
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private UserRepository userRepository(Optional<User> user) {
    return proxy(
        UserRepository.class,
        (proxy, method, args) -> {
          if ("findByUsername".equals(method.getName())) {
            assertThat(args[0]).isEqualTo(String.valueOf(args[0]).trim().toLowerCase());
            return user;
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  @SuppressWarnings("unchecked")
  private <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
  }

  private User user() {
    return User.builder()
        .publicId(UUID.fromString("7b7b7d42-5f42-4c5a-9281-8d1d36f6f59d"))
        .username("qc.demo@example.com")
        .passwordHash("hash")
        .displayName("QC Demo")
        .build();
  }

  private Project project(User creator) {
    return Project.builder()
        .publicId(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"))
        .name("AI Health Chatbot Demo")
        .createdBy(creator)
        .build();
  }

  private Dataset dataset(Project project, User creator, DatasetStatus status) {
    return Dataset.builder()
        .publicId(UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"))
        .project(project)
        .name("Health Demo Dataset")
        .sourceType(DatasetSourceType.SAMPLE_DEMO)
        .status(status)
        .createdBy(creator)
        .createdAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"))
        .updatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"))
        .build();
  }

  private record DatasetLookup(UUID publicId, User createdBy) {}
}
