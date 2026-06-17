package me.nghlong3004.vqc.api.judge.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.judge.request.CreateJudgeModelRequest;
import me.nghlong3004.vqc.api.judge.request.UpdateJudgeModelRequest;
import me.nghlong3004.vqc.api.judge.response.JudgeModelPageResponse;
import me.nghlong3004.vqc.api.judge.response.JudgeModelResponse;
import me.nghlong3004.vqc.api.judge.service.JudgeModelService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@RestController
@RequiredArgsConstructor
public class JudgeModelController {

  private final JudgeModelService judgeModelService;

  @Operation(summary = "Create judge model")
  @PostMapping(
      value = "/api/v1/projects/{projectPublicId}/judge-models",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public JudgeModelResponse createJudgeModel(
      @PathVariable UUID projectPublicId,
      @Valid @RequestBody CreateJudgeModelRequest request,
      Principal principal) {
    return judgeModelService.createJudgeModel(projectPublicId, request, principal.getName());
  }

  @Operation(summary = "List judge models")
  @GetMapping(
      value = "/api/v1/projects/{projectPublicId}/judge-models",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public JudgeModelPageResponse listJudgeModels(
      @PathVariable UUID projectPublicId,
      @RequestParam(required = false) Boolean active,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      Principal principal) {
    return judgeModelService.listJudgeModels(projectPublicId, active, pageable, principal.getName());
  }

  @Operation(summary = "Update judge model")
  @PatchMapping(
      value = "/api/v1/judge-models/{judgeModelPublicId}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public JudgeModelResponse updateJudgeModel(
      @PathVariable UUID judgeModelPublicId,
      @Valid @RequestBody UpdateJudgeModelRequest request,
      Principal principal) {
    return judgeModelService.updateJudgeModel(judgeModelPublicId, request, principal.getName());
  }

  @Operation(summary = "Test judge model connection")
  @PostMapping(
      value = "/api/v1/judge-models/{judgeModelPublicId}/test-connection",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public JudgeModelResponse testConnection(
      @PathVariable UUID judgeModelPublicId, Principal principal) {
    return judgeModelService.testConnection(judgeModelPublicId, principal.getName());
  }

  @Operation(summary = "Delete judge model")
  @DeleteMapping("/api/v1/judge-models/{judgeModelPublicId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteJudgeModel(
      @PathVariable UUID judgeModelPublicId, Principal principal) {
    judgeModelService.deleteJudgeModel(judgeModelPublicId, principal.getName());
  }
}
