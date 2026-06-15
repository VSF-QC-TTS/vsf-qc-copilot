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
import me.nghlong3004.vqc.api.rubric.enums.RubricVersionStatus;
import me.nghlong3004.vqc.api.rubric.request.CreateRubricVersionRequest;
import me.nghlong3004.vqc.api.rubric.request.UpdateRubricVersionRequest;
import me.nghlong3004.vqc.api.rubric.response.RubricVersionPageResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricVersionResponse;
import me.nghlong3004.vqc.api.rubric.service.RubricVersionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@Tag(name = "Rubric Versions", description = "Rubric version APIs")
public class RubricVersionController {

  private final RubricVersionService rubricVersionService;

  @Operation(summary = "Create rubric version", description = "Creates the next draft version for a rubric.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Rubric version created",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RubricVersionResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Rubric archived",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(
      value = "/api/v1/rubrics/{rubricPublicId}/versions",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public RubricVersionResponse createVersion(
      @PathVariable UUID rubricPublicId,
      @RequestBody(required = false) CreateRubricVersionRequest request,
      Principal principal) {
    UUID sourceVersionPublicId = request == null ? null : request.sourceVersionPublicId();
    return rubricVersionService.createVersion(
        rubricPublicId, sourceVersionPublicId, principal.getName());
  }

  @Operation(summary = "List rubric versions", description = "Lists versions under a rubric.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Rubric version page",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RubricVersionPageResponse.class)))
  })
  @GetMapping(
      value = "/api/v1/rubrics/{rubricPublicId}/versions",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public RubricVersionPageResponse listVersions(
      @PathVariable UUID rubricPublicId,
      @RequestParam(required = false) RubricVersionStatus status,
      @PageableDefault(size = 20, sort = "version", direction = Sort.Direction.DESC)
          Pageable pageable,
      Principal principal) {
    return rubricVersionService.listVersions(rubricPublicId, status, pageable, principal.getName());
  }

  @Operation(
      summary = "List user rubric versions",
      description = "Lists rubric versions owned by the authenticated user, optionally filtered by status.")
  @GetMapping(value = "/api/v1/rubric-versions", produces = MediaType.APPLICATION_JSON_VALUE)
  public RubricVersionPageResponse listUserVersions(
      @RequestParam(required = false) RubricVersionStatus status,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      Principal principal) {
    return rubricVersionService.listUserVersions(status, pageable, principal.getName());
  }

  @Operation(summary = "Get rubric version detail", description = "Returns a rubric version with criteria.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Rubric version detail",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RubricVersionResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Rubric version not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping(
      value = "/api/v1/rubric-versions/{rubricVersionPublicId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public RubricVersionResponse getVersion(
      @PathVariable UUID rubricVersionPublicId, Principal principal) {
    return rubricVersionService.getVersion(rubricVersionPublicId, principal.getName());
  }

  @Operation(summary = "Update rubric version", description = "Publishes or archives a rubric version.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Rubric version updated",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RubricVersionResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Rubric version immutable",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "422",
        description = "Rubric version cannot be published",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PatchMapping(
      value = "/api/v1/rubric-versions/{rubricVersionPublicId}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public RubricVersionResponse updateVersion(
      @PathVariable UUID rubricVersionPublicId,
      @Valid @RequestBody UpdateRubricVersionRequest request,
      Principal principal) {
    return rubricVersionService.updateVersion(rubricVersionPublicId, request, principal.getName());
  }
}
