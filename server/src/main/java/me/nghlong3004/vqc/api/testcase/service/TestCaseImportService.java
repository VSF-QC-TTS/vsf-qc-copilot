package me.nghlong3004.vqc.api.testcase.service;

import java.util.UUID;
import me.nghlong3004.vqc.api.testcase.response.ImportTestCaseResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Handles bulk import of test cases from Excel and CSV files.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/13/2026
 */
public interface TestCaseImportService {

  /**
   * Imports test cases from an uploaded file into a dataset.
   *
   * <p>Supports Excel (.xlsx) and CSV (.csv) files. The file must contain a header row with at least
   * a {@code question} column. Optional columns: {@code ground_truth}, {@code precondition},
   * {@code metadata}.
   *
   * @param datasetPublicId public identifier of the target {@link
   *     me.nghlong3004.vqc.api.dataset.entity.Dataset}
   * @param file uploaded multipart file (.xlsx or .csv)
   * @param username authenticated principal username/email
   * @return import result summary with per-row error details
   */
  ImportTestCaseResponse importTestCases(UUID datasetPublicId, MultipartFile file, String username);
}
