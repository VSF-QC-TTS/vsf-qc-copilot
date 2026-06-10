package me.nghlong3004.vqc.api.dataset.mapper;

import java.util.UUID;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
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
   * @param totalCases number of test cases in the dataset
   * @return public {@link DatasetResponse}
   */
  public DatasetResponse toResponse(Dataset dataset, long totalCases) {
    UUID requirementPublicId =
        dataset.getRequirement() == null ? null : dataset.getRequirement().getPublicId();
    return new DatasetResponse(
        dataset.getPublicId(),
        dataset.getProject().getPublicId(),
        requirementPublicId,
        dataset.getName(),
        dataset.getDescription(),
        dataset.getVersion(),
        dataset.getSourceType(),
        dataset.getStatus(),
        totalCases,
        dataset.getCreatedAt(),
        dataset.getUpdatedAt());
  }
}
