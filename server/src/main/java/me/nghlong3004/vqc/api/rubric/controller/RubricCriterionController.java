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
import me.nghlong3004.vqc.api.rubric.request.CreateRubricCriterionRequest;
import me.nghlong3004.vqc.api.rubric.request.UpdateRubricCriterionRequest;
import me.nghlong3004.vqc.api.rubric.response.RubricCriterionPageResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricCriterionResponse;
import me.nghlong3004.vqc.api.rubric.service.RubricCriterionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Rubric Criteria", description = "Rubric criterion APIs")
public class RubricCriterionController {

  private final RubricCriterionService rubricCriterionService;

  @Operation(summary = "Create rubric criterion", description = "Creates a criterion under a draft rubric version.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Rubric criterion created",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RubricCriterionResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Version immutable or metric key conflict",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(
      value = "/api/v1/rubric-versions/{rubricVersionPublicId}/criteria",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public RubricCriterionResponse createCriterion(
      @PathVariable UUID rubricVersionPublicId,
      @Valid @RequestBody CreateRubricCriterionRequest request,
      Principal principal) {
    return rubricCriterionService.createCriterion(
        rubricVersionPublicId, request, principal.getName());
  }

  @Operation(summary = "List rubric criteria", description = "Lists criteria under a rubric version.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Rubric criterion page",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RubricCriterionPageResponse.class)))
  })
  @GetMapping(
      value = "/api/v1/rubric-versions/{rubricVersionPublicId}/criteria",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public RubricCriterionPageResponse listCriteria(
      @PathVariable UUID rubricVersionPublicId,
      @PageableDefault(size = 100, sort = "sortOrder", direction = Sort.Direction.ASC)
          Pageable pageable,
      Principal principal) {
    return rubricCriterionService.listCriteria(
        rubricVersionPublicId, pageable, principal.getName());
  }

  @Operation(summary = "Update rubric criterion", description = "Updates a criterion under a draft rubric version.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Rubric criterion updated",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RubricCriterionResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Version immutable or metric key conflict",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PatchMapping(
      value = "/api/v1/rubric-criteria/{criterionPublicId}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public RubricCriterionResponse updateCriterion(
      @PathVariable UUID criterionPublicId,
      @Valid @RequestBody UpdateRubricCriterionRequest request,
      Principal principal) {
    return rubricCriterionService.updateCriterion(criterionPublicId, request, principal.getName());
  }

  @Operation(summary = "Delete rubric criterion", description = "Deletes a criterion under a draft rubric version.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Rubric criterion deleted", content = @Content),
    @ApiResponse(
        responseCode = "409",
        description = "Version immutable",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @DeleteMapping("/api/v1/rubric-criteria/{criterionPublicId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteCriterion(@PathVariable UUID criterionPublicId, Principal principal) {
    rubricCriterionService.deleteCriterion(criterionPublicId, principal.getName());
  }
}
