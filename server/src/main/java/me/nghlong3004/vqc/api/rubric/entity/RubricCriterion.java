package me.nghlong3004.vqc.api.rubric.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Entity
@Table(
    name = "rubric_criteria",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_rubric_criteria_version_metric_key",
          columnNames = {"rubric_version_id", "metric_key"})
    },
    indexes = {
      @Index(name = "idx_rubric_criteria_version_id", columnList = "rubric_version_id"),
      @Index(name = "idx_rubric_criteria_metric_key", columnList = "metric_key"),
      @Index(name = "idx_rubric_criteria_sort_order", columnList = "sort_order")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricCriterion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, updatable = false, unique = true)
  @Builder.Default
  private UUID publicId = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "rubric_version_id", nullable = false)
  private RubricVersion rubricVersion;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false, precision = 5, scale = 4)
  private BigDecimal weight;

  @Column(name = "pass_condition", columnDefinition = "TEXT")
  private String passCondition;

  @Column(name = "fail_condition", columnDefinition = "TEXT")
  private String failCondition;

  @Column(name = "judge_instruction", nullable = false, columnDefinition = "TEXT")
  private String judgeInstruction;

  @Column(name = "metric_key", nullable = false, length = 100)
  private String metricKey;

  @Column(name = "is_critical", nullable = false)
  @Builder.Default
  private Boolean critical = false;

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;

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
    if (critical == null) {
      critical = false;
    }
    if (sortOrder == null) {
      sortOrder = 0;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = OffsetDateTime.now();
  }
}
