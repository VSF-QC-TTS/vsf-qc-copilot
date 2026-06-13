package me.nghlong3004.vqc.api.testcase.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Bulk import result summary returned after importing test cases from a file.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/13/2026
 */
@Schema(name = "ImportTestCaseResponse", description = "Bulk import result summary")
public record ImportTestCaseResponse(
    @Schema(description = "Total rows parsed from the file (excluding header).", example = "25")
        int totalRows,
    @Schema(description = "Number of test cases successfully imported.", example = "23")
        int importedCount,
    @Schema(description = "Number of rows skipped due to validation errors.", example = "2")
        int skippedCount,
    @Schema(description = "Per-row validation error details.") List<ImportError> errors) {

  /**
   * Per-row import validation error.
   *
   * @author nghlong3004 (Long Nguyen Hoang)
   * @since 6/13/2026
   */
  @Schema(name = "ImportError", description = "Per-row import validation error")
  public record ImportError(
      @Schema(description = "1-based row number in the source file (excluding header).", example = "3")
          int row,
      @Schema(description = "Column name where the error occurred.", example = "question")
          String column,
      @Schema(description = "Human-readable error description.", example = "Question is required.")
          String message) {}
}
