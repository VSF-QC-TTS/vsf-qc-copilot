package me.nghlong3004.vqc.api.redteam.entity;

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
import me.nghlong3004.vqc.api.judge.entity.JudgeModel;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.redteam.enums.RedTeamRunStatus;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.user.entity.User;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Entity
@Table(
    name = "red_team_runs",
    indexes = {
      @Index(name = "idx_red_team_runs_project_id", columnList = "project_id"),
      @Index(name = "idx_red_team_runs_status", columnList = "status"),
      @Index(name = "idx_red_team_runs_created_at", columnList = "created_at")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedTeamRun {

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
  @JoinColumn(name = "target_api_connector_id", nullable = false)
  private TargetApiConnector targetApiConnector;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "judge_model_id")
  private JudgeModel judgeModel;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_id")
  private Job job;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String purpose;

  @Column(name = "plugins_json", columnDefinition = "JSONB")
  private String pluginsJson;

  @Column(name = "strategies_json", columnDefinition = "JSONB")
  private String strategiesJson;

  @Column(name = "num_tests", nullable = false)
  @Builder.Default
  private Integer numTests = 1;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  @Builder.Default
  private RedTeamRunStatus status = RedTeamRunStatus.PENDING;

  @Column(name = "total_cases", nullable = false)
  @Builder.Default
  private Integer totalCases = 0;

  @Column(name = "passed_cases", nullable = false)
  @Builder.Default
  private Integer passedCases = 0;

  @Column(name = "failed_cases", nullable = false)
  @Builder.Default
  private Integer failedCases = 0;

  @Column(name = "error_cases", nullable = false)
  @Builder.Default
  private Integer errorCases = 0;

  @Column(name = "artifact_dir", columnDefinition = "TEXT")
  private String artifactDir;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

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
      status = RedTeamRunStatus.PENDING;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = OffsetDateTime.now();
  }
}
