package me.nghlong3004.vqc.api.evaluation.controller;

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
import me.nghlong3004.vqc.api.evaluation.request.CreateEvaluationRunRequest;
import me.nghlong3004.vqc.api.evaluation.response.CreateEvaluationRunResponse;
import me.nghlong3004.vqc.api.evaluation.service.EvaluationRunService;
import me.nghlong3004.vqc.api.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
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
@Tag(name = "Evaluation Runs", description = "Evaluation run APIs")
public class EvaluationRunController {

  private final EvaluationRunService evaluationRunService;

  @Operation(
      summary = "Create evaluation run",
      description = "Creates an evaluation run and queues it for async processing.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "202",
        description = "Evaluation run queued",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = CreateEvaluationRunResponse.class))),
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
        description = "Project, dataset, rubric version, or connector not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "422",
        description = "Business rule violation",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(
      value = "/api/v1/projects/{projectPublicId}/evaluation-runs",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public CreateEvaluationRunResponse createEvaluationRun(
      @PathVariable UUID projectPublicId,
      @Valid @RequestBody CreateEvaluationRunRequest request,
      Principal principal) {
    return evaluationRunService.createEvaluationRun(
        projectPublicId, request, principal.getName());
  }
}
