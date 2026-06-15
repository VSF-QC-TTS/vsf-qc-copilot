package me.nghlong3004.vqc.api.targetconnector.entity;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.targetconnector.enums.AuthType;
import me.nghlong3004.vqc.api.targetconnector.enums.BodyType;
import me.nghlong3004.vqc.api.targetconnector.enums.ConnectorProtocol;
import me.nghlong3004.vqc.api.targetconnector.enums.HttpMethodType;
import me.nghlong3004.vqc.api.targetconnector.enums.ResponseFormat;
import me.nghlong3004.vqc.api.targetconnector.enums.StreamingType;
import me.nghlong3004.vqc.api.user.entity.User;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Entity
@Table(
    name = "target_api_connectors",
    indexes = {
      @Index(name = "idx_target_api_connectors_project_id", columnList = "project_id"),
      @Index(name = "idx_target_api_connectors_active", columnList = "active"),
      @Index(name = "idx_target_api_connectors_created_by", columnList = "created_by")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetApiConnector {

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

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "raw_curl", columnDefinition = "TEXT")
  private String rawCurl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  @Builder.Default
  private ConnectorProtocol protocol = ConnectorProtocol.HTTP;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private HttpMethodType method;

  @Column(name = "base_url", columnDefinition = "TEXT")
  private String baseUrl;

  @Column(columnDefinition = "TEXT")
  private String path;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String url;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "headers_json", columnDefinition = "jsonb")
  private Map<String, Object> headers;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "query_params_json", columnDefinition = "jsonb")
  private Map<String, Object> queryParams;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "path_params_json", columnDefinition = "jsonb")
  private Map<String, Object> pathParams;

  @Enumerated(EnumType.STRING)
  @Column(name = "body_type", nullable = false, length = 50)
  @Builder.Default
  private BodyType bodyType = BodyType.RAW_JSON;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "body_template_json", columnDefinition = "jsonb")
  private Map<String, Object> bodyTemplate;

  @Column(name = "body_template_text", columnDefinition = "TEXT")
  private String bodyTemplateText;

  @Enumerated(EnumType.STRING)
  @Column(name = "auth_type", nullable = false, length = 50)
  @Builder.Default
  private AuthType authType = AuthType.NONE;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "auth_config_json", columnDefinition = "jsonb")
  private Map<String, Object> authConfig;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "secret_refs_json", columnDefinition = "jsonb")
  private List<Map<String, Object>> secretRefs;

  @Column(name = "is_streaming", nullable = false)
  @Builder.Default
  private Boolean streaming = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "streaming_type", length = 50)
  private StreamingType streamingType;

  @Column(name = "streaming_event_selector")
  private String streamingEventSelector;

  @Column(name = "response_selector", nullable = false)
  @Builder.Default
  private String responseSelector = "$.answer";

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "response_schema_json", columnDefinition = "jsonb")
  private Map<String, Object> responseSchema;

  @Enumerated(EnumType.STRING)
  @Column(name = "response_format", nullable = false, length = 50)
  @Builder.Default
  private ResponseFormat responseFormat = ResponseFormat.JSON;

  @Column(name = "timeout_seconds", nullable = false)
  @Builder.Default
  private Integer timeoutSeconds = 60;

  @Column(name = "retry_count", nullable = false)
  @Builder.Default
  private Integer retryCount = 1;

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
    createdAt = now;
    updatedAt = now;
    if (publicId == null) {
      publicId = UUID.randomUUID();
    }
    if (protocol == null) {
      protocol = ConnectorProtocol.HTTP;
    }
    if (bodyType == null) {
      bodyType = BodyType.RAW_JSON;
    }
    if (authType == null) {
      authType = AuthType.NONE;
    }
    if (streaming == null) {
      streaming = false;
    }
    if (responseSelector == null || responseSelector.isBlank()) {
      responseSelector = "$.answer";
    }
    if (responseFormat == null) {
      responseFormat = ResponseFormat.JSON;
    }
    if (timeoutSeconds == null) {
      timeoutSeconds = 60;
    }
    if (retryCount == null) {
      retryCount = 1;
    }
    if (active == null) {
      active = true;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = OffsetDateTime.now();
  }
}
