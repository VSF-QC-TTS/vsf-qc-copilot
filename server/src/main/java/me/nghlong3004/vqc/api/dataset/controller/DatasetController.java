package me.nghlong3004.vqc.api.dataset.controller;

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
import me.nghlong3004.vqc.api.dataset.request.CreateDatasetRequest;
import me.nghlong3004.vqc.api.dataset.request.GenerateDatasetRequest;
import me.nghlong3004.vqc.api.dataset.enums.DatasetStatus;
import me.nghlong3004.vqc.api.dataset.request.UpdateDatasetRequest;
import me.nghlong3004.vqc.api.dataset.response.DatasetPageResponse;
import me.nghlong3004.vqc.api.dataset.response.DatasetResponse;
import me.nghlong3004.vqc.api.dataset.response.GenerateDatasetResponse;
import me.nghlong3004.vqc.api.dataset.service.DatasetGenerationService;
import me.nghlong3004.vqc.api.dataset.service.DatasetService;
import me.nghlong3004.vqc.api.exception.ErrorResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Datasets", description = "Dataset APIs")
public class DatasetController {

  private final DatasetService datasetService;
  private final DatasetGenerationService datasetGenerationService;

  @Operation(summary = "Create dataset", description = "Creates a dataset under a project.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Dataset created",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = DatasetResponse.class))),
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
        description = "Project not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(
      value = "/api/v1/projects/{projectPublicId}/datasets",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public DatasetResponse createDataset(
      @PathVariable UUID projectPublicId,
      @Valid @RequestBody CreateDatasetRequest request,
      Principal principal) {
    return datasetService.createDataset(projectPublicId, request, principal.getName());
  }

  @Operation(
      summary = "Generate test cases",
      description = "Queues an AI-powered test case generation job for the dataset.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "202",
        description = "Generation job queued",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = GenerateDatasetResponse.class))),
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
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "422",
        description = "Generation would exceed maximum test case limit",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(
      value = "/api/v1/datasets/{datasetPublicId}/generate",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public GenerateDatasetResponse generateTestCases(
      @PathVariable UUID datasetPublicId,
      @Valid @RequestBody GenerateDatasetRequest request,
      Principal principal) {
    return datasetGenerationService.generateTestCases(
        datasetPublicId, request, principal.getName());
  }

  @Operation(summary = "List datasets", description = "Lists datasets under a project.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Dataset page",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = DatasetPageResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid project identifier, status, or pagination request",
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
        description = "Project not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping(
      value = "/api/v1/projects/{projectPublicId}/datasets",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public DatasetPageResponse listDatasets(
      @PathVariable UUID projectPublicId,
      @RequestParam(required = false) DatasetStatus status,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      Principal principal) {
    return datasetService.listDatasets(projectPublicId, status, pageable, principal.getName());
  }

  @Operation(summary = "Get dataset detail", description = "Returns a dataset owned by the authenticated user.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Dataset detail",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = DatasetResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid dataset identifier",
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
      value = "/api/v1/datasets/{datasetPublicId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public DatasetResponse getDataset(@PathVariable UUID datasetPublicId, Principal principal) {
    return datasetService.getDataset(datasetPublicId, principal.getName());
  }

  @Operation(summary = "Update dataset", description = "Updates a dataset owned by the authenticated user.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Dataset updated",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = DatasetResponse.class))),
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
        description = "Dataset not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PatchMapping(
      value = "/api/v1/datasets/{datasetPublicId}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public DatasetResponse updateDataset(
      @PathVariable UUID datasetPublicId,
      @Valid @RequestBody UpdateDatasetRequest request,
      Principal principal) {
    return datasetService.updateDataset(datasetPublicId, request, principal.getName());
  }

  @Operation(summary = "Delete dataset", description = "Deletes a dataset owned by the authenticated user.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Dataset deleted", content = @Content),
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
  @DeleteMapping("/api/v1/datasets/{datasetPublicId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteDataset(
      @PathVariable UUID datasetPublicId, Principal principal) {
    datasetService.deleteDataset(datasetPublicId, principal.getName());
  }
}
