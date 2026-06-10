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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.nghlong3004.vqc.api.rubric.enums.RubricVersionStatus;
import me.nghlong3004.vqc.api.user.entity.User;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Entity
@Table(
    name = "rubric_versions",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_rubric_versions_rubric_version",
          columnNames = {"rubric_id", "version"})
    },
    indexes = {
      @Index(name = "idx_rubric_versions_rubric_id", columnList = "rubric_id"),
      @Index(name = "idx_rubric_versions_status", columnList = "status"),
      @Index(name = "idx_rubric_versions_created_by", columnList = "created_by")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricVersion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, updatable = false, unique = true)
  @Builder.Default
  private UUID publicId = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "rubric_id", nullable = false)
  private Rubric rubric;

  @Column(nullable = false)
  private Integer version;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  @Builder.Default
  private RubricVersionStatus status = RubricVersionStatus.DRAFT;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column(name = "published_at")
  private OffsetDateTime publishedAt;

  @PrePersist
  void prePersist() {
    if (publicId == null) {
      publicId = UUID.randomUUID();
    }
    if (createdAt == null) {
      createdAt = OffsetDateTime.now();
    }
    if (status == null) {
      status = RubricVersionStatus.DRAFT;
    }
  }
}
