package me.nghlong3004.vqc.api.export.controller;

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
import me.nghlong3004.vqc.api.export.request.CreateExportRequest;
import me.nghlong3004.vqc.api.export.response.CreateExportResponse;
import me.nghlong3004.vqc.api.export.response.ExportDownloadResponse;
import me.nghlong3004.vqc.api.export.response.ExportFileResponse;
import me.nghlong3004.vqc.api.export.service.ExportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Exports", description = "Export APIs")
public class ExportController {

  private final ExportService exportService;

  @Operation(summary = "Create export", description = "Creates an async export job for an evaluation run.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "202",
        description = "Export accepted",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = CreateExportResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Validation failed",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Evaluation run not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(
      value = "/api/v1/evaluation-runs/{runPublicId}/exports",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public CreateExportResponse createExport(
      @PathVariable UUID runPublicId,
      @Valid @RequestBody CreateExportRequest request,
      Principal principal) {
    return exportService.createExport(runPublicId, request, principal.getName());
  }

  @Operation(summary = "Get export detail", description = "Returns export metadata by public id.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Export detail",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ExportFileResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Export not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping(value = "/api/v1/exports/{exportPublicId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ExportFileResponse getExport(@PathVariable UUID exportPublicId, Principal principal) {
    return exportService.getExport(exportPublicId, principal.getName());
  }

  @Operation(summary = "Download export file", description = "Downloads a READY export file.")
  @GetMapping(value = "/api/v1/exports/{exportPublicId}/file")
  public ResponseEntity<Resource> downloadExport(
      @PathVariable UUID exportPublicId, Principal principal) {
    ExportDownloadResponse download =
        exportService.downloadExport(exportPublicId, principal.getName());
    return ResponseEntity.ok()
        .contentType(download.mediaType())
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.fileName() + "\"")
        .body(download.resource());
  }
}
