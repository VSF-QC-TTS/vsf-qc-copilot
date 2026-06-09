package me.nghlong3004.vqc.api.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.exception.ErrorResponse;
import me.nghlong3004.vqc.api.project.enums.ProjectStatus;
import me.nghlong3004.vqc.api.project.request.CreateProjectRequest;
import me.nghlong3004.vqc.api.project.response.ProjectPageResponse;
import me.nghlong3004.vqc.api.project.response.ProjectResponse;
import me.nghlong3004.vqc.api.project.service.ProjectService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project management APIs")
public class ProjectController {

  private final ProjectService projectService;

  @Operation(summary = "Create project", description = "Creates a project for an evaluation scope.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = CreateProjectRequest.class)))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Project created",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ProjectResponse.class))),
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
        description = "Authenticated user no longer exists",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public ProjectResponse createProject(
      @Valid @RequestBody CreateProjectRequest request, Principal principal) {
    return projectService.createProject(request, principal.getName());
  }

  @Operation(summary = "List projects", description = "Lists projects owned by the authenticated user.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Project page",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ProjectPageResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid filter or pagination request",
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
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ProjectPageResponse listProjects(
      @RequestParam(required = false) ProjectStatus status,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      Principal principal) {
    return projectService.listProjects(status, pageable, principal.getName());
  }
}
