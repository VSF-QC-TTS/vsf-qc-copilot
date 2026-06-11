package me.nghlong3004.vqc.api.export.entity;

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
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.export.enums.ExportFileStatus;
import me.nghlong3004.vqc.api.export.enums.ExportFileType;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.user.entity.User;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Entity
@Table(
    name = "export_files",
    indexes = {
      @Index(name = "idx_export_files_run_id", columnList = "evaluation_run_id"),
      @Index(name = "idx_export_files_status", columnList = "status"),
      @Index(name = "idx_export_files_project_id", columnList = "project_id")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportFile {

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
  @JoinColumn(name = "evaluation_run_id", nullable = false)
  private EvaluationRun evaluationRun;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_id")
  private Job job;

  @Enumerated(EnumType.STRING)
  @Column(name = "file_type", nullable = false, length = 50)
  private ExportFileType fileType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  @Builder.Default
  private ExportFileStatus status = ExportFileStatus.PENDING;

  @Column(name = "file_path", columnDefinition = "TEXT")
  private String filePath;

  @Column(name = "file_name")
  private String fileName;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column(name = "ready_at")
  private OffsetDateTime readyAt;

  @PrePersist
  void prePersist() {
    if (publicId == null) {
      publicId = UUID.randomUUID();
    }
    if (status == null) {
      status = ExportFileStatus.PENDING;
    }
    if (createdAt == null) {
      createdAt = OffsetDateTime.now();
    }
  }
}
