package me.nghlong3004.vqc.api.testcase.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.dataset.enums.DatasetSourceType;
import me.nghlong3004.vqc.api.dataset.enums.DatasetStatus;
import me.nghlong3004.vqc.api.dataset.repository.DatasetRepository;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;
import me.nghlong3004.vqc.api.testcase.repository.TestCaseRepository;
import me.nghlong3004.vqc.api.testcase.response.ImportTestCaseResponse;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/13/2026
 */
class TestCaseImportServiceImplTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void importExcelParsesHeaderAndDataRowsAndSavesTestCases() {
    User creator = user();
    Dataset dataset = dataset(project(creator), creator, DatasetStatus.DRAFT);
    List<TestCase> saved = new ArrayList<>();
    TestCaseImportServiceImpl service = service(creator, dataset, saved, 0L, Optional.of(5));

    MockMultipartFile file = excelFile("test.xlsx", new String[] {"question", "ground_truth"},
        new String[][] {
            {"What is AI?", "Artificial Intelligence"},
            {"What is ML?", "Machine Learning"}
        });

    ImportTestCaseResponse response =
        service.importTestCases(dataset.getPublicId(), file, "qc.demo@example.com");

    assertThat(response.totalRows()).isEqualTo(2);
    assertThat(response.importedCount()).isEqualTo(2);
    assertThat(response.skippedCount()).isZero();
    assertThat(response.errors()).isEmpty();
    assertThat(saved).hasSize(2);
    assertThat(saved.get(0).getQuestion()).isEqualTo("What is AI?");
    assertThat(saved.get(0).getGroundTruth()).isEqualTo("Artificial Intelligence");
    assertThat(saved.get(0).getSortOrder()).isEqualTo(6);
    assertThat(saved.get(1).getQuestion()).isEqualTo("What is ML?");
    assertThat(saved.get(1).getSortOrder()).isEqualTo(7);
    assertThat(saved.get(0).getStatus()).isEqualTo(TestCaseStatus.ACTIVE);
  }

  @Test
  void importCsvParsesHeaderAndDataRowsAndSavesTestCases() {
    User creator = user();
    Dataset dataset = dataset(project(creator), creator, DatasetStatus.DRAFT);
    List<TestCase> saved = new ArrayList<>();
    TestCaseImportServiceImpl service = service(creator, dataset, saved, 0L, Optional.empty());

    String csv = "question,ground_truth\nWhat is AI?,Artificial Intelligence\nWhat is ML?,Machine Learning\n";
    MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes());

    ImportTestCaseResponse response =
        service.importTestCases(dataset.getPublicId(), file, "qc.demo@example.com");

    assertThat(response.totalRows()).isEqualTo(2);
    assertThat(response.importedCount()).isEqualTo(2);
    assertThat(response.skippedCount()).isZero();
    assertThat(saved).hasSize(2);
    assertThat(saved.get(0).getQuestion()).isEqualTo("What is AI?");
    assertThat(saved.get(0).getSortOrder()).isEqualTo(1);
    assertThat(saved.get(1).getQuestion()).isEqualTo("What is ML?");
    assertThat(saved.get(1).getSortOrder()).isEqualTo(2);
  }

  @Test
  void importCsvHandlesQuotedFieldsWithCommasAndQuotes() {
    User creator = user();
    Dataset dataset = dataset(project(creator), creator, DatasetStatus.DRAFT);
    List<TestCase> saved = new ArrayList<>();
    TestCaseImportServiceImpl service = service(creator, dataset, saved, 0L, Optional.empty());

    String csv = "question,ground_truth\n\"What is AI, really?\",\"It is \"\"Artificial Intelligence\"\"\"\n";
    MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes());

    ImportTestCaseResponse response =
        service.importTestCases(dataset.getPublicId(), file, "qc.demo@example.com");

    assertThat(response.importedCount()).isEqualTo(1);
    assertThat(saved.get(0).getQuestion()).isEqualTo("What is AI, really?");
    assertThat(saved.get(0).getGroundTruth()).isEqualTo("It is \"Artificial Intelligence\"");
  }

  @Test
  void importSkipsRowsWithBlankQuestionAndReportsErrors() {
    User creator = user();
    Dataset dataset = dataset(project(creator), creator, DatasetStatus.DRAFT);
    List<TestCase> saved = new ArrayList<>();
    TestCaseImportServiceImpl service = service(creator, dataset, saved, 0L, Optional.empty());

    String csv = "question,ground_truth\nWhat is AI?,Answer 1\n,Missing question\nWhat is DL?,Answer 3\n";
    MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes());

    ImportTestCaseResponse response =
        service.importTestCases(dataset.getPublicId(), file, "qc.demo@example.com");

    assertThat(response.totalRows()).isEqualTo(3);
    assertThat(response.importedCount()).isEqualTo(2);
    assertThat(response.skippedCount()).isEqualTo(1);
    assertThat(response.errors()).hasSize(1);
    assertThat(response.errors().getFirst().row()).isEqualTo(2);
    assertThat(response.errors().getFirst().column()).isEqualTo("question");
    assertThat(response.errors().getFirst().message()).isEqualTo("Question is required.");
    assertThat(saved).hasSize(2);
  }

  @Test
  void importRejectsEmptyFile() {
    User creator = user();
    Dataset dataset = dataset(project(creator), creator, DatasetStatus.DRAFT);
    TestCaseImportServiceImpl service = service(creator, dataset, new ArrayList<>(), 0L, Optional.empty());

    MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);

    assertThatThrownBy(() ->
            service.importTestCases(dataset.getPublicId(), file, "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("IMPORT_FILE_EMPTY");
  }

  @Test
  void importRejectsFileLargerThan5MB() {
    User creator = user();
    Dataset dataset = dataset(project(creator), creator, DatasetStatus.DRAFT);
    TestCaseImportServiceImpl service = service(creator, dataset, new ArrayList<>(), 0L, Optional.empty());

    byte[] largeContent = new byte[5 * 1024 * 1024 + 1];
    MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", largeContent);

    assertThatThrownBy(() ->
            service.importTestCases(dataset.getPublicId(), file, "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("IMPORT_FILE_TOO_LARGE");
  }

  @Test
  void importRejectsUnsupportedFileFormat() {
    User creator = user();
    Dataset dataset = dataset(project(creator), creator, DatasetStatus.DRAFT);
    TestCaseImportServiceImpl service = service(creator, dataset, new ArrayList<>(), 0L, Optional.empty());

    MockMultipartFile file =
        new MockMultipartFile("file", "test.json", "application/json", "{\"key\":\"value\"}".getBytes());

    assertThatThrownBy(() ->
            service.importTestCases(dataset.getPublicId(), file, "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("IMPORT_FILE_INVALID_FORMAT");
  }

  @Test
  void importRejectsWhenTotalTestCasesWouldExceed100() {
    User creator = user();
    Dataset dataset = dataset(project(creator), creator, DatasetStatus.DRAFT);
    TestCaseImportServiceImpl service = service(creator, dataset, new ArrayList<>(), 99L, Optional.empty());

    String csv = "question,ground_truth\nQ1,A1\nQ2,A2\n";
    MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes());

    assertThatThrownBy(() ->
            service.importTestCases(dataset.getPublicId(), file, "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("IMPORT_TOO_MANY_ROWS");
  }

  @Test
  void importRejectsArchivedDataset() {
    User creator = user();
    Dataset dataset = dataset(project(creator), creator, DatasetStatus.ARCHIVED);
    TestCaseImportServiceImpl service = service(creator, dataset, new ArrayList<>(), 0L, Optional.empty());

    String csv = "question\nQ1\n";
    MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes());

    assertThatThrownBy(() ->
            service.importTestCases(dataset.getPublicId(), file, "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("DATASET_ARCHIVED");
  }

  @Test
  void importRejectsMissingUser() {
    TestCaseImportServiceImpl service =
        new TestCaseImportServiceImpl(
            ignoredTestCaseRepository(),
            ignoredDatasetRepository(),
            userRepository(Optional.empty()),
            objectMapper);

    String csv = "question\nQ1\n";
    MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes());

    assertThatThrownBy(() ->
            service.importTestCases(UUID.randomUUID(), file, "missing@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("USER_NOT_FOUND");
  }

  @Test
  void importRejectsMissingDataset() {
    User creator = user();
    TestCaseImportServiceImpl service =
        new TestCaseImportServiceImpl(
            ignoredTestCaseRepository(),
            datasetRepository(Optional.empty()),
            userRepository(Optional.of(creator)),
            objectMapper);

    String csv = "question\nQ1\n";
    MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes());

    assertThatThrownBy(() ->
            service.importTestCases(UUID.randomUUID(), file, "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("DATASET_NOT_FOUND");
  }

  @Test
  void importCsvWithPreconditionAndMetadataJsonColumns() {
    User creator = user();
    Dataset dataset = dataset(project(creator), creator, DatasetStatus.DRAFT);
    List<TestCase> saved = new ArrayList<>();
    TestCaseImportServiceImpl service = service(creator, dataset, saved, 0L, Optional.empty());

    String csv = "question,ground_truth,precondition,metadata\n"
        + "What is AI?,Answer,\"{\"\"key\"\":\"\"value\"\"}\",\"{\"\"userId\"\":\"\"u1\"\"}\"\n";
    MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes());

    ImportTestCaseResponse response =
        service.importTestCases(dataset.getPublicId(), file, "qc.demo@example.com");

    assertThat(response.importedCount()).isEqualTo(1);
    assertThat(saved.get(0).getPrecondition()).containsEntry("key", "value");
    assertThat(saved.get(0).getMetadata()).containsEntry("userId", "u1");
  }

  @Test
  void importCsvWithInvalidJsonReportsErrorButStillImportsRow() {
    User creator = user();
    Dataset dataset = dataset(project(creator), creator, DatasetStatus.DRAFT);
    List<TestCase> saved = new ArrayList<>();
    TestCaseImportServiceImpl service = service(creator, dataset, saved, 0L, Optional.empty());

    String csv = "question,precondition\nWhat is AI?,not-valid-json\n";
    MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes());

    ImportTestCaseResponse response =
        service.importTestCases(dataset.getPublicId(), file, "qc.demo@example.com");

    assertThat(response.importedCount()).isEqualTo(1);
    assertThat(response.errors()).hasSize(1);
    assertThat(response.errors().getFirst().column()).isEqualTo("precondition");
    assertThat(response.errors().getFirst().message()).startsWith("Invalid JSON:");
    assertThat(saved.get(0).getQuestion()).isEqualTo("What is AI?");
    assertThat(saved.get(0).getPrecondition()).isNull();
  }

  @Test
  void importExcelWithHeaderOnlyReturnsEmpty() {
    User creator = user();
    Dataset dataset = dataset(project(creator), creator, DatasetStatus.DRAFT);
    TestCaseImportServiceImpl service = service(creator, dataset, new ArrayList<>(), 0L, Optional.empty());

    MockMultipartFile file = excelFile("test.xlsx", new String[] {"question", "ground_truth"}, new String[0][]);

    assertThatThrownBy(() ->
            service.importTestCases(dataset.getPublicId(), file, "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting(e -> ((ResourceException) e).getResponse().code())
        .isEqualTo("IMPORT_FILE_EMPTY");
  }

  // ── Factory helpers ──

  private TestCaseImportServiceImpl service(
      User creator, Dataset dataset, List<TestCase> savedBucket,
      long existingActiveCount, Optional<Integer> maxSortOrder) {
    return new TestCaseImportServiceImpl(
        testCaseRepository(savedBucket, existingActiveCount, maxSortOrder),
        datasetRepository(Optional.of(dataset)),
        userRepository(Optional.of(creator)),
        objectMapper);
  }

  private TestCaseRepository testCaseRepository(
      List<TestCase> savedBucket, long existingActiveCount, Optional<Integer> maxSortOrder) {
    return proxy(
        TestCaseRepository.class,
        (proxy, method, args) -> {
          if ("saveAll".equals(method.getName())) {
            @SuppressWarnings("unchecked")
            Iterable<TestCase> entities = (Iterable<TestCase>) args[0];
            entities.forEach(savedBucket::add);
            return entities;
          }
          if ("countByDatasetAndStatus".equals(method.getName())) {
            return existingActiveCount;
          }
          if ("findMaxSortOrderByDatasetId".equals(method.getName())) {
            return maxSortOrder;
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

  private DatasetRepository datasetRepository(Optional<Dataset> dataset) {
    return proxy(
        DatasetRepository.class,
        (proxy, method, args) -> {
          if ("findByPublicIdAndCreatedBy".equals(method.getName())) {
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
        .id(1L)
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

  private MockMultipartFile excelFile(String filename, String[] headers, String[][] dataRows) {
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      XSSFSheet sheet = workbook.createSheet("Sheet1");
      org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        headerRow.createCell(i).setCellValue(headers[i]);
      }
      for (int r = 0; r < dataRows.length; r++) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(r + 1);
        for (int c = 0; c < dataRows[r].length; c++) {
          row.createCell(c).setCellValue(dataRows[r][c]);
        }
      }
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      workbook.write(bos);
      return new MockMultipartFile(
          "file", filename,
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          bos.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
