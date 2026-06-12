package me.nghlong3004.vqc.api.evaluation.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationResult;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.evaluation.enums.EvaluationRunStatus;
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;
import me.nghlong3004.vqc.api.evaluation.executor.PromptfooExecutor;
import me.nghlong3004.vqc.api.evaluation.executor.PromptfooResult;
import me.nghlong3004.vqc.api.evaluation.repository.EvaluationResultRepository;
import me.nghlong3004.vqc.api.evaluation.repository.EvaluationRunRepository;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.job.entity.JobEvent;
import me.nghlong3004.vqc.api.job.enums.JobStatus;
import me.nghlong3004.vqc.api.job.enums.JobType;
import me.nghlong3004.vqc.api.job.repository.JobEventRepository;
import me.nghlong3004.vqc.api.job.repository.JobRepository;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;
import me.nghlong3004.vqc.api.testcase.repository.TestCaseRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EvaluationJobHandler {

  private final JobRepository jobRepository;
  private final JobEventRepository jobEventRepository;
  private final EvaluationRunRepository evaluationRunRepository;
  private final EvaluationResultRepository evaluationResultRepository;
  private final TestCaseRepository testCaseRepository;
  private final PromptfooExecutor promptfooExecutor;
  private final ObjectMapper objectMapper;

  /**
   * Handles one queued evaluation job.
   *
   * @param jobPublicId public job identifier
   */
  @Transactional
  public void handle(UUID jobPublicId) {
    Job job = jobRepository.findByPublicId(jobPublicId).orElse(null);
    if (job == null) {
      log.warn("Evaluation job {} was queued but no longer exists", jobPublicId);
      return;
    }
    if (job.getJobType() != JobType.EVALUATION_RUN) {
      failJob(job, null, "Unsupported job type: " + job.getJobType());
      return;
    }

    EvaluationRun run = evaluationRunRepository.findById(job.getResourceId()).orElse(null);
    if (run == null) {
      failJob(job, null, "Evaluation run resource is missing.");
      return;
    }

    try {
      start(job, run);
      List<TestCase> testCases =
          testCaseRepository.findByDatasetAndStatusOrderBySortOrderAscIdAsc(
              run.getDataset(), TestCaseStatus.ACTIVE);
      if (testCases.isEmpty()) {
        throw new IllegalStateException("Evaluation run has no active test cases.");
      }

      List<PromptfooResult> promptfooResults =
          promptfooExecutor.evaluate(run, testCases);
      Map<Long, PromptfooResult> resultsByTestCaseId =
          promptfooResults.stream()
              .filter(result -> result.testCaseId() != null)
              .collect(Collectors.toMap(PromptfooResult::testCaseId, Function.identity(), (a, b) -> b));

      int completed = 0;
      for (TestCase testCase : testCases) {
        PromptfooResult promptfooResult = resultsByTestCaseId.get(testCase.getId());
        EvaluationResult result = toEvaluationResult(run, testCase, promptfooResult);
        evaluationResultRepository.save(result);
        completed++;
        job.setProgressCurrent(completed);
        jobRepository.save(job);
        emitEvent(
            job,
            "CASE_COMPLETED",
            Map.of(
                "completed", completed,
                "total", testCases.size(),
                "testCasePublicId", testCase.getPublicId()));
      }

      complete(job, run, testCases.size());
      log.info("Completed evaluation job {} for run {}", job.getPublicId(), run.getPublicId());
    } catch (Exception ex) {
      failJob(job, run, ex.getMessage());
      log.error("Failed evaluation job {} for run {}", job.getPublicId(), run.getPublicId(), ex);
    }
  }

  private void start(Job job, EvaluationRun run) {
    OffsetDateTime now = OffsetDateTime.now();
    job.setStatus(JobStatus.RUNNING);
    job.setStartedAt(now);
    job.setErrorMessage(null);
    run.setStatus(EvaluationRunStatus.RUNNING);
    run.setStartedAt(now);
    jobRepository.save(job);
    evaluationRunRepository.save(run);
    emitEvent(job, "RUNNING", Map.of("runPublicId", run.getPublicId()));
  }

  private EvaluationResult toEvaluationResult(
      EvaluationRun run, TestCase testCase, PromptfooResult promptfooResult) {
    if (promptfooResult == null) {
      return EvaluationResult.builder()
          .evaluationRun(run)
          .testCase(testCase)
          .judgeStatus(JudgeStatus.ERROR)
          .judgeReason("Promptfoo executor did not return a result for this test case.")
          .errorMessage("Missing promptfoo result.")
          .build();
    }
    return EvaluationResult.builder()
        .evaluationRun(run)
        .testCase(testCase)
        .actualAnswer(promptfooResult.actualAnswer())
        .judgeScore(promptfooResult.judgeScore())
        .judgeStatus(promptfooResult.judgeStatus())
        .judgeReason(promptfooResult.judgeReason())
        .latencyMs(promptfooResult.latencyMs())
        .errorMessage(promptfooResult.errorMessage())
        .rawPromptfooResultJson(promptfooResult.rawPromptfooResultJson())
        .build();
  }

  private void complete(Job job, EvaluationRun run, int totalCases) {
    long passed = countResults(run, JudgeStatus.PASS);
    long failed = countResults(run, JudgeStatus.FAIL);
    long warning = countResults(run, JudgeStatus.WARNING);
    long error = countResults(run, JudgeStatus.ERROR);
    OffsetDateTime now = OffsetDateTime.now();

    run.setStatus(EvaluationRunStatus.COMPLETED);
    run.setTotalCases(totalCases);
    run.setPassedCases((int) passed);
    run.setFailedCases((int) failed);
    run.setWarningCases((int) warning);
    run.setErrorCases((int) error);
    run.setPassRate(calculatePassRate(passed, totalCases));
    run.setCompletedAt(now);

    job.setStatus(JobStatus.COMPLETED);
    job.setProgressCurrent(totalCases);
    job.setProgressTotal(totalCases);
    job.setCompletedAt(now);

    evaluationRunRepository.save(run);
    jobRepository.save(job);
    emitEvent(
        job,
        "COMPLETED",
        Map.of(
            "total", totalCases,
            "passed", passed,
            "failed", failed,
            "warning", warning,
            "error", error));
  }

  private long countResults(EvaluationRun run, JudgeStatus status) {
    return evaluationResultRepository.findByEvaluationRunId(run.getId(), org.springframework.data.domain.Pageable.unpaged())
        .stream()
        .filter(result -> result.getJudgeStatus() == status)
        .count();
  }

  private BigDecimal calculatePassRate(long passed, int totalCases) {
    if (totalCases <= 0) {
      return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }
    return BigDecimal.valueOf(passed)
        .divide(BigDecimal.valueOf(totalCases), 4, RoundingMode.HALF_UP);
  }

  private void failJob(Job job, EvaluationRun run, String message) {
    OffsetDateTime now = OffsetDateTime.now();
    String safeMessage = message == null || message.isBlank() ? "Evaluation job failed." : message;
    job.setStatus(JobStatus.FAILED);
    job.setErrorMessage(safeMessage);
    job.setCompletedAt(now);
    jobRepository.save(job);
    if (run != null) {
      run.setStatus(EvaluationRunStatus.FAILED);
      run.setCompletedAt(now);
      evaluationRunRepository.save(run);
    }
    emitEvent(job, "FAILED", Map.of("errorMessage", safeMessage));
  }

  private void emitEvent(Job job, String eventType, Map<String, ?> payload) {
    jobEventRepository.save(
        JobEvent.builder()
            .job(job)
            .eventType(eventType)
            .payloadJson(toJson(payload))
            .build());
  }

  private String toJson(Map<String, ?> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      return "{\"error\":\"Failed to serialize event payload\"}";
    }
  }
}
