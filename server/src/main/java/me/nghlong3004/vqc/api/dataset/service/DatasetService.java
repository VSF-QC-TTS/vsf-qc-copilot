package me.nghlong3004.vqc.api.dataset.service;

import java.util.UUID;
import me.nghlong3004.vqc.api.dataset.request.CreateDatasetRequest;
import me.nghlong3004.vqc.api.dataset.response.DatasetResponse;

/**
 * Coordinates dataset use cases.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface DatasetService {

  /**
   * Creates a dataset under a project owned by the authenticated user.
   *
   * @param projectPublicId public project identifier
   * @param request create dataset payload
   * @param username authenticated principal username/email
   * @return created dataset response
   */
  DatasetResponse createDataset(
      UUID projectPublicId, CreateDatasetRequest request, String username);
}
