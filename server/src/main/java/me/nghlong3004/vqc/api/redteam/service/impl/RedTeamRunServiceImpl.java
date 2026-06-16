package me.nghlong3004.vqc.api.redteam.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.judge.entity.JudgeModel;
import me.nghlong3004.vqc.api.judge.repository.JudgeModelRepository;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.job.enums.JobStatus;
import me.nghlong3004.vqc.api.job.enums.JobType;
import me.nghlong3004.vqc.api.job.enums.ResourceType;
import me.nghlong3004.vqc.api.job.repository.JobRepository;
import me.nghlong3004.vqc.api.job.service.JobQueuePublisher;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.repository.ProjectRepository;
import me.nghlong3004.vqc.api.redteam.entity.RedTeamRun;
import me.nghlong3004.vqc.api.redteam.enums.RedTeamRunStatus;
import me.nghlong3004.vqc.api.redteam.repository.RedTeamRunRepository;
import me.nghlong3004.vqc.api.redteam.request.CreateRedTeamRunRequest;
import me.nghlong3004.vqc.api.redteam.response.CreateRedTeamRunResponse;
import me.nghlong3004.vqc.api.redteam.response.RedTeamResultResponse;
import me.nghlong3004.vqc.api.redteam.response.RedTeamRunPageResponse;
import me.nghlong3004.vqc.api.redteam.response.RedTeamRunResponse;
import me.nghlong3004.vqc.api.redteam.service.RedTeamRunService;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.targetconnector.repository.TargetApiConnectorRepository;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedTeamRunServiceImpl implements RedTeamRunService {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
  private static final List<String> DEFAULT_PLUGINS =
      List.of("harmful:privacy", "prompt-extraction", "pii:direct");
  private static final List<String> DEFAULT_STRATEGIES = List.of("basic");

  private final RedTeamRunRepository redTeamRunRepository;
  private final JobRepository jobRepository;
  private final ProjectRepository projectRepository;
  private final TargetApiConnectorRepository targetApiConnectorRepository;
  private final JudgeModelRepository judgeModelRepository;
  private final UserRepository userRepository;
  private final JobQueuePublisher jobQueuePublisher;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public CreateRedTeamRunResponse createRedTeamRun(
      UUID projectPublicId, CreateRedTeamRunRequest request, String username) {
    User creator = findCreator(username);
    Project project =
        projectRepository
            .findByPublicIdAndCreatedBy(projectPublicId, creator)
            .orElseThrow(() -> new ResourceException(ErrorCode.PROJECT_NOT_FOUND));
    TargetApiConnector connector =
        targetApiConnectorRepository
            .findByPublicIdAndCreatedBy(request.targetConnectorPublicId(), creator)
            .filter(c -> c.getProject().getId().equals(project.getId()))
            .orElseThrow(() -> new ResourceException(ErrorCode.TARGET_CONNECTOR_NOT_FOUND));
    if (!Boolean.TRUE.equals(connector.getActive())) {
      throw new ResourceException(ErrorCode.CONNECTOR_NOT_ACTIVE);
    }
    JudgeModel judgeModel = null;
    if (request.judgeModelPublicId() != null) {
      judgeModel =
          judgeModelRepository
              .findByPublicIdAndCreatedBy(request.judgeModelPublicId(), creator)
              .filter(model -> model.getProject().getId().equals(project.getId()))
              .orElseThrow(() -> new ResourceException(ErrorCode.JUDGE_MODEL_NOT_FOUND));
      if (!Boolean.TRUE.equals(judgeModel.getActive())) {
        throw new ResourceException(ErrorCode.JUDGE_MODEL_INACTIVE);
      }
    }

    RedTeamRun run =
        RedTeamRun.builder()
            .project(project)
            .targetApiConnector(connector)
            .judgeModel(judgeModel)
            .name(normalizeName(request.name()))
            .purpose(request.purpose())
            .pluginsJson(toJson(defaultIfEmpty(request.plugins(), DEFAULT_PLUGINS)))
            .strategiesJson(toJson(defaultIfEmpty(request.strategies(), DEFAULT_STRATEGIES)))
            .numTests(request.numTests() == null ? 1 : request.numTests())
            .createdBy(creator)
            .build();
    RedTeamRun savedRun = redTeamRunRepository.save(run);

    Job job =
        Job.builder()
            .jobType(JobType.RED_TEAM_RUN)
            .status(JobStatus.PENDING)
            .resourceType(ResourceType.RED_TEAM_RUN)
            .resourceId(savedRun.getId())
            .project(project)
            .createdBy(creator)
            .progressTotal(1)
            .build();
    Job savedJob = jobRepository.save(job);
    savedRun.setJob(savedJob);
    redTeamRunRepository.save(savedRun);
    jobQueuePublisher.publish(savedJob.getPublicId().toString());

    log.info(
        "Created red-team run {} with job {} for project {}",
        savedRun.getPublicId(),
        savedJob.getPublicId(),
        project.getPublicId());
    return new CreateRedTeamRunResponse(
        savedRun.getPublicId(),
        savedJob.getPublicId(),
        savedRun.getStatus().name(),
        "Red-team run queued successfully.");
  }

  @Override
  @Transactional(readOnly = true)
  public RedTeamRunPageResponse listRedTeamRuns(
      UUID projectPublicId, Pageable pageable, String username) {
    User creator = findCreator(username);
    Project project =
        projectRepository
            .findByPublicIdAndCreatedBy(projectPublicId, creator)
            .orElseThrow(() -> new ResourceException(ErrorCode.PROJECT_NOT_FOUND));
    var page = redTeamRunRepository.findByProjectIdAndCreatedBy(project.getId(), creator, pageable);
    return new RedTeamRunPageResponse(
        page.getContent().stream().map(this::toResponse).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true)
  public RedTeamRunResponse getRedTeamRun(UUID runPublicId, String username) {
    return toResponse(findRun(runPublicId, username));
  }

  @Override
  @Transactional(readOnly = true)
  public RedTeamResultResponse getRedTeamResults(UUID runPublicId, String username) {
    RedTeamRun run = findRun(runPublicId, username);
    if (run.getArtifactDir() == null || run.getArtifactDir().isBlank()) {
      return new RedTeamResultResponse(run.getPublicId().toString(), run.getStatus().name(), Map.of(), null);
    }
    Path resultsPath = Path.of(run.getArtifactDir()).resolve("results.json");
    if (!Files.exists(resultsPath)) {
      return new RedTeamResultResponse(run.getPublicId().toString(), run.getStatus().name(), Map.of(), null);
    }
    try {
      Map<String, Object> raw = objectMapper.readValue(resultsPath.toFile(), MAP_TYPE);
      Object results = raw.get("results");
      Map<String, Object> summary =
          results instanceof Map<?, ?> resultsMap && resultsMap.get("stats") instanceof Map<?, ?> stats
              ? castMap(stats)
              : Map.of();
      return new RedTeamResultResponse(run.getPublicId().toString(), run.getStatus().name(), summary, results);
    } catch (IOException ex) {
      return new RedTeamResultResponse(
          run.getPublicId().toString(),
          run.getStatus().name(),
          Map.of("error", "Failed to read Promptfoo red-team results."),
          null);
    }
  }

  private RedTeamRun findRun(UUID runPublicId, String username) {
    User creator = findCreator(username);
    return redTeamRunRepository
        .findByPublicIdAndCreatedBy(runPublicId, creator)
        .orElseThrow(() -> new ResourceException(ErrorCode.RED_TEAM_RUN_NOT_FOUND));
  }

  private User findCreator(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new ResourceException(ErrorCode.USER_NOT_FOUND));
  }

  private RedTeamRunResponse toResponse(RedTeamRun run) {
    return new RedTeamRunResponse(
        run.getPublicId(),
        run.getProject().getPublicId(),
        run.getTargetApiConnector().getPublicId(),
        run.getTargetApiConnector().getName(),
        run.getJudgeModel() == null ? null : run.getJudgeModel().getPublicId(),
        run.getJudgeModel() == null ? null : run.getJudgeModel().getName(),
        run.getJob() == null ? null : run.getJob().getPublicId(),
        run.getName(),
        run.getPurpose(),
        fromJsonList(run.getPluginsJson()),
        fromJsonList(run.getStrategiesJson()),
        run.getNumTests(),
        run.getStatus(),
        run.getTotalCases(),
        run.getPassedCases(),
        run.getFailedCases(),
        run.getErrorCases(),
        run.getErrorMessage(),
        run.getStartedAt(),
        run.getCompletedAt(),
        run.getCreatedAt(),
        run.getUpdatedAt());
  }

  private String normalizeName(String name) {
    return name == null || name.isBlank() ? "Red-team run" : name.trim();
  }

  private List<String> defaultIfEmpty(List<String> value, List<String> fallback) {
    return value == null || value.isEmpty() ? fallback : value;
  }

  private String toJson(List<String> value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      return "[]";
    }
  }

  private List<String> fromJsonList(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(raw, STRING_LIST_TYPE);
    } catch (IOException ex) {
      return List.of();
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> castMap(Map<?, ?> value) {
    return (Map<String, Object>) value;
  }
}
