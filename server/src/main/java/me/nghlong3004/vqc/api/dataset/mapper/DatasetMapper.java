package me.nghlong3004.vqc.api.dataset.mapper;


import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.dataset.response.DatasetListItemResponse;
import me.nghlong3004.vqc.api.dataset.response.DatasetResponse;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Component
public class DatasetMapper {

  /**
   * Maps an internal {@link Dataset} entity to a public API response.
   *
   * @param dataset internal {@link Dataset} entity
   * @param testCaseCount total number of test cases in the dataset
   * @param activeTestCaseCount number of active test cases in the dataset
   * @return public {@link DatasetResponse}
   */
  public DatasetResponse toResponse(Dataset dataset, long testCaseCount, long activeTestCaseCount) {
    return new DatasetResponse(
        dataset.getPublicId(),
        dataset.getProject().getPublicId(),
        dataset.getName(),
        dataset.getDescription(),
        dataset.getVersion(),
        dataset.getSourceType(),
        dataset.getStatus(),
        testCaseCount,
        activeTestCaseCount,
        dataset.getCreatedAt(),
        dataset.getUpdatedAt());
  }

  /**
   * Maps an internal {@link Dataset} entity to a public list item response.
   *
   * @param dataset internal {@link Dataset} entity
   * @param testCaseCount number of test cases in the dataset
   * @return public {@link DatasetListItemResponse}
   */
  public DatasetListItemResponse toListItemResponse(Dataset dataset, long testCaseCount) {
    return new DatasetListItemResponse(
        dataset.getPublicId(),
        dataset.getProject().getPublicId(),
        dataset.getName(),
        dataset.getSourceType(),
        dataset.getStatus(),
        testCaseCount,
        dataset.getCreatedAt());
  }
}
