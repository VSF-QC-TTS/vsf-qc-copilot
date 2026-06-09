package me.nghlong3004.vqc.api.targetconnector.controller;

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
import me.nghlong3004.vqc.api.targetconnector.request.CreateTargetApiConnectorRequest;
import me.nghlong3004.vqc.api.targetconnector.response.TargetApiConnectorPageResponse;
import me.nghlong3004.vqc.api.targetconnector.response.TargetApiConnectorResponse;
import me.nghlong3004.vqc.api.targetconnector.service.TargetApiConnectorService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@RestController
@RequestMapping("/api/v1/projects/{projectPublicId}/target-api-connectors")
@RequiredArgsConstructor
@Tag(name = "Target API Connectors", description = "Target chatbot/API connector APIs")
public class TargetApiConnectorController {

  private final TargetApiConnectorService targetApiConnectorService;

  @Operation(summary = "Create target connector", description = "Creates a connector under a project.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Connector created",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TargetApiConnectorResponse.class))),
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
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public TargetApiConnectorResponse createConnector(
      @PathVariable UUID projectPublicId,
      @Valid @RequestBody CreateTargetApiConnectorRequest request,
      Principal principal) {
    return targetApiConnectorService.createConnector(projectPublicId, request, principal.getName());
  }

  @Operation(summary = "List target connectors", description = "Lists connectors under a project.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Connector page",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TargetApiConnectorPageResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid project identifier or pagination request",
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
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public TargetApiConnectorPageResponse listConnectors(
      @PathVariable UUID projectPublicId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      Principal principal) {
    return targetApiConnectorService.listConnectors(projectPublicId, pageable, principal.getName());
  }
}
