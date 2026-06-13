package me.nghlong3004.vqc.api.testcase.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.exception.ErrorResponse;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;
import me.nghlong3004.vqc.api.testcase.request.CreateTestCaseRequest;
import me.nghlong3004.vqc.api.testcase.request.UpdateTestCaseRequest;
import me.nghlong3004.vqc.api.testcase.response.ImportTestCaseResponse;
import me.nghlong3004.vqc.api.testcase.response.TestCasePageResponse;
import me.nghlong3004.vqc.api.testcase.response.TestCaseResponse;
import me.nghlong3004.vqc.api.testcase.service.TestCaseImportService;
import me.nghlong3004.vqc.api.testcase.service.TestCaseService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Test Cases", description = "Dataset test case APIs")
public class TestCaseController {

  private final TestCaseService testCaseService;
  private final TestCaseImportService testCaseImportService;

  @Operation(summary = "Create test case", description = "Creates a test case under a dataset.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Test case created",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TestCaseResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Validation failed",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Authentication is required",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Dataset not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(
      value = "/api/v1/datasets/{datasetPublicId}/test-cases",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public TestCaseResponse createTestCase(
      @PathVariable UUID datasetPublicId,
      @Valid @RequestBody CreateTestCaseRequest request,
      Principal principal) {
    return testCaseService.createTestCase(datasetPublicId, request, principal.getName());
  }

  @Operation(
      summary = "Import test cases",
      description = "Bulk imports test cases from an Excel (.xlsx) or CSV (.csv) file.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Import completed",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ImportTestCaseResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "File is empty, too large, or has unsupported format",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Authentication is required",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Dataset not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "422",
        description = "Import would exceed maximum test case limit",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(
      value = "/api/v1/datasets/{datasetPublicId}/test-cases/import",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ImportTestCaseResponse importTestCases(
      @PathVariable UUID datasetPublicId,
      @RequestParam("file") MultipartFile file,
      Principal principal) {
    return testCaseImportService.importTestCases(datasetPublicId, file, principal.getName());
  }

  @Operation(summary = "List test cases", description = "Lists test cases under a dataset.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Test case page",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TestCasePageResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid dataset identifier, status, or pagination request",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Authentication is required",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Dataset not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping(
      value = "/api/v1/datasets/{datasetPublicId}/test-cases",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public TestCasePageResponse listTestCases(
      @PathVariable UUID datasetPublicId,
      @RequestParam(required = false) TestCaseStatus status,
      @PageableDefault(size = 100, sort = "sortOrder", direction = Sort.Direction.ASC)
          Pageable pageable,
      Principal principal) {
    return testCaseService.listTestCases(datasetPublicId, status, pageable, principal.getName());
  }

  @Operation(summary = "Update test case", description = "Updates a test case owned by the authenticated user.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Test case updated",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TestCaseResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Authentication is required",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Test case not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PatchMapping(
      value = "/api/v1/test-cases/{testCasePublicId}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public TestCaseResponse updateTestCase(
      @PathVariable UUID testCasePublicId,
      @Valid @RequestBody UpdateTestCaseRequest request,
      Principal principal) {
    return testCaseService.updateTestCase(testCasePublicId, request, principal.getName());
  }

  @Operation(summary = "Delete test case", description = "Deletes a test case owned by the authenticated user.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Test case deleted", content = @Content),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid test case identifier",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Authentication is required",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Test case not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @DeleteMapping("/api/v1/test-cases/{testCasePublicId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteTestCase(@PathVariable UUID testCasePublicId, Principal principal) {
    testCaseService.deleteTestCase(testCasePublicId, principal.getName());
  }
}
