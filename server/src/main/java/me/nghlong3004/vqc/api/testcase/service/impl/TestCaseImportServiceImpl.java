package me.nghlong3004.vqc.api.testcase.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.dataset.enums.DatasetStatus;
import me.nghlong3004.vqc.api.dataset.repository.DatasetRepository;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;
import me.nghlong3004.vqc.api.testcase.repository.TestCaseRepository;
import me.nghlong3004.vqc.api.testcase.response.ImportTestCaseResponse;
import me.nghlong3004.vqc.api.testcase.response.ImportTestCaseResponse.ImportError;
import me.nghlong3004.vqc.api.testcase.service.TestCaseImportService;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/13/2026
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestCaseImportServiceImpl implements TestCaseImportService {

  private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
  private static final int MAX_TEST_CASES = 100;
  private static final String COL_QUESTION = "question";
  private static final String COL_GROUND_TRUTH = "ground_truth";
  private static final String COL_PRECONDITION = "precondition";
  private static final String COL_METADATA = "metadata";
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final TestCaseRepository testCaseRepository;
  private final DatasetRepository datasetRepository;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public ImportTestCaseResponse importTestCases(
      UUID datasetPublicId, MultipartFile file, String username) {
    User creator = findCreator(username);
    Dataset dataset = findDataset(datasetPublicId, creator);

    if (dataset.getStatus() == DatasetStatus.ARCHIVED) {
      throw new ResourceException(ErrorCode.DATASET_ARCHIVED);
    }
    validateFile(file);

    String filename = file.getOriginalFilename();
    boolean isExcel = filename != null && filename.toLowerCase().endsWith(".xlsx");
    boolean isCsv = filename != null && filename.toLowerCase().endsWith(".csv");
    if (!isExcel && !isCsv) {
      throw new ResourceException(ErrorCode.IMPORT_FILE_INVALID_FORMAT);
    }

    List<Map<String, String>> rawRows;
    try {
      rawRows = isExcel ? parseExcel(file) : parseCsv(file);
    } catch (IOException ex) {
      log.error("Failed to parse import file for dataset {}", datasetPublicId, ex);
      throw new ResourceException(ErrorCode.IMPORT_FILE_INVALID_FORMAT);
    }

    if (rawRows.isEmpty()) {
      throw new ResourceException(ErrorCode.IMPORT_FILE_EMPTY);
    }

    long existingCount =
        testCaseRepository.countByDatasetAndStatus(dataset, TestCaseStatus.ACTIVE);
    if (existingCount + rawRows.size() > MAX_TEST_CASES) {
      throw new ResourceException(ErrorCode.IMPORT_TOO_MANY_ROWS);
    }

    int nextSortOrder =
        testCaseRepository.findMaxSortOrderByDatasetId(dataset.getId()).orElse(0) + 1;

    List<ImportError> errors = new ArrayList<>();
    List<TestCase> toSave = new ArrayList<>();
    for (int i = 0; i < rawRows.size(); i++) {
      Map<String, String> row = rawRows.get(i);
      int rowNumber = i + 1;
      String question = trimToNull(row.get(COL_QUESTION));
      if (question == null) {
        errors.add(new ImportError(rowNumber, COL_QUESTION, "Question is required."));
        continue;
      }
      TestCase testCase =
          TestCase.builder()
              .dataset(dataset)
              .question(question)
              .groundTruth(trimToNull(row.get(COL_GROUND_TRUTH)))
              .precondition(parseJson(row.get(COL_PRECONDITION), rowNumber, COL_PRECONDITION, errors))
              .metadata(parseJson(row.get(COL_METADATA), rowNumber, COL_METADATA, errors))
              .status(TestCaseStatus.ACTIVE)
              .sortOrder(nextSortOrder++)
              .build();
      toSave.add(testCase);
    }

    testCaseRepository.saveAll(toSave);
    log.info(
        "Imported {} test cases into dataset {} by user {} ({} skipped)",
        toSave.size(),
        dataset.getPublicId(),
        creator.getPublicId(),
        errors.size());

    return new ImportTestCaseResponse(rawRows.size(), toSave.size(), errors.size(), errors);
  }

  List<Map<String, String>> parseExcel(MultipartFile file) throws IOException {
    try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);
      if (sheet.getPhysicalNumberOfRows() < 2) {
        return List.of();
      }

      Row headerRow = sheet.getRow(0);
      Map<Integer, String> columnMapping = new HashMap<>();
      for (Cell cell : headerRow) {
        String header = cellToString(cell);
        if (header != null) {
          columnMapping.put(cell.getColumnIndex(), header.toLowerCase().trim());
        }
      }

      List<Map<String, String>> rows = new ArrayList<>();
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) {
          continue;
        }
        Map<String, String> rowData = new HashMap<>();
        boolean hasData = false;
        for (Map.Entry<Integer, String> entry : columnMapping.entrySet()) {
          Cell cell = row.getCell(entry.getKey());
          String value = cellToString(cell);
          if (value != null) {
            rowData.put(entry.getValue(), value);
            hasData = true;
          }
        }
        if (hasData) {
          rows.add(rowData);
        }
      }
      return rows;
    }
  }

  List<Map<String, String>> parseCsv(MultipartFile file) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String headerLine = reader.readLine();
      if (headerLine == null || headerLine.isBlank()) {
        return List.of();
      }

      String[] headers = splitCsvLine(headerLine);
      for (int i = 0; i < headers.length; i++) {
        headers[i] = headers[i].toLowerCase().trim();
      }

      List<Map<String, String>> rows = new ArrayList<>();
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isBlank()) {
          continue;
        }
        String[] values = splitCsvLine(line);
        Map<String, String> rowData = new HashMap<>();
        boolean hasData = false;
        for (int i = 0; i < headers.length && i < values.length; i++) {
          String value = values[i].isBlank() ? null : values[i].trim();
          if (value != null) {
            rowData.put(headers[i], value);
            hasData = true;
          }
        }
        if (hasData) {
          rows.add(rowData);
        }
      }
      return rows;
    }
  }

  private String[] splitCsvLine(String line) {
    List<String> result = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;
    for (int i = 0; i < line.length(); i++) {
      char ch = line.charAt(i);
      if (ch == '"') {
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          current.append('"');
          i++;
        } else {
          inQuotes = !inQuotes;
        }
      } else if (ch == ',' && !inQuotes) {
        result.add(current.toString());
        current.setLength(0);
      } else {
        current.append(ch);
      }
    }
    result.add(current.toString());
    return result.toArray(new String[0]);
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResourceException(ErrorCode.IMPORT_FILE_EMPTY);
    }
    if (file.getSize() > MAX_FILE_SIZE) {
      throw new ResourceException(ErrorCode.IMPORT_FILE_TOO_LARGE);
    }
  }

  private Map<String, Object> parseJson(
      String value, int rowNumber, String column, List<ImportError> errors) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(value.trim(), MAP_TYPE);
    } catch (JsonProcessingException ex) {
      errors.add(new ImportError(rowNumber, column, "Invalid JSON: " + ex.getOriginalMessage()));
      return null;
    }
  }

  private String cellToString(Cell cell) {
    if (cell == null) {
      return null;
    }
    if (cell.getCellType() == CellType.STRING) {
      String value = cell.getStringCellValue();
      return value == null || value.isBlank() ? null : value.trim();
    }
    if (cell.getCellType() == CellType.NUMERIC) {
      double val = cell.getNumericCellValue();
      if (val == Math.floor(val) && !Double.isInfinite(val)) {
        return String.valueOf((long) val);
      }
      return String.valueOf(val);
    }
    if (cell.getCellType() == CellType.BOOLEAN) {
      return String.valueOf(cell.getBooleanCellValue());
    }
    return null;
  }

  private Dataset findDataset(UUID datasetPublicId, User creator) {
    return datasetRepository
        .findByPublicIdAndCreatedBy(datasetPublicId, creator)
        .orElseThrow(() -> new ResourceException(ErrorCode.DATASET_NOT_FOUND));
  }

  private User findCreator(String username) {
    return userRepository
        .findByUsername(username.trim().toLowerCase())
        .orElseThrow(() -> new ResourceException(ErrorCode.USER_NOT_FOUND));
  }

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
