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
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Entity
@Table(
    name = "evaluation_results",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_evaluation_result_run_case",
          columnNames = {"evaluation_run_id", "test_case_id"})
    },
    indexes = {
      @Index(name = "idx_evaluation_results_run_id", columnList = "evaluation_run_id"),
      @Index(name = "idx_evaluation_results_test_case_id", columnList = "test_case_id"),
      @Index(name = "idx_evaluation_results_judge_status", columnList = "judge_status")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, updatable = false, unique = true)
  @Builder.Default
  private UUID publicId = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "evaluation_run_id", nullable = false)
  private EvaluationRun evaluationRun;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "test_case_id", nullable = false)
  private TestCase testCase;

  @Column(name = "actual_answer", columnDefinition = "TEXT")
  private String actualAnswer;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "raw_target_response_json", columnDefinition = "jsonb")
  private String rawTargetResponseJson;

  @Column(name = "judge_score", precision = 6, scale = 4)
  private BigDecimal judgeScore;

  @Enumerated(EnumType.STRING)
  @Column(name = "judge_status", nullable = false, length = 50)
  private JudgeStatus judgeStatus;

  @Column(name = "judge_reason", columnDefinition = "TEXT")
  private String judgeReason;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "criteria_results_json", columnDefinition = "jsonb")
  private String criteriaResultsJson;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "raw_promptfoo_result_json", columnDefinition = "jsonb")
  private String rawPromptfooResultJson;

  @Column(name = "latency_ms")
  private Integer latencyMs;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "token_usage_json", columnDefinition = "jsonb")
  private String tokenUsageJson;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @PrePersist
  void prePersist() {
    if (publicId == null) {
      publicId = UUID.randomUUID();
    }
    if (createdAt == null) {
      createdAt = OffsetDateTime.now();
    }
  }
}
