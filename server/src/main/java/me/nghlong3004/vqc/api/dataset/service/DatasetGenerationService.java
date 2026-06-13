package me.nghlong3004.vqc.api.dataset.service;

import java.util.UUID;
import me.nghlong3004.vqc.api.dataset.request.GenerateDatasetRequest;
import me.nghlong3004.vqc.api.dataset.response.GenerateDatasetResponse;

/**
 * Handles AI-powered test case generation for datasets.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/13/2026
 */
public interface DatasetGenerationService {

  /**
   * Creates a generation job for the given dataset and pushes it to the Redis queue.
   *
   * <p>The job handler ({@code DatasetGenerationJobHandler}) will later pick this up, call Gemini
   * to generate test cases, and bulk-insert them into the dataset.
   *
   * @param datasetPublicId public identifier of the target {@link
   *     me.nghlong3004.vqc.api.dataset.entity.Dataset}
   * @param request generation parameters (requirement, count, additional prompt)
   * @param username authenticated principal username/email
   * @return async job tracking response (202-style)
   */
  GenerateDatasetResponse generateTestCases(
      UUID datasetPublicId, GenerateDatasetRequest request, String username);
}
