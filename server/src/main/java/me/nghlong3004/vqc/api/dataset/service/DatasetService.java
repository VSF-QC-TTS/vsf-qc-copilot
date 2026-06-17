package me.nghlong3004.vqc.api.dataset.service;

import java.util.UUID;
import me.nghlong3004.vqc.api.dataset.enums.DatasetStatus;
import me.nghlong3004.vqc.api.dataset.request.CreateDatasetRequest;
import me.nghlong3004.vqc.api.dataset.request.UpdateDatasetRequest;
import me.nghlong3004.vqc.api.dataset.response.DatasetPageResponse;
import me.nghlong3004.vqc.api.dataset.response.DatasetResponse;
import org.springframework.data.domain.Pageable;

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

  /**
   * Lists datasets under a project owned by the authenticated user.
   *
   * @param projectPublicId public project identifier
   * @param status optional dataset status filter
   * @param pageable page and sort request
   * @param username authenticated principal username/email
   * @return page of datasets
   */
  DatasetPageResponse listDatasets(
      UUID projectPublicId, DatasetStatus status, Pageable pageable, String username);

  /**
   * Gets a dataset owned by the authenticated user.
   *
   * @param datasetPublicId public dataset identifier
   * @param username authenticated principal username/email
   * @return dataset detail response
   */
  DatasetResponse getDataset(UUID datasetPublicId, String username);

  /**
   * Updates a dataset owned by the authenticated user.
   *
   * @param datasetPublicId public dataset identifier
   * @param request update dataset payload
   * @param username authenticated principal username/email
   * @return updated dataset response
   */
  DatasetResponse updateDataset(
      UUID datasetPublicId, UpdateDatasetRequest request, String username);

  /**
   * Deletes a dataset owned by the authenticated user.
   *
   * @param datasetPublicId public dataset identifier
   * @param username authenticated principal username/email
   */
  void deleteDataset(UUID datasetPublicId, String username);
}
