package me.nghlong3004.vqc.api.requirement.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.requirement.entity.BusinessRequirement;
import me.nghlong3004.vqc.api.requirement.enums.RequirementStatus;
import me.nghlong3004.vqc.api.requirement.response.RequirementListItemResponse;
import me.nghlong3004.vqc.api.requirement.response.RequirementResponse;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class RequirementMapperTest {

  private final RequirementMapper requirementMapper = new RequirementMapper();

  @Test
  void toResponseMapsPublicRequirementFields() {
    Project project = new Project();
    project.setPublicId(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"));
    BusinessRequirement requirement = new BusinessRequirement();
    requirement.setPublicId(UUID.fromString("ebd7f0f0-4924-4e81-9795-d1f060bec2f2"));
    requirement.setProject(project);
    requirement.setContent("Evaluate Apple Health step-count answers.");
    requirement.setVersion(2);
    requirement.setStatus(RequirementStatus.ACTIVE);
    requirement.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"));
    requirement.setUpdatedAt(OffsetDateTime.parse("2026-06-08T10:35:00Z"));

    RequirementResponse response = requirementMapper.toResponse(requirement);

    assertThat(response.publicId()).isEqualTo(requirement.getPublicId());
    assertThat(response.projectPublicId()).isEqualTo(project.getPublicId());
    assertThat(response.content()).isEqualTo("Evaluate Apple Health step-count answers.");
    assertThat(response.version()).isEqualTo(2);
    assertThat(response.status()).isEqualTo(RequirementStatus.ACTIVE);
    assertThat(response.createdAt()).isEqualTo(OffsetDateTime.parse("2026-06-08T10:30:00Z"));
    assertThat(response.updatedAt()).isEqualTo(OffsetDateTime.parse("2026-06-08T10:35:00Z"));
  }

  @Test
  void toListItemResponseMapsPublicRequirementSummaryFields() {
    Project project = new Project();
    project.setPublicId(UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"));
    BusinessRequirement requirement = new BusinessRequirement();
    requirement.setPublicId(UUID.fromString("ebd7f0f0-4924-4e81-9795-d1f060bec2f2"));
    requirement.setProject(project);
    requirement.setContent("Evaluate Apple Health step-count answers.");
    requirement.setVersion(1);
    requirement.setStatus(RequirementStatus.ACTIVE);
    requirement.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"));

    RequirementListItemResponse response = requirementMapper.toListItemResponse(requirement);

    assertThat(response.publicId()).isEqualTo(requirement.getPublicId());
    assertThat(response.projectPublicId()).isEqualTo(project.getPublicId());
    assertThat(response.content()).isEqualTo("Evaluate Apple Health step-count answers.");
    assertThat(response.version()).isEqualTo(1);
    assertThat(response.status()).isEqualTo(RequirementStatus.ACTIVE);
    assertThat(response.createdAt()).isEqualTo(OffsetDateTime.parse("2026-06-08T10:30:00Z"));
  }
}
