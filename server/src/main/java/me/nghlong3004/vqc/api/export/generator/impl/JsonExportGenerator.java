package me.nghlong3004.vqc.api.export.generator.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.config.ExportProperties;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationResult;
import me.nghlong3004.vqc.api.export.entity.ExportFile;
import me.nghlong3004.vqc.api.export.enums.ExportFileType;
import me.nghlong3004.vqc.api.export.generator.ExportGenerator;
import me.nghlong3004.vqc.api.export.generator.GeneratedExportFile;
import me.nghlong3004.vqc.api.review.entity.ReviewDecision;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Component
@RequiredArgsConstructor
public class JsonExportGenerator implements ExportGenerator {

  private final ExportProperties exportProperties;
  private final ObjectMapper objectMapper;

  @Override
  public boolean supports(ExportFileType fileType) {
    return fileType == ExportFileType.JSON;
  }

  @Override
  public GeneratedExportFile generate(ExportFile exportFile, List<EvaluationResult> results) {
    try {
      Path dir = Files.createDirectories(Path.of(exportProperties.getDir()));
      String fileName = "evaluation-run-" + exportFile.getEvaluationRun().getPublicId() + ".json";
      Path path = dir.resolve(fileName);
      List<Map<String, Object>> items = results.stream().map(this::toRow).toList();
      Files.writeString(path, objectMapper.writeValueAsString(items), StandardCharsets.UTF_8);
      return new GeneratedExportFile(fileName, path.toString());
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to generate JSON export.", ex);
    }
  }

  private Map<String, Object> toRow(EvaluationResult result) {
    ReviewDecision review = result.getReviewDecision();
    return Map.ofEntries(
        Map.entry("resultPublicId", result.getPublicId()),
        Map.entry("testCasePublicId", result.getTestCase().getPublicId()),
        Map.entry("externalId", nullable(result.getTestCase().getExternalId())),
        Map.entry("question", nullable(result.getTestCase().getQuestion())),
        Map.entry("groundTruth", nullable(result.getTestCase().getGroundTruth())),
        Map.entry("actualAnswer", nullable(result.getActualAnswer())),
        Map.entry("judgeScore", nullable(result.getJudgeScore())),
        Map.entry("judgeStatus", result.getJudgeStatus()),
        Map.entry("judgeReason", nullable(result.getJudgeReason())),
        Map.entry("qcStatus", review == null ? "NOT_REVIEWED" : review.getQcStatus().name()),
        Map.entry("qcNote", nullable(review == null ? null : review.getQcNote())),
        Map.entry("picBug", nullable(review == null || review.getPicBugUser() == null ? null : review.getPicBugUser().getDisplayName())));
  }

  private Object nullable(Object value) {
    return value == null ? "" : value;
  }
}
