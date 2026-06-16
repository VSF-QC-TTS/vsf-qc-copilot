package me.nghlong3004.vqc.api.redteam.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.redteam.request.CreateRedTeamRunRequest;
import me.nghlong3004.vqc.api.redteam.response.CreateRedTeamRunResponse;
import me.nghlong3004.vqc.api.redteam.response.RedTeamResultResponse;
import me.nghlong3004.vqc.api.redteam.response.RedTeamRunPageResponse;
import me.nghlong3004.vqc.api.redteam.response.RedTeamRunResponse;
import me.nghlong3004.vqc.api.redteam.service.RedTeamRunService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@RestController
@RequiredArgsConstructor
public class RedTeamRunController {

  private final RedTeamRunService redTeamRunService;

  @PostMapping(
      value = "/api/v1/projects/{projectPublicId}/red-team-runs",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public CreateRedTeamRunResponse createRedTeamRun(
      @PathVariable UUID projectPublicId,
      @Valid @RequestBody CreateRedTeamRunRequest request,
      Principal principal) {
    return redTeamRunService.createRedTeamRun(projectPublicId, request, principal.getName());
  }

  @GetMapping(
      value = "/api/v1/projects/{projectPublicId}/red-team-runs",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public RedTeamRunPageResponse listRedTeamRuns(
      @PathVariable UUID projectPublicId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      Principal principal) {
    return redTeamRunService.listRedTeamRuns(projectPublicId, pageable, principal.getName());
  }

  @GetMapping(value = "/api/v1/red-team-runs/{runPublicId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public RedTeamRunResponse getRedTeamRun(@PathVariable UUID runPublicId, Principal principal) {
    return redTeamRunService.getRedTeamRun(runPublicId, principal.getName());
  }

  @GetMapping(
      value = "/api/v1/red-team-runs/{runPublicId}/results",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public RedTeamResultResponse getRedTeamResults(
      @PathVariable UUID runPublicId, Principal principal) {
    return redTeamRunService.getRedTeamResults(runPublicId, principal.getName());
  }
}
