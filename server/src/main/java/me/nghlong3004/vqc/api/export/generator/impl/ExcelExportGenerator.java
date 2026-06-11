package me.nghlong3004.vqc.api.export.generator.impl;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.config.ExportProperties;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationResult;
import me.nghlong3004.vqc.api.export.entity.ExportFile;
import me.nghlong3004.vqc.api.export.enums.ExportFileType;
import me.nghlong3004.vqc.api.export.generator.ExportGenerator;
import me.nghlong3004.vqc.api.export.generator.GeneratedExportFile;
import me.nghlong3004.vqc.api.review.entity.ReviewDecision;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Component
@RequiredArgsConstructor
public class ExcelExportGenerator implements ExportGenerator {

  private static final List<String> HEADERS =
      List.of(
          "externalId",
          "question",
          "groundTruth",
          "actualAnswer",
          "judgeStatus",
          "judgeScore",
          "judgeReason",
          "qcStatus",
          "qcNote",
          "picBug");

  private final ExportProperties exportProperties;

  @Override
  public boolean supports(ExportFileType fileType) {
    return fileType == ExportFileType.EXCEL;
  }

  @Override
  public GeneratedExportFile generate(ExportFile exportFile, List<EvaluationResult> results) {
    try {
      Path dir = Files.createDirectories(Path.of(exportProperties.getDir()));
      String fileName = "evaluation-run-" + exportFile.getEvaluationRun().getPublicId() + ".xlsx";
      Path path = dir.resolve(fileName);
      try (Workbook workbook = new XSSFWorkbook();
          OutputStream outputStream = Files.newOutputStream(path)) {
        Sheet sheet = workbook.createSheet("Evaluation Results");
        writeRow(sheet.createRow(0), HEADERS);
        for (int i = 0; i < results.size(); i++) {
          writeRow(sheet.createRow(i + 1), values(results.get(i)));
        }
        for (int i = 0; i < HEADERS.size(); i++) {
          sheet.autoSizeColumn(i);
        }
        workbook.write(outputStream);
      }
      return new GeneratedExportFile(fileName, path.toString());
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to generate Excel export.", ex);
    }
  }

  private void writeRow(Row row, List<String> values) {
    for (int i = 0; i < values.size(); i++) {
      row.createCell(i).setCellValue(values.get(i));
    }
  }

  private List<String> values(EvaluationResult result) {
    ReviewDecision review = result.getReviewDecision();
    return List.of(
        value(result.getTestCase().getExternalId()),
        value(result.getTestCase().getQuestion()),
        value(result.getTestCase().getGroundTruth()),
        value(result.getActualAnswer()),
        result.getJudgeStatus().name(),
        value(result.getJudgeScore()),
        value(result.getJudgeReason()),
        review == null ? "NOT_REVIEWED" : review.getQcStatus().name(),
        value(review == null ? null : review.getQcNote()),
        value(review == null || review.getPicBugUser() == null ? null : review.getPicBugUser().getDisplayName()));
  }

  private String value(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

}
