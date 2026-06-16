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
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;
import me.nghlong3004.vqc.api.evaluation.request.CreateEvaluationRunRequest;
import me.nghlong3004.vqc.api.evaluation.request.QuickEvaluateRequest;
import me.nghlong3004.vqc.api.evaluation.response.CreateEvaluationRunResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationResultPageResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationRunDetailResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationRunPageResponse;
import me.nghlong3004.vqc.api.evaluation.service.EvaluationRunService;
import me.nghlong3004.vqc.api.exception.ErrorResponse;
import me.nghlong3004.vqc.api.review.enums.QcStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

  @Operation(
      summary = "List evaluation runs",
      description = "Lists evaluation runs under a project.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Evaluation run page",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = EvaluationRunPageResponse.class))),
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
      value = "/api/v1/projects/{projectPublicId}/evaluation-runs",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public EvaluationRunPageResponse listEvaluationRuns(
      @PathVariable UUID projectPublicId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      Principal principal) {
    return evaluationRunService.listEvaluationRuns(
        projectPublicId, pageable, principal.getName());
  }

  @Operation(
      summary = "Get evaluation run detail",
      description = "Returns a single evaluation run by public identifier.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Evaluation run detail",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = EvaluationRunDetailResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Authentication is required",
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
  @GetMapping(
      value = "/api/v1/evaluation-runs/{runPublicId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public EvaluationRunDetailResponse getEvaluationRun(
      @PathVariable UUID runPublicId, Principal principal) {
    return evaluationRunService.getEvaluationRun(runPublicId, principal.getName());
  }

  @Operation(
      summary = "List evaluation results",
      description = "Lists results for an evaluation run, optionally filtered by judge status.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Evaluation results page",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = EvaluationResultPageResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Evaluation run not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping(
      value = "/api/v1/evaluation-runs/{runPublicId}/results",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public EvaluationResultPageResponse listEvaluationResults(
      @PathVariable UUID runPublicId,
      @RequestParam(required = false) JudgeStatus judgeStatus,
      @RequestParam(required = false) QcStatus qcStatus,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC)
          Pageable pageable,
      Principal principal) {
    return evaluationRunService.listEvaluationResults(
        runPublicId, judgeStatus, qcStatus, pageable, principal.getName());
  }

  @Operation(
      summary = "List evaluation run events",
      description = "Returns job events for an evaluation run in chronological order.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Event list",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = me.nghlong3004.vqc.api.job.response.JobEventResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Evaluation run not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping(
      value = "/api/v1/evaluation-runs/{runPublicId}/events",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public java.util.List<me.nghlong3004.vqc.api.job.response.JobEventResponse> listEvaluationRunEvents(
      @PathVariable UUID runPublicId, Principal principal) {
    return evaluationRunService.listEvaluationRunEvents(runPublicId, principal.getName());
  }

  @Operation(
      summary = "Stream evaluation run events",
      description = "Streams job events for an evaluation run using Server-Sent Events.")
  @GetMapping(
      value = "/api/v1/evaluation-runs/{runPublicId}/events/stream",
      produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamEvaluationRunEvents(
      @PathVariable UUID runPublicId, Principal principal) {
    return evaluationRunService.streamEvaluationRunEvents(runPublicId, principal.getName());
  }

  @Operation(
      summary = "Quick evaluate",
      description =
          "Starts an evaluation run with auto-resolved defaults. Null fields are resolved to "
              + "the sole candidate in the project; returns 422 if 0 or >1 candidates exist.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "202",
        description = "Evaluation run queued",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = CreateEvaluationRunResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Project not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "422",
        description = "Auto-resolve failed or business rule violation",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(
      value = "/api/v1/projects/{projectPublicId}/quick-evaluate",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public CreateEvaluationRunResponse quickEvaluate(
      @PathVariable UUID projectPublicId,
      @Valid @RequestBody QuickEvaluateRequest request,
      Principal principal) {
    return evaluationRunService.quickEvaluate(
        projectPublicId, request, principal.getName());
  }
}
