package me.nghlong3004.vqc.api.export.service.impl;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.evaluation.repository.EvaluationRunRepository;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.export.entity.ExportFile;
import me.nghlong3004.vqc.api.export.enums.ExportFileStatus;
import me.nghlong3004.vqc.api.export.enums.ExportFileType;
import me.nghlong3004.vqc.api.export.mapper.ExportFileMapper;
import me.nghlong3004.vqc.api.export.repository.ExportFileRepository;
import me.nghlong3004.vqc.api.export.request.CreateExportRequest;
import me.nghlong3004.vqc.api.export.response.CreateExportResponse;
import me.nghlong3004.vqc.api.export.response.ExportDownloadResponse;
import me.nghlong3004.vqc.api.export.response.ExportFileResponse;
import me.nghlong3004.vqc.api.export.service.ExportService;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.job.enums.JobStatus;
import me.nghlong3004.vqc.api.job.enums.JobType;
import me.nghlong3004.vqc.api.job.enums.ResourceType;
import me.nghlong3004.vqc.api.job.repository.JobRepository;
import me.nghlong3004.vqc.api.job.service.JobQueuePublisher;
import me.nghlong3004.vqc.api.storage.model.StorageResource;
import me.nghlong3004.vqc.api.storage.service.ObjectStorageService;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportServiceImpl implements ExportService {

  private final ExportFileRepository exportFileRepository;
  private final EvaluationRunRepository evaluationRunRepository;
  private final JobRepository jobRepository;
  private final UserRepository userRepository;
  private final JobQueuePublisher jobQueuePublisher;
  private final ExportFileMapper exportFileMapper;
  private final ObjectStorageService objectStorageService;

  @Override
  @Transactional
  public CreateExportResponse createExport(
      UUID runPublicId, CreateExportRequest request, String username) {
    User creator =
        userRepository
            .findByUsername(username.trim().toLowerCase())
            .orElseThrow(() -> new ResourceException(ErrorCode.USER_NOT_FOUND));
    EvaluationRun run =
        evaluationRunRepository
            .findByPublicIdAndCreatedBy(runPublicId, creator)
            .orElseThrow(() -> new ResourceException(ErrorCode.EVALUATION_RUN_NOT_FOUND));

    ExportFile exportFile =
        exportFileRepository.save(
            ExportFile.builder()
                .project(run.getProject())
                .evaluationRun(run)
                .fileType(request.fileType())
                .status(ExportFileStatus.PENDING)
                .createdBy(creator)
                .build());
    Job job =
        jobRepository.save(
            Job.builder()
                .jobType(toJobType(request.fileType()))
                .status(JobStatus.PENDING)
                .resourceType(ResourceType.EXPORT_FILE)
                .resourceId(exportFile.getId())
                .project(run.getProject())
                .createdBy(creator)
                .progressTotal(1)
                .build());
    exportFile.setJob(job);
    ExportFile savedExport = exportFileRepository.save(exportFile);
    jobQueuePublisher.publish(job.getPublicId().toString());

    log.info(
        "Created export {} with job {} for run {} by user {}",
        savedExport.getPublicId(),
        job.getPublicId(),
        run.getPublicId(),
        creator.getPublicId());
    return new CreateExportResponse(
        savedExport.getPublicId(), job.getPublicId(), savedExport.getStatus(), "Export job accepted.");
  }

  @Override
  @Transactional(readOnly = true)
  public ExportFileResponse getExport(UUID exportPublicId, String username) {
    User creator =
        userRepository
            .findByUsername(username.trim().toLowerCase())
            .orElseThrow(() -> new ResourceException(ErrorCode.USER_NOT_FOUND));
    ExportFile exportFile =
        exportFileRepository
            .findByPublicIdAndCreatedBy(exportPublicId, creator)
            .orElseThrow(() -> new ResourceException(ErrorCode.EXPORT_FILE_NOT_FOUND));
    return exportFileMapper.toResponse(exportFile);
  }

  @Override
  @Transactional(readOnly = true)
  public ExportDownloadResponse downloadExport(UUID exportPublicId, String username) {
    User creator =
        userRepository
            .findByUsername(username.trim().toLowerCase())
            .orElseThrow(() -> new ResourceException(ErrorCode.USER_NOT_FOUND));
    ExportFile exportFile =
        exportFileRepository
            .findByPublicIdAndCreatedBy(exportPublicId, creator)
            .orElseThrow(() -> new ResourceException(ErrorCode.EXPORT_FILE_NOT_FOUND));
    if (exportFile.getStatus() != ExportFileStatus.READY) {
      throw new ResourceException(ErrorCode.EXPORT_FILE_NOT_READY);
    }
    if (exportFile.getStorageKey() == null || exportFile.getStorageKey().isBlank()) {
      throw new ResourceException(ErrorCode.EXPORT_FILE_NOT_FOUND);
    }
    StorageResource resource;
    try {
      resource = objectStorageService.load(exportFile.getStorageKey());
    } catch (RuntimeException ex) {
      throw new ResourceException(ErrorCode.EXPORT_FILE_NOT_FOUND);
    }
    return new ExportDownloadResponse(
        exportFile.getFileName(),
        mediaType(exportFile),
        resource.resource());
  }

  private MediaType mediaType(ExportFile exportFile) {
    if (exportFile.getContentType() != null && !exportFile.getContentType().isBlank()) {
      return MediaType.parseMediaType(exportFile.getContentType());
    }
    return exportFile.getFileType() == ExportFileType.EXCEL
        ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        : MediaType.APPLICATION_JSON;
  }

  private JobType toJobType(ExportFileType fileType) {
    return fileType == ExportFileType.EXCEL ? JobType.EXPORT_EXCEL : JobType.EXPORT_JSON;
  }
}
