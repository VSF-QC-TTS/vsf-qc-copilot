package me.nghlong3004.vqc.api.project.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.enums.ProjectStatus;
import me.nghlong3004.vqc.api.user.entity.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class ProjectMapperTest {

  private final ProjectMapper projectMapper = Mappers.getMapper(ProjectMapper.class);

  @Test
  void toResponseMapsPublicProjectFieldsAndCreator() {
    User creator = new User();
    creator.setDisplayName("QC Demo");
    Project project = new Project();
    project.setName("AI Health Chatbot Demo");
    project.setDescription("Evaluate health chatbot answers.");
    project.setEvaluationScope("Health assistant QA evaluation");
    project.setRetentionDays(30);
    project.setStatus(ProjectStatus.ACTIVE);
    project.setCreatedBy(creator);
    project.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"));
    project.setUpdatedAt(OffsetDateTime.parse("2026-06-08T10:35:00Z"));

    var response = projectMapper.toResponse(project);

    assertThat(response.publicId()).isEqualTo(project.getPublicId());
    assertThat(response.name()).isEqualTo("AI Health Chatbot Demo");
    assertThat(response.description()).isEqualTo("Evaluate health chatbot answers.");
    assertThat(response.evaluationScope()).isEqualTo("Health assistant QA evaluation");
    assertThat(response.retentionDays()).isEqualTo(30);
    assertThat(response.status()).isEqualTo(ProjectStatus.ACTIVE);
    assertThat(response.createdBy().publicId()).isEqualTo(creator.getPublicId());
    assertThat(response.createdBy().displayName()).isEqualTo("QC Demo");
    assertThat(response.createdAt()).isEqualTo(project.getCreatedAt());
    assertThat(response.updatedAt()).isEqualTo(project.getUpdatedAt());
  }

  @Test
  void toListItemResponseMapsListFieldsOnly() {
    Project project = new Project();
    project.setName("AI Health Chatbot Demo");
    project.setStatus(ProjectStatus.ACTIVE);
    project.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"));
    project.setUpdatedAt(OffsetDateTime.parse("2026-06-08T10:35:00Z"));

    var response = projectMapper.toListItemResponse(project);

    assertThat(response.publicId()).isEqualTo(project.getPublicId());
    assertThat(response.name()).isEqualTo("AI Health Chatbot Demo");
    assertThat(response.status()).isEqualTo(ProjectStatus.ACTIVE);
    assertThat(response.createdAt()).isEqualTo(project.getCreatedAt());
    assertThat(response.updatedAt()).isEqualTo(project.getUpdatedAt());
  }
}
