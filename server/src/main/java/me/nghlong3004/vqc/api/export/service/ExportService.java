package me.nghlong3004.vqc.api.export.service;

import java.util.UUID;
import me.nghlong3004.vqc.api.export.request.CreateExportRequest;
import me.nghlong3004.vqc.api.export.response.CreateExportResponse;
import me.nghlong3004.vqc.api.export.response.ExportFileResponse;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public interface ExportService {

  CreateExportResponse createExport(UUID runPublicId, CreateExportRequest request, String username);

  ExportFileResponse getExport(UUID exportPublicId, String username);
}
