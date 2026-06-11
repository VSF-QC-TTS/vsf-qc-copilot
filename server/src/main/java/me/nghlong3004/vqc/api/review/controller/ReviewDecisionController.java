package me.nghlong3004.vqc.api.review.controller;

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
import me.nghlong3004.vqc.api.review.request.UpsertReviewDecisionRequest;
import me.nghlong3004.vqc.api.review.response.ReviewDecisionResponse;
import me.nghlong3004.vqc.api.review.service.ReviewDecisionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Review Decisions", description = "QC review decision APIs")
public class ReviewDecisionController {

  private final ReviewDecisionService reviewDecisionService;

  @Operation(
      summary = "Upsert review decision",
      description = "Creates or updates a QC review decision for an evaluation result.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Review decision",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ReviewDecisionResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Validation failed",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Evaluation result or PIC bug user not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PutMapping(
      value = "/api/v1/evaluation-results/{resultPublicId}/review-decision",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ReviewDecisionResponse upsertReviewDecision(
      @PathVariable UUID resultPublicId,
      @Valid @RequestBody UpsertReviewDecisionRequest request,
      Principal principal) {
    return reviewDecisionService.upsertReviewDecision(
        resultPublicId, request, principal.getName());
  }
}
