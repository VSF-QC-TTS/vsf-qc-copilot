package me.nghlong3004.vqc.api.export.generator;

import java.util.List;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationResult;
import me.nghlong3004.vqc.api.export.entity.ExportFile;
import me.nghlong3004.vqc.api.export.enums.ExportFileType;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public interface ExportGenerator {

  boolean supports(ExportFileType fileType);

  GeneratedExportFile generate(ExportFile exportFile, List<EvaluationResult> results);
}
