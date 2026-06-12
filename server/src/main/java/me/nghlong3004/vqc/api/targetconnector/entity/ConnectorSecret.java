package me.nghlong3004.vqc.api.targetconnector.entity;

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
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Encrypted secret stored for a target API connector (e.g. API key, bearer token).
 *
 * <p>The {@code encryptedValue} contains the AES-256-GCM ciphertext (Base64-encoded), and
 * {@code maskedValue} is a safe display value like {@code ****xyz}.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
@Entity
@Table(
    name = "connector_secrets",
    indexes = @Index(name = "idx_connector_secrets_connector_id", columnList = "connector_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectorSecret {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "connector_id", nullable = false)
  private TargetApiConnector connector;

  @Column(name = "secret_key", nullable = false, length = 255)
  private String secretKey;

  @Column(name = "encrypted_value", nullable = false, columnDefinition = "TEXT")
  private String encryptedValue;

  @Column(name = "masked_value", nullable = false, length = 50)
  private String maskedValue;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  void prePersist() {
    OffsetDateTime now = OffsetDateTime.now();
    if (createdAt == null) createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = OffsetDateTime.now();
  }
}
