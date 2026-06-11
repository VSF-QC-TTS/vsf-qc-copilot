package me.nghlong3004.vqc.api.export.mapper;

import java.util.UUID;
import me.nghlong3004.vqc.api.export.entity.ExportFile;
import me.nghlong3004.vqc.api.export.enums.ExportFileStatus;
import me.nghlong3004.vqc.api.export.response.ExportFileResponse;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Component
public class ExportFileMapper {

  public ExportFileResponse toResponse(ExportFile exportFile) {
    UUID jobPublicId = exportFile.getJob() == null ? null : exportFile.getJob().getPublicId();
    String downloadUrl =
        exportFile.getStatus() == ExportFileStatus.READY
            ? "/api/v1/exports/" + exportFile.getPublicId() + "/file"
            : null;
    return new ExportFileResponse(
        exportFile.getPublicId(),
        exportFile.getProject().getPublicId(),
        exportFile.getEvaluationRun().getPublicId(),
        jobPublicId,
        exportFile.getFileType(),
        exportFile.getStatus(),
        exportFile.getFileName(),
        downloadUrl,
        exportFile.getErrorMessage(),
        exportFile.getCreatedAt(),
        exportFile.getReadyAt());
  }
}
