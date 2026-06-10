package me.nghlong3004.vqc.api.dataset.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.dataset.enums.DatasetSourceType;
import me.nghlong3004.vqc.api.dataset.enums.DatasetStatus;
import me.nghlong3004.vqc.api.dataset.response.DatasetListItemResponse;
import me.nghlong3004.vqc.api.dataset.response.DatasetResponse;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.requirement.entity.BusinessRequirement;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class DatasetMapperTest {

  private final DatasetMapper datasetMapper = new DatasetMapper();

  @Test
  void toResponseMapsPublicDatasetFields() {
    Project project = new Project();
    project.setPublicId(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"));
    BusinessRequirement requirement = new BusinessRequirement();
    requirement.setPublicId(UUID.fromString("ebd7f0f0-4924-4e81-9795-d1f060bec2f2"));
    Dataset dataset = new Dataset();
    dataset.setPublicId(UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"));
    dataset.setProject(project);
    dataset.setRequirement(requirement);
    dataset.setName("Health Demo Dataset");
    dataset.setDescription("Sample dataset for Week 4 demo.");
    dataset.setVersion(1);
    dataset.setSourceType(DatasetSourceType.SAMPLE_DEMO);
    dataset.setStatus(DatasetStatus.DRAFT);
    dataset.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"));
    dataset.setUpdatedAt(OffsetDateTime.parse("2026-06-08T10:35:00Z"));

    DatasetResponse response = datasetMapper.toResponse(dataset, 12);

    assertThat(response.publicId()).isEqualTo(dataset.getPublicId());
    assertThat(response.projectPublicId()).isEqualTo(project.getPublicId());
    assertThat(response.requirementPublicId()).isEqualTo(requirement.getPublicId());
    assertThat(response.name()).isEqualTo("Health Demo Dataset");
    assertThat(response.description()).isEqualTo("Sample dataset for Week 4 demo.");
    assertThat(response.version()).isEqualTo(1);
    assertThat(response.sourceType()).isEqualTo(DatasetSourceType.SAMPLE_DEMO);
    assertThat(response.status()).isEqualTo(DatasetStatus.DRAFT);
    assertThat(response.totalCases()).isEqualTo(12);
    assertThat(response.createdAt()).isEqualTo(OffsetDateTime.parse("2026-06-08T10:30:00Z"));
    assertThat(response.updatedAt()).isEqualTo(OffsetDateTime.parse("2026-06-08T10:35:00Z"));
  }

  @Test
  void toResponseAllowsMissingRequirement() {
    Project project = new Project();
    project.setPublicId(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"));
    Dataset dataset = new Dataset();
    dataset.setPublicId(UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"));
    dataset.setProject(project);
    dataset.setName("Manual Dataset");
    dataset.setVersion(1);
    dataset.setSourceType(DatasetSourceType.MANUAL);
    dataset.setStatus(DatasetStatus.DRAFT);
    dataset.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"));
    dataset.setUpdatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"));

    DatasetResponse response = datasetMapper.toResponse(dataset, 0);

    assertThat(response.requirementPublicId()).isNull();
    assertThat(response.totalCases()).isZero();
  }

  @Test
  void toListItemResponseMapsPublicDatasetSummaryFields() {
    Project project = new Project();
    project.setPublicId(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"));
    Dataset dataset = new Dataset();
    dataset.setPublicId(UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"));
    dataset.setProject(project);
    dataset.setName("Health Demo Dataset");
    dataset.setSourceType(DatasetSourceType.SAMPLE_DEMO);
    dataset.setStatus(DatasetStatus.DRAFT);
    dataset.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"));

    DatasetListItemResponse response = datasetMapper.toListItemResponse(dataset, 3);

    assertThat(response.publicId()).isEqualTo(dataset.getPublicId());
    assertThat(response.projectPublicId()).isEqualTo(project.getPublicId());
    assertThat(response.name()).isEqualTo("Health Demo Dataset");
    assertThat(response.sourceType()).isEqualTo(DatasetSourceType.SAMPLE_DEMO);
    assertThat(response.status()).isEqualTo(DatasetStatus.DRAFT);
    assertThat(response.totalCases()).isEqualTo(3);
    assertThat(response.createdAt()).isEqualTo(OffsetDateTime.parse("2026-06-08T10:30:00Z"));
  }
}
