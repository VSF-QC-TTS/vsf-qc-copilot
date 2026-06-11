package me.nghlong3004.vqc.api.evaluation.entity;

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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.evaluation.enums.EvaluationRunStatus;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.rubric.entity.RubricVersion;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.user.entity.User;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Entity
@Table(
    name = "evaluation_runs",
    indexes = {
      @Index(name = "idx_evaluation_runs_project_id", columnList = "project_id"),
      @Index(name = "idx_evaluation_runs_status", columnList = "status"),
      @Index(name = "idx_evaluation_runs_created_at", columnList = "created_at"),
      @Index(
          name = "idx_evaluation_runs_connector_id",
          columnList = "target_api_connector_id")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRun {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, updatable = false, unique = true)
  @Builder.Default
  private UUID publicId = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "dataset_id", nullable = false)
  private Dataset dataset;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "rubric_version_id", nullable = false)
  private RubricVersion rubricVersion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "target_api_connector_id", nullable = false)
  private TargetApiConnector targetApiConnector;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_id")
  private Job job;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  @Builder.Default
  private EvaluationRunStatus status = EvaluationRunStatus.PENDING;

  @Column(name = "total_cases", nullable = false)
  @Builder.Default
  private Integer totalCases = 0;

  @Column(name = "passed_cases", nullable = false)
  @Builder.Default
  private Integer passedCases = 0;

  @Column(name = "failed_cases", nullable = false)
  @Builder.Default
  private Integer failedCases = 0;

  @Column(name = "warning_cases", nullable = false)
  @Builder.Default
  private Integer warningCases = 0;

  @Column(name = "error_cases", nullable = false)
  @Builder.Default
  private Integer errorCases = 0;

  @Column(name = "pass_rate", precision = 6, scale = 4)
  private BigDecimal passRate;

  @Column(name = "max_concurrency")
  @Builder.Default
  private Integer maxConcurrency = 1;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @Column(name = "started_at")
  private OffsetDateTime startedAt;

  @Column(name = "completed_at")
  private OffsetDateTime completedAt;

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
      status = EvaluationRunStatus.PENDING;
    }
    if (totalCases == null) {
      totalCases = 0;
    }
    if (passedCases == null) {
      passedCases = 0;
    }
    if (failedCases == null) {
      failedCases = 0;
    }
    if (warningCases == null) {
      warningCases = 0;
    }
    if (errorCases == null) {
      errorCases = 0;
    }
    if (maxConcurrency == null) {
      maxConcurrency = 1;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = OffsetDateTime.now();
  }
}
