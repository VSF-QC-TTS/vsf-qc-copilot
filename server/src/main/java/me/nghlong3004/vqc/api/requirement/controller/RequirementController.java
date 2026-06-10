package me.nghlong3004.vqc.api.requirement.controller;

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
import me.nghlong3004.vqc.api.requirement.enums.RequirementStatus;
import me.nghlong3004.vqc.api.requirement.request.CreateRequirementRequest;
import me.nghlong3004.vqc.api.requirement.response.RequirementPageResponse;
import me.nghlong3004.vqc.api.requirement.response.RequirementResponse;
import me.nghlong3004.vqc.api.requirement.service.RequirementService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@Tag(name = "Requirements", description = "Business requirement APIs")
public class RequirementController {

  private final RequirementService requirementService;

  @Operation(summary = "Create requirement", description = "Creates a business requirement under a project.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Requirement created",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RequirementResponse.class))),
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
      value = "/api/v1/projects/{projectPublicId}/requirements",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public RequirementResponse createRequirement(
      @PathVariable UUID projectPublicId,
      @Valid @RequestBody CreateRequirementRequest request,
      Principal principal) {
    return requirementService.createRequirement(projectPublicId, request, principal.getName());
  }

  @Operation(summary = "List requirements", description = "Lists business requirements under a project.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Requirement page",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RequirementPageResponse.class))),
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
      value = "/api/v1/projects/{projectPublicId}/requirements",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public RequirementPageResponse listRequirements(
      @PathVariable UUID projectPublicId,
      @RequestParam(required = false) RequirementStatus status,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      Principal principal) {
    return requirementService.listRequirements(projectPublicId, status, pageable, principal.getName());
  }

  @Operation(summary = "Get requirement detail", description = "Returns a requirement owned by the authenticated user.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Requirement detail",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RequirementResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid requirement identifier",
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
        description = "Requirement not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping(
      value = "/api/v1/requirements/{requirementPublicId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public RequirementResponse getRequirement(
      @PathVariable UUID requirementPublicId, Principal principal) {
    return requirementService.getRequirement(requirementPublicId, principal.getName());
  }
}
