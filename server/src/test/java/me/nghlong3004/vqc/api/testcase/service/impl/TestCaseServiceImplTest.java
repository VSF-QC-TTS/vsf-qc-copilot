package me.nghlong3004.vqc.api.testcase.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.List;
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
import me.nghlong3004.vqc.api.testcase.request.UpdateTestCaseRequest;
import me.nghlong3004.vqc.api.testcase.response.TestCasePageResponse;
import me.nghlong3004.vqc.api.testcase.response.TestCaseResponse;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

  @Test
  void listTestCasesLoadsOwnedDatasetAndReturnsFilteredPage() {
    User creator = user();
    Dataset dataset = dataset(project(creator), creator, DatasetStatus.DRAFT);
    TestCase testCase = testCase(dataset);
    AtomicReference<DatasetLookup> datasetLookup = new AtomicReference<>();
    AtomicReference<TestCaseQuery> testCaseQuery = new AtomicReference<>();
    TestCaseServiceImpl testCaseService =
        new TestCaseServiceImpl(
            testCaseRepository(
                new AtomicReference<>(),
                new PageImpl<>(List.of(testCase), PageRequest.of(0, 100), 1),
                testCaseQuery),
            datasetRepository(datasetLookup, Optional.of(dataset)),
            userRepository(Optional.of(creator)),
            new TestCaseMapper());

    TestCasePageResponse response =
        testCaseService.listTestCases(
            dataset.getPublicId(), TestCaseStatus.ACTIVE, PageRequest.of(0, 100), "  QC.Demo@Example.COM  ");

    assertThat(datasetLookup.get().publicId()).isEqualTo(dataset.getPublicId());
    assertThat(testCaseQuery.get().dataset()).isSameAs(dataset);
    assertThat(testCaseQuery.get().status()).isEqualTo(TestCaseStatus.ACTIVE);
    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().publicId()).isEqualTo(testCase.getPublicId());
    assertThat(response.page()).isZero();
    assertThat(response.size()).isEqualTo(100);
    assertThat(response.totalItems()).isEqualTo(1);
    assertThat(response.totalPages()).isEqualTo(1);
  }

  @Test
  void listTestCasesUsesUnfilteredQueryWhenStatusIsMissing() {
    User creator = user();
    Dataset dataset = dataset(project(creator), creator, DatasetStatus.DRAFT);
    AtomicReference<TestCaseQuery> testCaseQuery = new AtomicReference<>();
    TestCaseServiceImpl testCaseService =
        new TestCaseServiceImpl(
            testCaseRepository(
                new AtomicReference<>(),
                new PageImpl<>(List.of(), PageRequest.of(0, 100), 0),
                testCaseQuery),
            datasetRepository(new AtomicReference<>(), Optional.of(dataset)),
            userRepository(Optional.of(creator)),
            new TestCaseMapper());

    testCaseService.listTestCases(dataset.getPublicId(), null, PageRequest.of(0, 100), "qc.demo@example.com");

    assertThat(testCaseQuery.get().dataset()).isSameAs(dataset);
    assertThat(testCaseQuery.get().status()).isNull();
  }

  @Test
  void updateTestCaseChangesEditableFields() {
    User creator = user();
    Dataset dataset = dataset(project(creator), creator, DatasetStatus.DRAFT);
    TestCase testCase = testCase(dataset);
    AtomicReference<TestCase> savedTestCase = new AtomicReference<>();
    AtomicReference<TestCaseLookup> testCaseLookup = new AtomicReference<>();
    TestCaseServiceImpl testCaseService =
        new TestCaseServiceImpl(
            testCaseRepository(
                savedTestCase,
                null,
                new AtomicReference<>(),
                Optional.of(testCase),
                testCaseLookup),
            ignoredDatasetRepository(),
            userRepository(Optional.of(creator)),
            new TestCaseMapper());

    TestCaseResponse response =
        testCaseService.updateTestCase(
            testCase.getPublicId(),
            new UpdateTestCaseRequest(
                "  HEALTH_002  ",
                "  How many active calories today?  ",
                Map.of("calories", 500),
                "  The user burned 500 active calories today.  ",
                Map.of("userId", "demo-user-2"),
                TestCaseStatus.INACTIVE,
                2),
            "qc.demo@example.com");

    assertThat(testCaseLookup.get().publicId()).isEqualTo(testCase.getPublicId());
    assertThat(testCaseLookup.get().createdBy()).isSameAs(creator);
    assertThat(savedTestCase.get()).isSameAs(testCase);
    assertThat(testCase.getExternalId()).isEqualTo("HEALTH_002");
    assertThat(testCase.getQuestion()).isEqualTo("How many active calories today?");
    assertThat(testCase.getPrecondition()).containsEntry("calories", 500);
    assertThat(testCase.getGroundTruth()).isEqualTo("The user burned 500 active calories today.");
    assertThat(testCase.getMetadata()).containsEntry("userId", "demo-user-2");
    assertThat(testCase.getStatus()).isEqualTo(TestCaseStatus.INACTIVE);
    assertThat(testCase.getSortOrder()).isEqualTo(2);
    assertThat(response.status()).isEqualTo(TestCaseStatus.INACTIVE);
  }

  @Test
  void updateTestCaseRejectsMissingTestCase() {
    User creator = user();
    AtomicReference<TestCaseLookup> testCaseLookup = new AtomicReference<>();
    TestCaseServiceImpl testCaseService =
        new TestCaseServiceImpl(
            testCaseRepository(
                new AtomicReference<>(),
                null,
                new AtomicReference<>(),
                Optional.empty(),
                testCaseLookup),
            ignoredDatasetRepository(),
            userRepository(Optional.of(creator)),
            new TestCaseMapper());
    UUID testCasePublicId = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                testCaseService.updateTestCase(
                    testCasePublicId,
                    new UpdateTestCaseRequest(null, "Question?", null, null, null, null, null),
                    "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(error -> ((ResourceException) error).getResponse().code())
        .isEqualTo("TEST_CASE_NOT_FOUND");
    assertThat(testCaseLookup.get().publicId()).isEqualTo(testCasePublicId);
    assertThat(testCaseLookup.get().createdBy()).isSameAs(creator);
  }

  @Test
  void updateTestCaseRejectsArchivedDataset() {
    User creator = user();
    Dataset dataset = dataset(project(creator), creator, DatasetStatus.ARCHIVED);
    TestCase testCase = testCase(dataset);
    TestCaseServiceImpl testCaseService =
        new TestCaseServiceImpl(
            testCaseRepository(
                new AtomicReference<>(),
                null,
                new AtomicReference<>(),
                Optional.of(testCase),
                new AtomicReference<>()),
            ignoredDatasetRepository(),
            userRepository(Optional.of(creator)),
            new TestCaseMapper());

    assertThatThrownBy(
            () ->
                testCaseService.updateTestCase(
                    testCase.getPublicId(),
                    new UpdateTestCaseRequest(null, "Question?", null, null, null, null, null),
                    "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(error -> ((ResourceException) error).getResponse().code())
        .isEqualTo("DATASET_ARCHIVED");
  }

  private TestCaseRepository testCaseRepository(AtomicReference<TestCase> savedTestCase) {
    return testCaseRepository(savedTestCase, null, new AtomicReference<>());
  }

  private TestCaseRepository testCaseRepository(
      AtomicReference<TestCase> savedTestCase,
      PageImpl<TestCase> testCasePage,
      AtomicReference<TestCaseQuery> testCaseQuery) {
    return testCaseRepository(
        savedTestCase, testCasePage, testCaseQuery, Optional.empty(), new AtomicReference<>());
  }

  private TestCaseRepository testCaseRepository(
      AtomicReference<TestCase> savedTestCase,
      PageImpl<TestCase> testCasePage,
      AtomicReference<TestCaseQuery> testCaseQuery,
      Optional<TestCase> testCaseLookupResult,
      AtomicReference<TestCaseLookup> testCaseLookup) {
    return proxy(
        TestCaseRepository.class,
        (proxy, method, args) -> {
          if ("save".equals(method.getName())) {
            TestCase testCase = (TestCase) args[0];
            savedTestCase.set(testCase);
            return testCase;
          }
          if ("findByDataset".equals(method.getName())) {
            testCaseQuery.set(new TestCaseQuery((Dataset) args[0], null));
            return testCasePage;
          }
          if ("findByDatasetAndStatus".equals(method.getName())) {
            testCaseQuery.set(new TestCaseQuery((Dataset) args[0], (TestCaseStatus) args[1]));
            return testCasePage;
          }
          if ("findByPublicIdAndDatasetCreatedBy".equals(method.getName())) {
            testCaseLookup.set(new TestCaseLookup((UUID) args[0], (User) args[1]));
            return testCaseLookupResult;
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

  private TestCase testCase(Dataset dataset) {
    return TestCase.builder()
        .publicId(UUID.fromString("b4788db3-6cf3-47df-8ae1-4c73dbb7d0a8"))
        .dataset(dataset)
        .externalId("HEALTH_001")
        .question("How many steps did I walk today?")
        .precondition(Map.of("steps", 8200))
        .groundTruth("The user walked 8,200 steps today.")
        .metadata(Map.of("userId", "demo-user-1"))
        .status(TestCaseStatus.ACTIVE)
        .sortOrder(1)
        .createdAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"))
        .updatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"))
        .build();
  }

  private record DatasetLookup(UUID publicId, User createdBy) {}

  private record TestCaseQuery(Dataset dataset, TestCaseStatus status) {}

  private record TestCaseLookup(UUID publicId, User createdBy) {}
}
