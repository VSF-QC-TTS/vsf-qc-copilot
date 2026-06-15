package me.nghlong3004.vqc.api.judge.entity;

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
import me.nghlong3004.vqc.api.judge.enums.JudgeProvider;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.user.entity.User;

/**
 * LLM model configuration used to grade evaluation runs.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Entity
@Table(
    name = "judge_models",
    indexes = {
      @Index(name = "idx_judge_models_project_id", columnList = "project_id"),
      @Index(name = "idx_judge_models_created_by", columnList = "created_by"),
      @Index(name = "idx_judge_models_active", columnList = "active")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeModel {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, updatable = false, unique = true)
  @Builder.Default
  private UUID publicId = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @Column(nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private JudgeProvider provider;

  @Column(name = "model_name", nullable = false)
  private String modelName;

  @Column(name = "base_url", columnDefinition = "TEXT")
  private String baseUrl;

  @Column(name = "encrypted_api_key", columnDefinition = "TEXT")
  private String encryptedApiKey;

  @Column(name = "api_key_masked", length = 100)
  private String apiKeyMasked;

  @Column(name = "config_json", columnDefinition = "TEXT")
  private String configJson;

  @Column(nullable = false)
  @Builder.Default
  private Boolean active = true;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column(name = "updated_at", nullable = false)
  @Builder.Default
  private OffsetDateTime updatedAt = OffsetDateTime.now();

  @PrePersist
  void prePersist() {
    OffsetDateTime now = OffsetDateTime.now();
    if (publicId == null) {
      publicId = UUID.randomUUID();
    }
    if (active == null) {
      active = true;
    }
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = OffsetDateTime.now();
  }
}
