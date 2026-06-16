package me.nghlong3004.vqc.api.redteam.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.config.PromptfooProperties;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.job.entity.JobEvent;
import me.nghlong3004.vqc.api.job.enums.JobStatus;
import me.nghlong3004.vqc.api.job.enums.JobType;
import me.nghlong3004.vqc.api.job.repository.JobEventRepository;
import me.nghlong3004.vqc.api.job.repository.JobRepository;
import me.nghlong3004.vqc.api.redteam.entity.RedTeamRun;
import me.nghlong3004.vqc.api.redteam.enums.RedTeamRunStatus;
import me.nghlong3004.vqc.api.redteam.promptfoo.RedTeamPromptfooExecutor;
import me.nghlong3004.vqc.api.redteam.repository.RedTeamRunRepository;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedTeamJobHandler {

  private final JobRepository jobRepository;
  private final JobEventRepository jobEventRepository;
  private final RedTeamRunRepository redTeamRunRepository;
  private final RedTeamPromptfooExecutor promptfooExecutor;
  private final PromptfooProperties promptfooProperties;
  private final ObjectMapper objectMapper;

  public void handle(UUID jobPublicId) {
    Job job = jobRepository.findByPublicId(jobPublicId).orElse(null);
    if (job == null) {
      log.warn("Red-team job {} was queued but no longer exists", jobPublicId);
      return;
    }
    if (job.getJobType() != JobType.RED_TEAM_RUN) {
      fail(job, null, "Unsupported job type: " + job.getJobType());
      return;
    }
    RedTeamRun run = redTeamRunRepository.findWithWorkerContextById(job.getResourceId()).orElse(null);
    if (run == null) {
      fail(job, null, "Red-team run resource is missing.");
      return;
    }
    try {
      start(job, run);
      Path runDir =
          Path.of(promptfooProperties.getWorkDir()).resolve("red-team").resolve(run.getPublicId().toString());
      run.setArtifactDir(runDir.toString());
      redTeamRunRepository.save(run);
      emit(job, "GENERATING_RED_TEAM_TESTS", Map.of("runPublicId", run.getPublicId()));
      var result = promptfooExecutor.execute(run, runDir);
      complete(job, run, result.total(), result.passed(), result.failed(), result.errors());
      log.info("Completed red-team job {} for run {}", job.getPublicId(), run.getPublicId());
    } catch (Exception ex) {
      fail(job, run, ex.getMessage());
      log.error("Failed red-team job {} for run {}", job.getPublicId(), run.getPublicId(), ex);
    }
  }

  private void start(Job job, RedTeamRun run) {
    OffsetDateTime now = OffsetDateTime.now();
    job.setStatus(JobStatus.RUNNING);
    job.setStartedAt(now);
    job.setErrorMessage(null);
    run.setStatus(RedTeamRunStatus.RUNNING);
    run.setStartedAt(now);
    jobRepository.save(job);
    redTeamRunRepository.save(run);
    emit(job, "RUNNING", Map.of("runPublicId", run.getPublicId()));
  }

  private void complete(Job job, RedTeamRun run, int total, int passed, int failed, int errors) {
    OffsetDateTime now = OffsetDateTime.now();
    run.setStatus(RedTeamRunStatus.COMPLETED);
    run.setTotalCases(total);
    run.setPassedCases(passed);
    run.setFailedCases(failed);
    run.setErrorCases(errors);
    run.setCompletedAt(now);
    job.setStatus(JobStatus.COMPLETED);
    job.setProgressCurrent(1);
    job.setProgressTotal(1);
    job.setCompletedAt(now);
    redTeamRunRepository.save(run);
    jobRepository.save(job);
    emit(
        job,
        "COMPLETED",
        Map.of("total", total, "passed", passed, "failed", failed, "errors", errors));
  }

  private void fail(Job job, RedTeamRun run, String message) {
    OffsetDateTime now = OffsetDateTime.now();
    String safeMessage = message == null || message.isBlank() ? "Red-team job failed." : message;
    job.setStatus(JobStatus.FAILED);
    job.setErrorMessage(safeMessage);
    job.setCompletedAt(now);
    jobRepository.save(job);
    if (run != null) {
      run.setStatus(RedTeamRunStatus.FAILED);
      run.setErrorMessage(safeMessage);
      run.setCompletedAt(now);
      redTeamRunRepository.save(run);
    }
    emit(job, "FAILED", Map.of("errorMessage", safeMessage));
  }

  private void emit(Job job, String eventType, Map<String, ?> payload) {
    jobEventRepository.save(
        JobEvent.builder().job(job).eventType(eventType).payloadJson(toJson(payload)).build());
  }

  private String toJson(Map<String, ?> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      return "{\"error\":\"Failed to serialize event payload\"}";
    }
  }
}
