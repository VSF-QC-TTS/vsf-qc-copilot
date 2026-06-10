package me.nghlong3004.vqc.api.rubric.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.rubric.entity.Rubric;
import me.nghlong3004.vqc.api.rubric.entity.RubricCriterion;
import me.nghlong3004.vqc.api.rubric.entity.RubricVersion;
import me.nghlong3004.vqc.api.rubric.enums.RubricStatus;
import me.nghlong3004.vqc.api.rubric.enums.RubricVersionStatus;
import me.nghlong3004.vqc.api.rubric.response.RubricCriterionResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricListItemResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricVersionListItemResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricVersionResponse;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class RubricMapperTest {

  private final RubricMapper rubricMapper = new RubricMapper();

  @Test
  void toResponseMapsRubricPublicFields() {
    Rubric rubric = rubric();

    RubricResponse response = rubricMapper.toResponse(rubric);

    assertThat(response.publicId()).isEqualTo(rubric.getPublicId());
    assertThat(response.projectPublicId()).isEqualTo(rubric.getProject().getPublicId());
    assertThat(response.name()).isEqualTo("Health Answer Quality Rubric");
    assertThat(response.description()).isEqualTo("Checks correctness and safety.");
    assertThat(response.currentVersion()).isEqualTo(2);
    assertThat(response.status()).isEqualTo(RubricStatus.ACTIVE);
    assertThat(response.createdAt()).isEqualTo(OffsetDateTime.parse("2026-06-08T10:30:00Z"));
    assertThat(response.updatedAt()).isEqualTo(OffsetDateTime.parse("2026-06-08T10:35:00Z"));
  }

  @Test
  void toListItemResponseMapsRubricSummaryFields() {
    Rubric rubric = rubric();

    RubricListItemResponse response = rubricMapper.toListItemResponse(rubric);

    assertThat(response.publicId()).isEqualTo(rubric.getPublicId());
    assertThat(response.projectPublicId()).isEqualTo(rubric.getProject().getPublicId());
    assertThat(response.name()).isEqualTo("Health Answer Quality Rubric");
    assertThat(response.currentVersion()).isEqualTo(2);
    assertThat(response.status()).isEqualTo(RubricStatus.ACTIVE);
  }

  @Test
  void toVersionResponseMapsVersionWithCriteria() {
    RubricVersion version = version(rubric());
    RubricCriterion criterion = criterion(version);

    RubricVersionResponse response = rubricMapper.toVersionResponse(version, List.of(criterion));

    assertThat(response.publicId()).isEqualTo(version.getPublicId());
    assertThat(response.rubricPublicId()).isEqualTo(version.getRubric().getPublicId());
    assertThat(response.version()).isEqualTo(2);
    assertThat(response.status()).isEqualTo(RubricVersionStatus.PUBLISHED);
    assertThat(response.publishedAt()).isEqualTo(OffsetDateTime.parse("2026-06-08T11:00:00Z"));
    assertThat(response.criteria()).hasSize(1);
    assertThat(response.criteria().getFirst().metricKey()).isEqualTo("correctness");
  }

  @Test
  void toVersionListItemResponseMapsCriterionCount() {
    RubricVersion version = version(rubric());

    RubricVersionListItemResponse response = rubricMapper.toVersionListItemResponse(version, 4);

    assertThat(response.publicId()).isEqualTo(version.getPublicId());
    assertThat(response.rubricPublicId()).isEqualTo(version.getRubric().getPublicId());
    assertThat(response.version()).isEqualTo(2);
    assertThat(response.status()).isEqualTo(RubricVersionStatus.PUBLISHED);
    assertThat(response.totalCriteria()).isEqualTo(4);
  }

  @Test
  void toCriterionResponseMapsCriterionPublicFields() {
    RubricVersion version = version(rubric());
    RubricCriterion criterion = criterion(version);

    RubricCriterionResponse response = rubricMapper.toCriterionResponse(criterion);

    assertThat(response.publicId()).isEqualTo(criterion.getPublicId());
    assertThat(response.rubricVersionPublicId()).isEqualTo(version.getPublicId());
    assertThat(response.name()).isEqualTo("Correctness");
    assertThat(response.weight()).isEqualByComparingTo("0.4000");
    assertThat(response.passCondition()).isEqualTo("Facts match.");
    assertThat(response.failCondition()).isEqualTo("Facts are wrong.");
    assertThat(response.judgeInstruction()).isEqualTo("Compare with ground truth.");
    assertThat(response.metricKey()).isEqualTo("correctness");
    assertThat(response.isCritical()).isTrue();
    assertThat(response.sortOrder()).isEqualTo(1);
  }

  private Rubric rubric() {
    Project project = new Project();
    project.setPublicId(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"));
    Rubric rubric = new Rubric();
    rubric.setPublicId(UUID.fromString("3c5582c5-96d8-40e4-9aa1-168f6d27c9dc"));
    rubric.setProject(project);
    rubric.setName("Health Answer Quality Rubric");
    rubric.setDescription("Checks correctness and safety.");
    rubric.setCurrentVersion(2);
    rubric.setStatus(RubricStatus.ACTIVE);
    rubric.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"));
    rubric.setUpdatedAt(OffsetDateTime.parse("2026-06-08T10:35:00Z"));
    return rubric;
  }

  private RubricVersion version(Rubric rubric) {
    RubricVersion version = new RubricVersion();
    version.setPublicId(UUID.fromString("5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a"));
    version.setRubric(rubric);
    version.setVersion(2);
    version.setStatus(RubricVersionStatus.PUBLISHED);
    version.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:45:00Z"));
    version.setPublishedAt(OffsetDateTime.parse("2026-06-08T11:00:00Z"));
    return version;
  }

  private RubricCriterion criterion(RubricVersion version) {
    RubricCriterion criterion = new RubricCriterion();
    criterion.setPublicId(UUID.fromString("d10d218f-0e3c-4771-bf80-9815751f6460"));
    criterion.setRubricVersion(version);
    criterion.setName("Correctness");
    criterion.setDescription("Checks factual match.");
    criterion.setWeight(new BigDecimal("0.4000"));
    criterion.setPassCondition("Facts match.");
    criterion.setFailCondition("Facts are wrong.");
    criterion.setJudgeInstruction("Compare with ground truth.");
    criterion.setMetricKey("correctness");
    criterion.setCritical(true);
    criterion.setSortOrder(1);
    criterion.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:50:00Z"));
    criterion.setUpdatedAt(OffsetDateTime.parse("2026-06-08T10:55:00Z"));
    return criterion;
  }
}
