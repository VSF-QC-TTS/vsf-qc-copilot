package me.nghlong3004.vqc.api.evaluation.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.dataset.enums.DatasetStatus;
import me.nghlong3004.vqc.api.dataset.repository.DatasetRepository;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationResult;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.evaluation.enums.EvaluationRunStatus;
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;
import me.nghlong3004.vqc.api.evaluation.mapper.EvaluationRunMapper;
import me.nghlong3004.vqc.api.evaluation.repository.EvaluationResultRepository;
import me.nghlong3004.vqc.api.evaluation.repository.EvaluationRunRepository;
import me.nghlong3004.vqc.api.evaluation.request.CreateEvaluationRunRequest;
import me.nghlong3004.vqc.api.evaluation.request.QuickEvaluateRequest;
import me.nghlong3004.vqc.api.evaluation.response.CreateEvaluationRunResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationResultListItemResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationResultPageResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationRunDetailResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationRunListItemResponse;
import me.nghlong3004.vqc.api.evaluation.response.EvaluationRunPageResponse;
import me.nghlong3004.vqc.api.evaluation.service.EvaluationRunService;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.judge.entity.JudgeModel;
import me.nghlong3004.vqc.api.judge.repository.JudgeModelRepository;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.job.enums.JobStatus;
import me.nghlong3004.vqc.api.job.enums.JobType;
import me.nghlong3004.vqc.api.job.enums.ResourceType;
import me.nghlong3004.vqc.api.job.response.JobEventResponse;
import me.nghlong3004.vqc.api.job.repository.JobRepository;
import me.nghlong3004.vqc.api.job.service.JobQueuePublisher;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.repository.ProjectRepository;
import me.nghlong3004.vqc.api.rubric.entity.RubricVersion;
import me.nghlong3004.vqc.api.rubric.enums.RubricVersionStatus;
import me.nghlong3004.vqc.api.rubric.repository.RubricCriterionRepository;
import me.nghlong3004.vqc.api.rubric.repository.RubricVersionRepository;
import me.nghlong3004.vqc.api.review.enums.QcStatus;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.targetconnector.repository.TargetApiConnectorRepository;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;
import me.nghlong3004.vqc.api.testcase.repository.TestCaseRepository;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationRunServiceImpl implements EvaluationRunService {

  private static final int MAX_ACTIVE_TEST_CASES = 100;
  private static final long EVENT_STREAM_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30);
  private static final long EVENT_STREAM_POLL_MS = 1_000L;
  private static final long EVENT_STREAM_HEARTBEAT_MS = 15_000L;

  private final EvaluationRunRepository evaluationRunRepository;
  private final EvaluationResultRepository evaluationResultRepository;
  private final JobRepository jobRepository;
  private final me.nghlong3004.vqc.api.job.repository.JobEventRepository jobEventRepository;
  private final ProjectRepository projectRepository;
  private final DatasetRepository datasetRepository;
  private final RubricVersionRepository rubricVersionRepository;
  private final RubricCriterionRepository rubricCriterionRepository;
  private final TargetApiConnectorRepository targetApiConnectorRepository;
  private final JudgeModelRepository judgeModelRepository;
  private final TestCaseRepository testCaseRepository;
  private final UserRepository userRepository;
  private final JobQueuePublisher jobQueuePublisher;
  private final EvaluationRunMapper evaluationRunMapper;

  @Override
  @Transactional
  public CreateEvaluationRunResponse createEvaluationRun(
      UUID projectPublicId, CreateEvaluationRunRequest request, String username) {

    User creator = findCreator(username);
    Project project = findProject(projectPublicId, creator);

    Dataset dataset = findDataset(request.datasetPublicId(), creator, project);
    validateDatasetApproved(dataset);

    RubricVersion rubricVersion = findRubricVersion(request.rubricVersionPublicId(), creator);
    validateRubricVersionPublished(rubricVersion);

    TargetApiConnector connector =
        findConnector(request.targetConnectorPublicId(), creator, project);
    validateConnectorActive(connector);

    JudgeModel judgeModel = findJudgeModel(request.judgeModelPublicId(), creator, project);
    validateJudgeModelActive(judgeModel);

    long activeCases = testCaseRepository.countByDatasetAndStatus(dataset, TestCaseStatus.ACTIVE);
    validateTestCaseCount(activeCases);

    EvaluationRun run =
        EvaluationRun.builder()
            .project(project)
            .dataset(dataset)
            .rubricVersion(rubricVersion)
            .targetApiConnector(connector)
            .judgeModel(judgeModel)
            .status(EvaluationRunStatus.PENDING)
            .totalCases((int) activeCases)
            .maxConcurrency(request.maxConcurrency() != null ? request.maxConcurrency() : 1)
            .createdBy(creator)
            .build();
    EvaluationRun savedRun = evaluationRunRepository.save(run);

    Job job =
        Job.builder()
            .jobType(JobType.EVALUATION_RUN)
            .status(JobStatus.PENDING)
            .resourceType(ResourceType.EVALUATION_RUN)
            .resourceId(savedRun.getId())
            .project(project)
            .createdBy(creator)
            .progressTotal((int) activeCases)
            .build();
    Job savedJob = jobRepository.save(job);

    savedRun.setJob(savedJob);
    evaluationRunRepository.save(savedRun);

    jobQueuePublisher.publish(savedJob.getPublicId().toString());

    log.info(
        "Created evaluation run {} with job {} for project {} by user {}",
        savedRun.getPublicId(),
        savedJob.getPublicId(),
        project.getPublicId(),
        creator.getPublicId());

    return new CreateEvaluationRunResponse(
        savedRun.getPublicId(),
        savedJob.getPublicId(),
        savedRun.getStatus().name(),
        "Evaluation run queued successfully.");
  }

  @Override
  @Transactional(readOnly = true)
  public EvaluationRunPageResponse listEvaluationRuns(
      UUID projectPublicId, Pageable pageable, String username) {
    User creator = findCreator(username);
    Project project = findProject(projectPublicId, creator);
    Page<EvaluationRun> runs =
        evaluationRunRepository.findByProjectIdAndCreatedBy(project.getId(), creator, pageable);
    List<EvaluationRunListItemResponse> items =
        runs.getContent().stream()
            .map(evaluationRunMapper::toListItemResponse)
            .toList();
    log.info(
        "Listed evaluation runs for project {} by user {} page {} size {}",
        project.getPublicId(),
        creator.getPublicId(),
        runs.getNumber(),
        runs.getSize());
    return new EvaluationRunPageResponse(
        items, runs.getNumber(), runs.getSize(), runs.getTotalElements(), runs.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true)
  public EvaluationRunDetailResponse getEvaluationRun(UUID runPublicId, String username) {
    User creator = findCreator(username);
    EvaluationRun run =
        evaluationRunRepository
            .findByPublicIdAndCreatedBy(runPublicId, creator)
            .orElseThrow(() -> new ResourceException(ErrorCode.EVALUATION_RUN_NOT_FOUND));
    log.info(
        "Loaded evaluation run {} for project {} by user {}",
        run.getPublicId(),
        run.getProject().getPublicId(),
        creator.getPublicId());
    return evaluationRunMapper.toDetailResponse(run);
  }

  @Override
  @Transactional(readOnly = true)
  public EvaluationResultPageResponse listEvaluationResults(
      UUID runPublicId, JudgeStatus judgeStatus, QcStatus qcStatus, Pageable pageable, String username) {
    User creator = findCreator(username);
    EvaluationRun run =
        evaluationRunRepository
            .findByPublicIdAndCreatedBy(runPublicId, creator)
            .orElseThrow(() -> new ResourceException(ErrorCode.EVALUATION_RUN_NOT_FOUND));

    Page<EvaluationResult> results = findResults(run.getId(), judgeStatus, qcStatus, pageable);
    var criteria =
        rubricCriterionRepository.findByRubricVersionOrderBySortOrderAscIdAsc(
            run.getRubricVersion());

    List<EvaluationResultListItemResponse> items =
        results.getContent().stream()
            .map(result -> evaluationRunMapper.toResultListItem(result, criteria))
            .toList();
    log.info(
        "Listed evaluation results for run {} page {} size {}",
        run.getPublicId(),
        results.getNumber(),
        results.getSize());
    return new EvaluationResultPageResponse(
        items, results.getNumber(), results.getSize(),
        results.getTotalElements(), results.getTotalPages());
  }

  private Page<EvaluationResult> findResults(
      Long runId, JudgeStatus judgeStatus, QcStatus qcStatus, Pageable pageable) {
    if (qcStatus == null) {
      return judgeStatus == null
          ? evaluationResultRepository.findByEvaluationRunId(runId, pageable)
          : evaluationResultRepository.findByEvaluationRunIdAndJudgeStatus(
              runId, judgeStatus, pageable);
    }
    if (qcStatus == QcStatus.NOT_REVIEWED) {
      return judgeStatus == null
          ? evaluationResultRepository.findByEvaluationRunIdAndReviewDecisionIsNull(
              runId, pageable)
          : evaluationResultRepository.findByEvaluationRunIdAndJudgeStatusAndReviewDecisionIsNull(
              runId, judgeStatus, pageable);
    }
    return judgeStatus == null
        ? evaluationResultRepository.findByEvaluationRunIdAndReviewDecisionQcStatus(
            runId, qcStatus, pageable)
        : evaluationResultRepository.findByEvaluationRunIdAndJudgeStatusAndReviewDecisionQcStatus(
            runId, judgeStatus, qcStatus, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public java.util.List<me.nghlong3004.vqc.api.job.response.JobEventResponse> listEvaluationRunEvents(
      UUID runPublicId, String username) {
    User creator = findCreator(username);
    EvaluationRun run =
        evaluationRunRepository
            .findByPublicIdAndCreatedBy(runPublicId, creator)
            .orElseThrow(() -> new ResourceException(ErrorCode.EVALUATION_RUN_NOT_FOUND));

    if (run.getJob() == null) {
      return java.util.List.of();
    }

    log.info("Listed events for evaluation run {} job {}", run.getPublicId(), run.getJob().getPublicId());
    return jobEventRepository.findByJobIdOrderByCreatedAtAsc(run.getJob().getId()).stream()
        .map(this::toJobEventResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public SseEmitter streamEvaluationRunEvents(UUID runPublicId, String username) {
    User creator = findCreator(username);
    EvaluationRun run =
        evaluationRunRepository
            .findByPublicIdAndCreatedBy(runPublicId, creator)
            .orElseThrow(() -> new ResourceException(ErrorCode.EVALUATION_RUN_NOT_FOUND));

    SseEmitter emitter = new SseEmitter(EVENT_STREAM_TIMEOUT_MS);
    if (run.getJob() == null) {
      emitter.complete();
      return emitter;
    }

    Long runId = run.getId();
    Long jobId = run.getJob().getId();
    AtomicLong lastEventId = new AtomicLong(0L);

    CompletableFuture.runAsync(
        () -> streamEvents(emitter, runId, jobId, lastEventId, run.getPublicId()));
    return emitter;
  }

  private void streamEvents(
      SseEmitter emitter, Long runId, Long jobId, AtomicLong lastEventId, UUID runPublicId) {
    long lastHeartbeatAt = 0L;
    try {
      sendExistingEvents(emitter, jobId, lastEventId);
      while (true) {
        sendNewEvents(emitter, jobId, lastEventId);
        EvaluationRunStatus status = currentRunStatus(runId);
        if (isTerminal(status)) {
          emitter.complete();
          return;
        }

        long now = System.currentTimeMillis();
        if (now - lastHeartbeatAt >= EVENT_STREAM_HEARTBEAT_MS) {
          emitter.send(SseEmitter.event().comment("keep-alive"));
          lastHeartbeatAt = now;
        }
        Thread.sleep(EVENT_STREAM_POLL_MS);
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      emitter.completeWithError(ex);
    } catch (Exception ex) {
      log.debug("Evaluation event stream closed for run {}", runPublicId, ex);
      emitter.completeWithError(ex);
    }
  }

  private void sendExistingEvents(SseEmitter emitter, Long jobId, AtomicLong lastEventId)
      throws java.io.IOException {
    for (var event : jobEventRepository.findByJobIdOrderByCreatedAtAsc(jobId)) {
      sendJobEvent(emitter, event);
      lastEventId.set(event.getId());
    }
  }

  private void sendNewEvents(SseEmitter emitter, Long jobId, AtomicLong lastEventId)
      throws java.io.IOException {
    for (var event :
        jobEventRepository.findByJobIdAndIdGreaterThanOrderByCreatedAtAscIdAsc(
            jobId, lastEventId.get())) {
      sendJobEvent(emitter, event);
      lastEventId.set(event.getId());
    }
  }

  private void sendJobEvent(SseEmitter emitter, me.nghlong3004.vqc.api.job.entity.JobEvent event)
      throws java.io.IOException {
    emitter.send(
        SseEmitter.event()
            .name("job-event")
            .id(event.getPublicId().toString())
            .data(toJobEventResponse(event)));
  }

  private EvaluationRunStatus currentRunStatus(Long runId) {
    return evaluationRunRepository
        .findById(runId)
        .map(EvaluationRun::getStatus)
        .orElse(EvaluationRunStatus.FAILED);
  }

  private boolean isTerminal(EvaluationRunStatus status) {
    return status == EvaluationRunStatus.COMPLETED
        || status == EvaluationRunStatus.FAILED
        || status == EvaluationRunStatus.CANCELLED;
  }

  private JobEventResponse toJobEventResponse(me.nghlong3004.vqc.api.job.entity.JobEvent event) {
    return new JobEventResponse(
        event.getPublicId(), event.getEventType(), event.getPayloadJson(), event.getCreatedAt());
  }

  // ── Validation helpers ──

  private void validateDatasetApproved(Dataset dataset) {
    if (dataset.getStatus() != DatasetStatus.APPROVED) {
      throw new ResourceException(ErrorCode.DATASET_NOT_APPROVED);
    }
  }

  private void validateRubricVersionPublished(RubricVersion rubricVersion) {
    if (rubricVersion.getStatus() != RubricVersionStatus.PUBLISHED) {
      throw new ResourceException(ErrorCode.RUBRIC_VERSION_NOT_PUBLISHED);
    }
  }

  private void validateConnectorActive(TargetApiConnector connector) {
    if (!connector.getActive()) {
      throw new ResourceException(ErrorCode.CONNECTOR_NOT_ACTIVE);
    }
  }

  private void validateJudgeModelActive(JudgeModel judgeModel) {
    if (!Boolean.TRUE.equals(judgeModel.getActive())) {
      throw new ResourceException(ErrorCode.JUDGE_MODEL_INACTIVE);
    }
  }

  private void validateTestCaseCount(long activeCases) {
    if (activeCases < 1) {
      throw new ResourceException(ErrorCode.DATASET_NO_ACTIVE_CASES);
    }
    if (activeCases > MAX_ACTIVE_TEST_CASES) {
      throw new ResourceException(ErrorCode.DATASET_TOO_MANY_CASES);
    }
  }

  // ── Lookup helpers ──

  private Dataset findDataset(UUID datasetPublicId, User creator, Project project) {
    Dataset dataset =
        datasetRepository
            .findByPublicIdAndCreatedBy(datasetPublicId, creator)
            .orElseThrow(() -> new ResourceException(ErrorCode.DATASET_NOT_FOUND));
    if (!project.getId().equals(dataset.getProject().getId())) {
      throw new ResourceException(ErrorCode.DATASET_NOT_FOUND);
    }
    return dataset;
  }

  private RubricVersion findRubricVersion(UUID rubricVersionPublicId, User creator) {
    return rubricVersionRepository
        .findByPublicIdAndRubricCreatedBy(rubricVersionPublicId, creator)
        .orElseThrow(() -> new ResourceException(ErrorCode.RUBRIC_VERSION_NOT_FOUND));
  }

  private TargetApiConnector findConnector(UUID connectorPublicId, User creator, Project project) {
    TargetApiConnector connector =
        targetApiConnectorRepository
            .findByPublicIdAndCreatedBy(connectorPublicId, creator)
            .orElseThrow(() -> new ResourceException(ErrorCode.TARGET_CONNECTOR_NOT_FOUND));
    if (!project.getId().equals(connector.getProject().getId())) {
      throw new ResourceException(ErrorCode.TARGET_CONNECTOR_NOT_FOUND);
    }
    return connector;
  }

  private JudgeModel findJudgeModel(UUID judgeModelPublicId, User creator, Project project) {
    JudgeModel judgeModel =
        judgeModelRepository
            .findByPublicIdAndCreatedBy(judgeModelPublicId, creator)
            .orElseThrow(() -> new ResourceException(ErrorCode.JUDGE_MODEL_NOT_FOUND));
    if (!project.getId().equals(judgeModel.getProject().getId())) {
      throw new ResourceException(ErrorCode.JUDGE_MODEL_NOT_FOUND);
    }
    return judgeModel;
  }

  private Project findProject(UUID projectPublicId, User creator) {
    return projectRepository
        .findByPublicIdAndCreatedBy(projectPublicId, creator)
        .orElseThrow(() -> new ResourceException(ErrorCode.PROJECT_NOT_FOUND));
  }

  private User findCreator(String username) {
    return userRepository
        .findByUsername(username.trim().toLowerCase())
        .orElseThrow(() -> new ResourceException(ErrorCode.USER_NOT_FOUND));
  }

  @Override
  @Transactional
  public CreateEvaluationRunResponse quickEvaluate(
      UUID projectPublicId, QuickEvaluateRequest request, String username) {

    User creator = findCreator(username);
    Project project = findProject(projectPublicId, creator);

    UUID datasetId = request.datasetPublicId();
    if (datasetId == null) {
      Page<Dataset> approved =
          datasetRepository.findByProjectAndStatus(
              project, DatasetStatus.APPROVED, PageRequest.of(0, 2));
      if (approved.getTotalElements() != 1) {
        throw new ResourceException(ErrorCode.QUICK_EVALUATE_AMBIGUOUS);
      }
      datasetId = approved.getContent().getFirst().getPublicId();
    }

    UUID connectorId = request.targetConnectorPublicId();
    if (connectorId == null) {
      Page<TargetApiConnector> connectors =
          targetApiConnectorRepository.findByProject(project, PageRequest.of(0, 2));
      if (connectors.getTotalElements() != 1) {
        throw new ResourceException(ErrorCode.QUICK_EVALUATE_AMBIGUOUS);
      }
      connectorId = connectors.getContent().getFirst().getPublicId();
    }

    UUID judgeModelId = request.judgeModelPublicId();
    if (judgeModelId == null) {
      Page<JudgeModel> judgeModels =
          judgeModelRepository.findByProjectAndActive(project, true, PageRequest.of(0, 2));
      if (judgeModels.getTotalElements() != 1) {
        throw new ResourceException(ErrorCode.QUICK_EVALUATE_AMBIGUOUS);
      }
      judgeModelId = judgeModels.getContent().getFirst().getPublicId();
    }

    UUID rubricVersionId = request.rubricVersionPublicId();
    if (rubricVersionId == null) {
      Page<RubricVersion> versions =
          rubricVersionRepository.findByRubricCreatedByAndStatus(
              creator, RubricVersionStatus.PUBLISHED, PageRequest.of(0, 2));
      if (versions.getTotalElements() != 1) {
        throw new ResourceException(ErrorCode.QUICK_EVALUATE_AMBIGUOUS);
      }
      rubricVersionId = versions.getContent().getFirst().getPublicId();
    }

    CreateEvaluationRunRequest resolved =
        new CreateEvaluationRunRequest(
            datasetId,
            rubricVersionId,
            connectorId,
            judgeModelId,
            request.maxConcurrency());

    return createEvaluationRun(projectPublicId, resolved, username);
  }
}
