package me.nghlong3004.vqc.api.targetconnector.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.repository.ProjectRepository;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.targetconnector.enums.AuthType;
import me.nghlong3004.vqc.api.targetconnector.enums.BodyType;
import me.nghlong3004.vqc.api.targetconnector.enums.ConnectorProtocol;
import me.nghlong3004.vqc.api.targetconnector.enums.ResponseFormat;
import me.nghlong3004.vqc.api.targetconnector.mapper.TargetApiConnectorMapper;
import me.nghlong3004.vqc.api.targetconnector.repository.TargetApiConnectorRepository;
import me.nghlong3004.vqc.api.targetconnector.request.CreateTargetApiConnectorRequest;
import me.nghlong3004.vqc.api.targetconnector.response.TargetApiConnectorListItemResponse;
import me.nghlong3004.vqc.api.targetconnector.response.TargetApiConnectorPageResponse;
import me.nghlong3004.vqc.api.targetconnector.response.TargetApiConnectorResponse;
import me.nghlong3004.vqc.api.targetconnector.service.TargetApiConnectorService;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Service
@RequiredArgsConstructor
public class TargetApiConnectorServiceImpl implements TargetApiConnectorService {

  private final TargetApiConnectorRepository targetApiConnectorRepository;
  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final TargetApiConnectorMapper targetApiConnectorMapper;

  @Override
  @Transactional
  public TargetApiConnectorResponse createConnector(
      UUID projectPublicId, CreateTargetApiConnectorRequest request, String username) {
    User creator = findCreator(username);
    Project project = findProject(projectPublicId, creator);
    TargetApiConnector connector =
        TargetApiConnector.builder()
            .project(project)
            .name(request.name().trim())
            .description(trimToNull(request.description()))
            .rawCurl(trimToNull(request.rawCurl()))
            .protocol(defaultValue(request.protocol(), ConnectorProtocol.HTTP))
            .method(request.method())
            .baseUrl(trimToNull(request.baseUrl()))
            .path(trimToNull(request.path()))
            .url(request.url().trim())
            .headers(safeMap(request.headers(), request.secretValues()))
            .queryParams(safeMap(request.queryParams(), request.secretValues()))
            .pathParams(safeMap(request.pathParams(), request.secretValues()))
            .bodyType(defaultValue(request.bodyType(), BodyType.RAW_JSON))
            .bodyTemplate(safeMap(request.bodyTemplate(), request.secretValues()))
            .bodyTemplateText(trimToNull(request.bodyTemplateText()))
            .authType(defaultValue(request.authType(), AuthType.NONE))
            .authConfig(safeMap(request.authConfig(), request.secretValues()))
            .secretRefs(secretRefs(request.secretValues()))
            .responseFormat(defaultValue(request.responseFormat(), ResponseFormat.JSON))
            .responseSelector(request.responseSelector().trim())
            .streaming(defaultValue(request.isStreaming(), false))
            .streamingType(request.streamingType())
            .streamingEventSelector(trimToNull(request.streamingEventSelector()))
            .timeoutSeconds(defaultValue(request.timeoutSeconds(), 60))
            .retryCount(defaultValue(request.retryCount(), 1))
            .active(defaultValue(request.active(), true))
            .createdBy(creator)
            .build();
    TargetApiConnector saved = targetApiConnectorRepository.save(connector);
    return targetApiConnectorMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public TargetApiConnectorPageResponse listConnectors(
      UUID projectPublicId, Pageable pageable, String username) {
    User creator = findCreator(username);
    Project project = findProject(projectPublicId, creator);
    Page<TargetApiConnector> connectors = targetApiConnectorRepository.findByProject(project, pageable);
    List<TargetApiConnectorListItemResponse> items =
        connectors.getContent().stream().map(targetApiConnectorMapper::toListItemResponse).toList();
    return new TargetApiConnectorPageResponse(
        items,
        connectors.getNumber(),
        connectors.getSize(),
        connectors.getTotalElements(),
        connectors.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true)
  public TargetApiConnectorResponse getConnector(UUID connectorPublicId, String username) {
    User creator = findCreator(username);
    TargetApiConnector connector =
        targetApiConnectorRepository
            .findByPublicIdAndCreatedBy(connectorPublicId, creator)
            .orElseThrow(() -> new ResourceException(ErrorCode.TARGET_CONNECTOR_NOT_FOUND));
    return targetApiConnectorMapper.toResponse(connector);
  }

  private Project findProject(UUID projectPublicId, User creator) {
    return projectRepository
        .findByPublicIdAndCreatedBy(projectPublicId, creator)
        .orElseThrow(() -> new ResourceException(ErrorCode.PROJECT_NOT_FOUND));
  }

  private User findCreator(String username) {
    return userRepository
        .findByUsername(username.trim().toLowerCase())
        .orElseThrow(() -> new ResourceException(ErrorCode.USER_NOT_FOUND));
  }

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private Map<String, Object> defaultMap(Map<String, Object> value) {
    return value == null ? Map.of() : value;
  }

  private Map<String, Object> safeMap(Map<String, Object> value, Map<String, String> secretValues) {
    if (value == null || value.isEmpty()) {
      return Map.of();
    }
    Map<String, Object> safe = new LinkedHashMap<>();
    value.forEach((key, item) -> safe.put(key, safeValue(item, secretValues)));
    return safe;
  }

  private Object safeValue(Object value, Map<String, String> secretValues) {
    if (value instanceof String text) {
      return replaceSecrets(text, secretValues);
    }
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> safe = new LinkedHashMap<>();
      map.forEach((key, item) -> safe.put(String.valueOf(key), safeValue(item, secretValues)));
      return safe;
    }
    if (value instanceof List<?> list) {
      return list.stream().map(item -> safeValue(item, secretValues)).toList();
    }
    return value;
  }

  private String replaceSecrets(String value, Map<String, String> secretValues) {
    if (secretValues == null || secretValues.isEmpty()) {
      return value;
    }
    String safe = value;
    for (Map.Entry<String, String> entry : secretValues.entrySet()) {
      String secret = entry.getValue();
      if (secret != null && !secret.isBlank()) {
        safe = safe.replace(secret.trim(), "{{secret:" + entry.getKey() + "}}");
      }
    }
    return safe;
  }

  private <T> T defaultValue(T value, T defaultValue) {
    return value == null ? defaultValue : value;
  }

  private List<Map<String, Object>> secretRefs(Map<String, String> secretValues) {
    if (secretValues == null || secretValues.isEmpty()) {
      return List.of();
    }
    return secretValues.entrySet().stream()
        .map(entry -> Map.<String, Object>of("secretKey", entry.getKey(), "maskedValue", mask(entry.getValue())))
        .toList();
  }

  private String mask(String value) {
    if (value == null || value.isBlank()) {
      return "****";
    }
    String trimmed = value.trim();
    if (trimmed.length() <= 4) {
      return "****";
    }
    return "****" + trimmed.substring(trimmed.length() - 4);
  }
}
