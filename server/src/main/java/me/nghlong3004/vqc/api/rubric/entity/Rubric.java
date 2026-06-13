package me.nghlong3004.vqc.api.rubric.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.rubric.enums.RubricStatus;
import me.nghlong3004.vqc.api.user.entity.User;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Entity
@Table(
    name = "rubrics",
    indexes = {
      @Index(name = "idx_rubrics_project_id", columnList = "project_id"),
      @Index(name = "idx_rubrics_status", columnList = "status"),
      @Index(name = "idx_rubrics_created_by", columnList = "created_by"),
      @Index(name = "idx_rubrics_is_template", columnList = "is_template")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rubric {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, updatable = false, unique = true)
  @Builder.Default
  private UUID publicId = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  private Project project;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "current_version")
  private Integer currentVersion;

  @Column(name = "is_template", nullable = false)
  @Builder.Default
  private Boolean isTemplate = false;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  @Builder.Default
  private RubricStatus status = RubricStatus.ACTIVE;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @Column(name = "archived_at")
  private OffsetDateTime archivedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column(name = "updated_at", nullable = false)
  @Builder.Default
  private OffsetDateTime updatedAt = OffsetDateTime.now();

  @PrePersist
  void prePersist() {
    OffsetDateTime now = OffsetDateTime.now();
    createdAt = now;
    updatedAt = now;
    if (publicId == null) {
      publicId = UUID.randomUUID();
    }
    if (status == null) {
      status = RubricStatus.ACTIVE;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = OffsetDateTime.now();
  }
}
