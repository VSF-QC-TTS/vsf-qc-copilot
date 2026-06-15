package me.nghlong3004.vqc.api.rubric.controller;

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
import me.nghlong3004.vqc.api.rubric.enums.RubricStatus;
import me.nghlong3004.vqc.api.rubric.request.CreateRubricRequest;
import me.nghlong3004.vqc.api.rubric.request.CreateRubricWithVersionRequest;
import me.nghlong3004.vqc.api.rubric.request.GenerateRubricPreviewRequest;
import me.nghlong3004.vqc.api.rubric.request.UpdateRubricRequest;
import me.nghlong3004.vqc.api.rubric.response.GenerateRubricPreviewResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricPageResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricVersionResponse;
import me.nghlong3004.vqc.api.rubric.service.RubricGenerationService;
import me.nghlong3004.vqc.api.rubric.service.RubricService;
import me.nghlong3004.vqc.api.rubric.service.RubricWorkflowService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Rubrics", description = "Rubric APIs")
public class RubricController {

  private final RubricService rubricService;
  private final RubricWorkflowService rubricWorkflowService;
  private final RubricGenerationService rubricGenerationService;

  @Operation(
      summary = "Generate rubric preview",
      description = "Generates an editable rubric preview without persisting it.")
  @PostMapping(
      value = "/api/v1/rubrics/generate-preview",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public GenerateRubricPreviewResponse generateRubricPreview(
      @Valid @RequestBody GenerateRubricPreviewRequest request, Principal principal) {
    return rubricGenerationService.generatePreview(request, principal.getName());
  }

  @Operation(
      summary = "Create user-scoped rubric with draft version",
      description = "Creates a reusable rubric and its first draft version in one request.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Rubric and first draft version created",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RubricVersionResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Validation failed",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(
      value = "/api/v1/rubrics",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public RubricVersionResponse createUserScopedRubric(
      @Valid @RequestBody CreateRubricWithVersionRequest request, Principal principal) {
    return rubricWorkflowService.createRubricWithVersion(request, principal.getName());
  }

  @Operation(summary = "Create rubric", description = "Creates a rubric under a project.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Rubric created",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RubricResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Validation failed",
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
      value = "/api/v1/projects/{projectPublicId}/rubrics",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public RubricResponse createRubric(
      @PathVariable UUID projectPublicId,
      @Valid @RequestBody CreateRubricRequest request,
      Principal principal) {
    return rubricService.createRubric(projectPublicId, request, principal.getName());
  }

  @Operation(summary = "List rubrics", description = "Lists rubrics under a project.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Rubric page",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RubricPageResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Project not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping(
      value = "/api/v1/projects/{projectPublicId}/rubrics",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public RubricPageResponse listRubrics(
      @PathVariable UUID projectPublicId,
      @RequestParam(required = false) RubricStatus status,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      Principal principal) {
    return rubricService.listRubrics(projectPublicId, status, pageable, principal.getName());
  }

  @Operation(summary = "Get rubric detail", description = "Returns a rubric owned by the authenticated user.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Rubric detail",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RubricResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Rubric not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping(value = "/api/v1/rubrics/{rubricPublicId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public RubricResponse getRubric(@PathVariable UUID rubricPublicId, Principal principal) {
    return rubricService.getRubric(rubricPublicId, principal.getName());
  }

  @Operation(summary = "Update rubric", description = "Updates rubric metadata.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Rubric updated",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RubricResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Rubric archived",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PatchMapping(
      value = "/api/v1/rubrics/{rubricPublicId}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public RubricResponse updateRubric(
      @PathVariable UUID rubricPublicId,
      @Valid @RequestBody UpdateRubricRequest request,
      Principal principal) {
    return rubricService.updateRubric(rubricPublicId, request, principal.getName());
  }

  @Operation(summary = "Archive rubric", description = "Soft archives a rubric and keeps versions for audit.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Rubric archived", content = @Content),
    @ApiResponse(
        responseCode = "404",
        description = "Rubric not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @DeleteMapping("/api/v1/rubrics/{rubricPublicId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void archiveRubric(@PathVariable UUID rubricPublicId, Principal principal) {
    rubricService.archiveRubric(rubricPublicId, principal.getName());
  }

  @Operation(
      summary = "List my rubrics",
      description = "Lists all rubrics owned by the authenticated user (user-scoped, not project-scoped).")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Rubric page",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RubricPageResponse.class)))
  })
  @GetMapping(value = "/api/v1/rubrics", produces = MediaType.APPLICATION_JSON_VALUE)
  public RubricPageResponse listMyRubrics(
      @RequestParam(required = false) RubricStatus status,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      Principal principal) {
    return rubricService.listMyRubrics(status, pageable, principal.getName());
  }

  @Operation(
      summary = "List rubric templates",
      description = "Lists system-provided rubric templates available for cloning.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Template page",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RubricPageResponse.class)))
  })
  @GetMapping(value = "/api/v1/rubrics/templates", produces = MediaType.APPLICATION_JSON_VALUE)
  public RubricPageResponse listTemplates(
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      Principal principal) {
    return rubricService.listTemplates(pageable, principal.getName());
  }

  @Operation(
      summary = "Clone rubric",
      description = "Creates a copy of a rubric (own or template) for the authenticated user.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Rubric cloned",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RubricResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Source rubric not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(
      value = "/api/v1/rubrics/{rubricPublicId}/clone",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public RubricResponse cloneRubric(@PathVariable UUID rubricPublicId, Principal principal) {
    return rubricService.cloneRubric(rubricPublicId, principal.getName());
  }
}
