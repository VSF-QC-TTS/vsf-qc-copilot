package me.nghlong3004.vqc.api.dataset.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.dataset.repository.DatasetRepository;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.job.entity.JobEvent;
import me.nghlong3004.vqc.api.job.enums.JobStatus;
import me.nghlong3004.vqc.api.job.enums.JobType;
import me.nghlong3004.vqc.api.job.repository.JobEventRepository;
import me.nghlong3004.vqc.api.job.repository.JobRepository;

import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;
import me.nghlong3004.vqc.api.testcase.repository.TestCaseRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processes DATASET_GENERATION jobs by calling Gemini AI to generate test cases.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/13/2026
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatasetGenerationJobHandler {

  private static final TypeReference<List<Map<String, String>>> LIST_MAP_TYPE =
      new TypeReference<>() {};

  private final JobRepository jobRepository;
  private final JobEventRepository jobEventRepository;
  private final DatasetRepository datasetRepository;
  private final TestCaseRepository testCaseRepository;
  private final ChatClient.Builder chatClientBuilder;
  private final ObjectMapper objectMapper;

  /**
   * Handles one queued dataset generation job.
   *
   * @param jobPublicId public job identifier
   */
  @Async
  public void handle(UUID jobPublicId) {
    Job job = jobRepository.findByPublicId(jobPublicId).orElse(null);
    if (job == null) {
      log.warn("Dataset generation job {} was queued but no longer exists", jobPublicId);
      return;
    }
    if (job.getJobType() != JobType.DATASET_GENERATION) {
      failJob(job, "Unsupported job type: " + job.getJobType());
      return;
    }

    Dataset dataset = datasetRepository.findById(job.getResourceId()).orElse(null);
    if (dataset == null) {
      failJob(job, "Dataset resource is missing.");
      return;
    }

    try {
      start(job);
      String aiResponse = callGemini(dataset, job.getProgressTotal());
      List<Map<String, String>> generated = parseResponse(aiResponse);

      int maxSortOrder =
          testCaseRepository.findMaxSortOrderByDatasetId(dataset.getId()).orElse(0);
      int completed = 0;
      for (Map<String, String> entry : generated) {
        String question = entry.get("question");
        if (question == null || question.isBlank()) {
          continue;
        }
        TestCase testCase =
            TestCase.builder()
                .dataset(dataset)
                .question(question.trim())
                .groundTruth(
                    entry.get("ground_truth") != null
                        ? entry.get("ground_truth").trim()
                        : null)
                .status(TestCaseStatus.ACTIVE)
                .sortOrder(++maxSortOrder)
                .build();
        testCaseRepository.save(testCase);
        completed++;
        job.setProgressCurrent(completed);
        jobRepository.save(job);
        emitEvent(
            job,
            "CASE_COMPLETED",
            Map.of("completed", completed, "total", generated.size()));
      }

      complete(job, completed);
      log.info(
          "Completed dataset generation job {} for dataset {}, generated {} test cases",
          job.getPublicId(),
          dataset.getPublicId(),
          completed);
    } catch (Exception ex) {
      failJob(job, ex.getMessage());
      log.error(
          "Failed dataset generation job {} for dataset {}",
          job.getPublicId(),
          dataset.getPublicId(),
          ex);
    }
  }

  String callGemini(Dataset dataset, int count) {
    String generationContext =
        dataset.getGenerationPrompt() != null ? dataset.getGenerationPrompt() : "No context provided.";

    List<TestCase> existing =
        testCaseRepository.findByDatasetAndStatusOrderBySortOrderAscIdAsc(
            dataset, TestCaseStatus.ACTIVE);
    StringBuilder existingQuestions = new StringBuilder();
    for (TestCase tc : existing) {
      existingQuestions.append("- ").append(tc.getQuestion()).append("\n");
    }

    String additionalPrompt =
        dataset.getGenerationPrompt() != null ? dataset.getGenerationPrompt() : "";

    String systemPrompt =
        """
        You are a QA test case generator for chatbot evaluation.
        Generate exactly %d test cases as a JSON array.
        Each item must have exactly two fields: "question" and "ground_truth".
        Return ONLY the JSON array, no markdown, no explanation.
        """
            .formatted(count);

    String userPrompt =
        """
        Generation context:
        %s

        %s

        Existing test case questions to avoid duplicates:
        %s

        Generate %d unique test cases. Return a JSON array:
        [{"question": "...", "ground_truth": "..."}, ...]
        """
            .formatted(
                generationContext,
                additionalPrompt.isBlank() ? "" : "Additional instructions:\n" + additionalPrompt,
                existingQuestions.isEmpty() ? "(none)" : existingQuestions.toString(),
                count);

    ChatClient chatClient = chatClientBuilder.build();
    return chatClient.prompt().system(systemPrompt).user(userPrompt).call().content();
  }

  List<Map<String, String>> parseResponse(String aiResponse) {
    if (aiResponse == null || aiResponse.isBlank()) {
      return List.of();
    }
    String cleaned = aiResponse.trim();
    if (cleaned.startsWith("```json")) {
      cleaned = cleaned.substring(7);
    }
    if (cleaned.startsWith("```")) {
      cleaned = cleaned.substring(3);
    }
    if (cleaned.endsWith("```")) {
      cleaned = cleaned.substring(0, cleaned.length() - 3);
    }
    cleaned = cleaned.trim();
    try {
      return objectMapper.readValue(cleaned, LIST_MAP_TYPE);
    } catch (JsonProcessingException ex) {
      log.error("Failed to parse AI response as JSON array: {}", cleaned, ex);
      return List.of();
    }
  }

  private void start(Job job) {
    OffsetDateTime now = OffsetDateTime.now();
    job.setStatus(JobStatus.RUNNING);
    job.setStartedAt(now);
    job.setErrorMessage(null);
    jobRepository.save(job);
    emitEvent(job, "RUNNING", Map.of("datasetId", job.getResourceId()));
  }

  private void complete(Job job, int generatedCount) {
    OffsetDateTime now = OffsetDateTime.now();
    job.setStatus(JobStatus.COMPLETED);
    job.setProgressCurrent(generatedCount);
    job.setCompletedAt(now);
    jobRepository.save(job);
    emitEvent(job, "COMPLETED", Map.of("generated", generatedCount));
  }

  private void failJob(Job job, String message) {
    OffsetDateTime now = OffsetDateTime.now();
    String safeMessage =
        message == null || message.isBlank() ? "Dataset generation job failed." : message;
    job.setStatus(JobStatus.FAILED);
    job.setErrorMessage(safeMessage);
    job.setCompletedAt(now);
    jobRepository.save(job);
    emitEvent(job, "FAILED", Map.of("errorMessage", safeMessage));
  }

  private void emitEvent(Job job, String eventType, Map<String, ?> payload) {
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
