package me.nghlong3004.vqc.api.review.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationResult;
import me.nghlong3004.vqc.api.review.enums.QcStatus;
import me.nghlong3004.vqc.api.user.entity.User;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Entity
@Table(
    name = "review_decisions",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_review_decisions_result_id",
          columnNames = "evaluation_result_id")
    },
    indexes = {
      @Index(name = "idx_review_decisions_qc_status", columnList = "qc_status"),
      @Index(name = "idx_review_decisions_pic_bug_user_id", columnList = "pic_bug_user_id")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDecision {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, updatable = false, unique = true)
  @Builder.Default
  private UUID publicId = UUID.randomUUID();

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "evaluation_result_id", nullable = false)
  private EvaluationResult evaluationResult;

  @Enumerated(EnumType.STRING)
  @Column(name = "qc_status", nullable = false, length = 50)
  private QcStatus qcStatus;

  @Column(name = "qc_note", columnDefinition = "TEXT")
  private String qcNote;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pic_bug_user_id")
  private User picBugUser;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reviewed_by", nullable = false)
  private User reviewedBy;

  @Column(name = "reviewed_at", nullable = false)
  private OffsetDateTime reviewedAt;

  @Column(name = "updated_at", nullable = false)
  @Builder.Default
  private OffsetDateTime updatedAt = OffsetDateTime.now();

  /**
   * Applies a QC decision to this review record.
   *
   * @param status QC status
   * @param note optional QC note
   * @param picBugUser optional PIC bug user
   * @param reviewer authenticated reviewer
   */
  public void applyDecision(QcStatus status, String note, User picBugUser, User reviewer) {
    OffsetDateTime now = OffsetDateTime.now();
    this.qcStatus = status;
    this.qcNote = normalize(note);
    this.picBugUser = picBugUser;
    this.reviewedBy = reviewer;
    this.reviewedAt = now;
    this.updatedAt = now;
  }

  @PrePersist
  void prePersist() {
    OffsetDateTime now = OffsetDateTime.now();
    if (publicId == null) {
      publicId = UUID.randomUUID();
    }
    if (reviewedAt == null) {
      reviewedAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  private String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
