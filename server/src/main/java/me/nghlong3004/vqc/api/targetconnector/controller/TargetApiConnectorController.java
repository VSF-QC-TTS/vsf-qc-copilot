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
import me.nghlong3004.vqc.api.targetconnector.request.UpdateTargetApiConnectorRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
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
  @PostMapping(
      value = "/api/v1/projects/{projectPublicId}/target-api-connectors",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
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
  @GetMapping(
      value = "/api/v1/projects/{projectPublicId}/target-api-connectors",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public TargetApiConnectorPageResponse listConnectors(
      @PathVariable UUID projectPublicId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      Principal principal) {
    return targetApiConnectorService.listConnectors(projectPublicId, pageable, principal.getName());
  }

  @Operation(summary = "Get target connector detail", description = "Returns a connector owned by the authenticated user.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Connector detail",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TargetApiConnectorResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid connector identifier",
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
        description = "Connector not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping(value = "/api/v1/target-api-connectors/{connectorPublicId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public TargetApiConnectorResponse getConnector(
      @PathVariable UUID connectorPublicId, Principal principal) {
    return targetApiConnectorService.getConnector(connectorPublicId, principal.getName());
  }

  @Operation(summary = "Update target connector", description = "Updates a connector owned by the authenticated user.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Connector updated",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TargetApiConnectorResponse.class))),
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
        description = "Connector not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PatchMapping(
      value = "/api/v1/target-api-connectors/{connectorPublicId}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public TargetApiConnectorResponse updateConnector(
      @PathVariable UUID connectorPublicId,
      @Valid @RequestBody UpdateTargetApiConnectorRequest request,
      Principal principal) {
    return targetApiConnectorService.updateConnector(
        connectorPublicId, request, principal.getName());
  }
}
