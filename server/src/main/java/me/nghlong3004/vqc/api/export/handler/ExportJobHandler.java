package me.nghlong3004.vqc.api.export.handler;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationResult;
import me.nghlong3004.vqc.api.evaluation.repository.EvaluationResultRepository;
import me.nghlong3004.vqc.api.export.entity.ExportFile;
import me.nghlong3004.vqc.api.export.enums.ExportFileStatus;
import me.nghlong3004.vqc.api.export.generator.ExportGenerator;
import me.nghlong3004.vqc.api.export.generator.GeneratedExportFile;
import me.nghlong3004.vqc.api.export.repository.ExportFileRepository;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.job.enums.JobStatus;
import me.nghlong3004.vqc.api.job.repository.JobRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExportJobHandler {

  private final JobRepository jobRepository;
  private final ExportFileRepository exportFileRepository;
  private final EvaluationResultRepository evaluationResultRepository;
  private final List<ExportGenerator> exportGenerators;

  @Transactional
  public void handle(UUID jobPublicId) {
    Job job = jobRepository.findByPublicId(jobPublicId).orElse(null);
    if (job == null) {
      log.warn("Export job {} was queued but no longer exists", jobPublicId);
      return;
    }
    ExportFile exportFile = exportFileRepository.findById(job.getResourceId()).orElse(null);
    if (exportFile == null) {
      failJob(job, null, "Export file resource is missing.");
      return;
    }

    try {
      job.setStatus(JobStatus.RUNNING);
      job.setStartedAt(OffsetDateTime.now());
      jobRepository.save(job);

      List<EvaluationResult> results =
          evaluationResultRepository
              .findByEvaluationRunId(exportFile.getEvaluationRun().getId(), Pageable.unpaged())
              .getContent();
      ExportGenerator generator =
          exportGenerators.stream()
              .filter(candidate -> candidate.supports(exportFile.getFileType()))
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("No export generator for " + exportFile.getFileType()));
      GeneratedExportFile generated = generator.generate(exportFile, results);

      exportFile.setStatus(ExportFileStatus.READY);
      exportFile.setFileName(generated.fileName());
      exportFile.setFilePath(generated.filePath());
      exportFile.setReadyAt(OffsetDateTime.now());
      exportFile.setErrorMessage(null);
      exportFileRepository.save(exportFile);

      job.setStatus(JobStatus.COMPLETED);
      job.setProgressCurrent(1);
      job.setProgressTotal(1);
      job.setCompletedAt(OffsetDateTime.now());
      jobRepository.save(job);
      log.info("Generated export {} for job {}", exportFile.getPublicId(), job.getPublicId());
    } catch (Exception ex) {
      failJob(job, exportFile, ex.getMessage());
      log.error("Failed export job {}", job.getPublicId(), ex);
    }
  }

  private void failJob(Job job, ExportFile exportFile, String message) {
    String safeMessage = message == null || message.isBlank() ? "Export job failed." : message;
    job.setStatus(JobStatus.FAILED);
    job.setErrorMessage(safeMessage);
    job.setCompletedAt(OffsetDateTime.now());
    jobRepository.save(job);
    if (exportFile != null) {
      exportFile.setStatus(ExportFileStatus.FAILED);
      exportFile.setErrorMessage(safeMessage);
      exportFileRepository.save(exportFile);
    }
  }
}
