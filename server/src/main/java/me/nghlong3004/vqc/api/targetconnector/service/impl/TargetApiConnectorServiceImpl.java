package me.nghlong3004.vqc.api.targetconnector.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.repository.ProjectRepository;
import me.nghlong3004.vqc.api.targetconnector.client.TargetConnectorClient;
import me.nghlong3004.vqc.api.targetconnector.client.TargetConnectorClientRequest;
import me.nghlong3004.vqc.api.targetconnector.client.TargetConnectorClientResult;
import me.nghlong3004.vqc.api.targetconnector.curl.ConnectorSecretDetector;
import me.nghlong3004.vqc.api.targetconnector.curl.CurlParseResult;
import me.nghlong3004.vqc.api.targetconnector.curl.CurlParser;
import me.nghlong3004.vqc.api.targetconnector.curl.ResponseSelectorDetector;
import me.nghlong3004.vqc.api.targetconnector.curl.SecretDetectionResult;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.targetconnector.enums.AuthType;
import me.nghlong3004.vqc.api.targetconnector.enums.BodyType;
import me.nghlong3004.vqc.api.targetconnector.enums.ConnectorProtocol;
import me.nghlong3004.vqc.api.targetconnector.enums.ResponseFormat;
import me.nghlong3004.vqc.api.targetconnector.mapper.TargetApiConnectorMapper;
import me.nghlong3004.vqc.api.targetconnector.repository.TargetApiConnectorRepository;
import me.nghlong3004.vqc.api.targetconnector.request.CreateConnectorFromCurlRequest;
import me.nghlong3004.vqc.api.targetconnector.request.CreateTargetApiConnectorRequest;
import me.nghlong3004.vqc.api.targetconnector.request.TestTargetConnectorRequest;
import me.nghlong3004.vqc.api.targetconnector.request.UpdateConnectorFromCurlRequest;
import me.nghlong3004.vqc.api.targetconnector.request.UpdateTargetApiConnectorRequest;
import me.nghlong3004.vqc.api.targetconnector.response.CreateConnectorFromCurlResponse;
import me.nghlong3004.vqc.api.targetconnector.response.TargetApiConnectorListItemResponse;
import me.nghlong3004.vqc.api.targetconnector.response.TargetApiConnectorPageResponse;
import me.nghlong3004.vqc.api.targetconnector.response.TargetApiConnectorResponse;
import me.nghlong3004.vqc.api.targetconnector.response.TargetConnectorRequestPreview;
import me.nghlong3004.vqc.api.targetconnector.response.TestTargetConnectorResponse;
import me.nghlong3004.vqc.api.targetconnector.service.ConnectorSecretService;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class TargetApiConnectorServiceImpl implements TargetApiConnectorService {

  private final TargetApiConnectorRepository targetApiConnectorRepository;
  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final TargetApiConnectorMapper targetApiConnectorMapper;
  private final TargetConnectorClient targetConnectorClient;
  private final ConnectorSecretService connectorSecretService;
  private final CurlParser curlParser;
  private final ConnectorSecretDetector connectorSecretDetector;
  private final ResponseSelectorDetector responseSelectorDetector;

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
    connectorSecretService.saveSecrets(saved, request.secretValues());
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
    TargetApiConnector connector = findConnector(connectorPublicId, username);
    return targetApiConnectorMapper.toResponse(connector);
  }

  @Override
  @Transactional
  public TargetApiConnectorResponse updateConnector(
      UUID connectorPublicId, UpdateTargetApiConnectorRequest request, String username) {
    TargetApiConnector connector = findConnector(connectorPublicId, username);
    if (request.name() != null) {
      connector.setName(request.name().trim());
    }
    if (request.description() != null) {
      connector.setDescription(trimToNull(request.description()));
    }
    if (request.rawCurl() != null) {
      connector.setRawCurl(trimToNull(request.rawCurl()));
    }
    if (request.protocol() != null) {
      connector.setProtocol(request.protocol());
    }
    if (request.method() != null) {
      connector.setMethod(request.method());
    }
    if (request.baseUrl() != null) {
      connector.setBaseUrl(trimToNull(request.baseUrl()));
    }
    if (request.path() != null) {
      connector.setPath(trimToNull(request.path()));
    }
    if (request.url() != null) {
      connector.setUrl(request.url().trim());
    }
    if (request.headers() != null) {
      connector.setHeaders(safeMap(request.headers(), request.secretValues()));
    }
    if (request.queryParams() != null) {
      connector.setQueryParams(safeMap(request.queryParams(), request.secretValues()));
    }
    if (request.pathParams() != null) {
      connector.setPathParams(safeMap(request.pathParams(), request.secretValues()));
    }
    if (request.bodyType() != null) {
      connector.setBodyType(request.bodyType());
    }
    if (request.bodyTemplate() != null) {
      connector.setBodyTemplate(safeMap(request.bodyTemplate(), request.secretValues()));
    }
    if (request.bodyTemplateText() != null) {
      connector.setBodyTemplateText(trimToNull(request.bodyTemplateText()));
    }
    if (request.authType() != null) {
      connector.setAuthType(request.authType());
    }
    if (request.authConfig() != null) {
      connector.setAuthConfig(safeMap(request.authConfig(), request.secretValues()));
    }
    if (request.secretValues() != null) {
      connector.setSecretRefs(secretRefs(request.secretValues()));
    }
    if (request.responseFormat() != null) {
      connector.setResponseFormat(request.responseFormat());
    }
    if (request.responseSelector() != null) {
      connector.setResponseSelector(request.responseSelector().trim());
    }
    if (request.isStreaming() != null) {
      connector.setStreaming(request.isStreaming());
    }
    if (request.streamingType() != null) {
      connector.setStreamingType(request.streamingType());
    }
    if (request.streamingEventSelector() != null) {
      connector.setStreamingEventSelector(trimToNull(request.streamingEventSelector()));
    }
    if (request.timeoutSeconds() != null) {
      connector.setTimeoutSeconds(request.timeoutSeconds());
    }
    if (request.retryCount() != null) {
      connector.setRetryCount(request.retryCount());
    }
    if (request.active() != null) {
      connector.setActive(request.active());
    }
    TargetApiConnector saved = targetApiConnectorRepository.save(connector);
    if (request.secretValues() != null) {
      connectorSecretService.saveSecrets(saved, request.secretValues());
    }
    return targetApiConnectorMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public TestTargetConnectorResponse testConnector(
      UUID connectorPublicId, TestTargetConnectorRequest request, String username) {
    TargetApiConnector connector = findConnector(connectorPublicId, username);
    Object renderedBody = renderValue(connector.getBodyTemplate(), request);
    Map<String, Object> headers = connector.getHeaders() == null ? Map.of() : connector.getHeaders();
    Map<String, String> decryptedSecrets = connectorSecretService.decryptSecrets(connector);
    Map<String, Object> resolvedHeaders = resolveSecretPlaceholders(headers, decryptedSecrets);
    Object resolvedBody = resolveSecretPlaceholders(renderedBody, decryptedSecrets);
    String resolvedUrl = resolveSecretString(connector.getUrl(), decryptedSecrets);
    TargetConnectorClientRequest clientRequest =
        new TargetConnectorClientRequest(
            connector.getMethod(), resolvedUrl, resolvedHeaders, resolvedBody);
    TargetConnectorClientResult clientResult =
        targetConnectorClient.execute(
            clientRequest, connector.getTimeoutSeconds(), connector.getRetryCount());
    return new TestTargetConnectorResponse(
        true,
        new TargetConnectorRequestPreview(
            connector.getMethod(), connector.getUrl(), maskHeaders(headers), renderedBody),
        clientResult.rawResponse(),
        extractAnswer(clientResult.rawResponse(), connector.getResponseSelector()),
        clientResult.latencyMs());
  }

  @Override
  @Transactional
  public CreateConnectorFromCurlResponse createConnectorFromCurl(
      UUID projectPublicId, CreateConnectorFromCurlRequest request, String username) {
    User creator = findCreator(username);
    Project project = findProject(projectPublicId, creator);

    // 1. Parse cURL
    CurlParseResult parsed = curlParser.parse(request.rawCurl());

    // 2. Detect secrets in headers
    SecretDetectionResult secretResult = connectorSecretDetector.detect(parsed.headers());

    // 3. Determine auth type from headers
    AuthType authType = detectAuthType(parsed.headers());

    // 4. Build body fields
    BodyType bodyType = parsed.isJsonBody() ? BodyType.RAW_JSON : (parsed.bodyRaw() != null ? BodyType.RAW_TEXT : BodyType.NONE);
    Map<String, Object> bodyTemplate = parsed.bodyJson();
    String bodyTemplateText = parsed.isJsonBody() ? null : parsed.bodyRaw();

    // 5. Sanitize headers as Map<String, Object>
    Map<String, Object> sanitizedHeaders = new LinkedHashMap<>(secretResult.sanitizedHeaders());

    // 6. Build timeout/retry
    int timeoutSeconds = defaultValue(request.timeoutSeconds(), 60);
    int retryCount = defaultValue(request.retryCount(), 1);

    // 7. Test-call the target API (using raw headers so secrets resolve)
    Map<String, Object> rawHeaders = new LinkedHashMap<>(parsed.headers());
    TargetConnectorClientRequest clientRequest =
        new TargetConnectorClientRequest(
            parsed.method(),
            parsed.url(),
            rawHeaders,
            parsed.isJsonBody() ? parsed.bodyJson() : parsed.bodyRaw());

    TargetConnectorClientResult clientResult;
    try {
      clientResult = targetConnectorClient.execute(clientRequest, timeoutSeconds, retryCount);
    } catch (Exception ex) {
      log.warn("cURL test-call failed for URL {}: {}", parsed.url(), ex.getMessage());
      throw new ResourceException(ErrorCode.TARGET_CONNECTOR_TEST_FAILED);
    }

    // 8. Detect response selector
    String responseSelector =
        (request.responseSelector() != null && !request.responseSelector().isBlank())
            ? request.responseSelector().trim()
            : responseSelectorDetector.detect(clientResult.rawResponse());

    // 9. Build and save connector
    TargetApiConnector connector =
        TargetApiConnector.builder()
            .project(project)
            .name(request.name().trim())
            .description(trimToNull(request.description()))
            .rawCurl(request.rawCurl())
            .protocol(ConnectorProtocol.HTTP)
            .method(parsed.method())
            .url(parsed.url())
            .headers(sanitizedHeaders)
            .bodyType(bodyType)
            .bodyTemplate(bodyTemplate)
            .bodyTemplateText(bodyTemplateText)
            .authType(authType)
            .secretRefs(secretRefs(secretResult.secretValues()))
            .responseFormat(ResponseFormat.JSON)
            .responseSelector(responseSelector)
            .streaming(false)
            .timeoutSeconds(timeoutSeconds)
            .retryCount(retryCount)
            .active(true)
            .createdBy(creator)
            .build();
    TargetApiConnector saved = targetApiConnectorRepository.save(connector);
    connectorSecretService.saveSecrets(saved, secretResult.secretValues());

    // 10. Build response
    TargetApiConnectorResponse connectorResponse = targetApiConnectorMapper.toResponse(saved);
    TargetConnectorRequestPreview preview =
        new TargetConnectorRequestPreview(
            parsed.method(), parsed.url(), maskHeaders(rawHeaders), clientRequest.body());
    String extractedAnswer =
        extractAnswer(clientResult.rawResponse(), responseSelector);

    return new CreateConnectorFromCurlResponse(
        connectorResponse,
        preview,
        clientResult.rawResponse(),
        extractedAnswer,
        clientResult.latencyMs());
  }

  @Override
  @Transactional
  public CreateConnectorFromCurlResponse updateConnectorFromCurl(
      UUID connectorPublicId, UpdateConnectorFromCurlRequest request, String username) {
    TargetApiConnector connector = findConnector(connectorPublicId, username);

    // 1. Parse cURL if provided
    CurlParseResult parsed = null;
    SecretDetectionResult secretResult = null;
    AuthType authType = null;
    BodyType bodyType = null;
    Map<String, Object> bodyTemplate = null;
    String bodyTemplateText = null;
    Map<String, Object> sanitizedHeaders = null;
    Map<String, Object> rawHeaders = null;

    if (request.rawCurl() != null) {
      parsed = curlParser.parse(request.rawCurl());
      secretResult = connectorSecretDetector.detect(parsed.headers());
      authType = detectAuthType(parsed.headers());
      bodyType = parsed.isJsonBody() ? BodyType.RAW_JSON : (parsed.bodyRaw() != null ? BodyType.RAW_TEXT : BodyType.NONE);
      bodyTemplate = parsed.bodyJson();
      bodyTemplateText = parsed.isJsonBody() ? null : parsed.bodyRaw();
      sanitizedHeaders = new LinkedHashMap<>(secretResult.sanitizedHeaders());
      rawHeaders = new LinkedHashMap<>(parsed.headers());
    }

    // 2. Build timeout/retry
    int timeoutSeconds = request.timeoutSeconds() != null ? request.timeoutSeconds() : connector.getTimeoutSeconds();
    int retryCount = request.retryCount() != null ? request.retryCount() : connector.getRetryCount();

    TargetConnectorClientResult clientResult = null;
    String responseSelector = request.responseSelector() != null ? request.responseSelector().trim() : connector.getResponseSelector();

    // 3. If cURL is provided, test-call the target API
    if (parsed != null) {
      TargetConnectorClientRequest clientRequest =
          new TargetConnectorClientRequest(
              parsed.method(),
              parsed.url(),
              rawHeaders,
              parsed.isJsonBody() ? parsed.bodyJson() : parsed.bodyRaw());

      try {
        clientResult = targetConnectorClient.execute(clientRequest, timeoutSeconds, retryCount);
      } catch (Exception ex) {
        log.warn("cURL test-call failed for URL {}: {}", parsed.url(), ex.getMessage());
        throw new ResourceException(ErrorCode.TARGET_CONNECTOR_TEST_FAILED);
      }

      if (request.responseSelector() == null) {
        responseSelector = responseSelectorDetector.detect(clientResult.rawResponse());
      }
    }

    // 4. Update connector fields
    if (request.name() != null) {
      connector.setName(request.name().trim());
    }
    if (request.description() != null) {
      connector.setDescription(trimToNull(request.description()));
    }
    if (request.active() != null) {
      connector.setActive(request.active());
    }
    connector.setTimeoutSeconds(timeoutSeconds);
    connector.setRetryCount(retryCount);
    connector.setResponseSelector(responseSelector);

    if (parsed != null) {
      connector.setRawCurl(request.rawCurl());
      connector.setProtocol(ConnectorProtocol.HTTP);
      connector.setMethod(parsed.method());
      connector.setUrl(parsed.url());
      connector.setHeaders(sanitizedHeaders);
      connector.setBodyType(bodyType);
      connector.setBodyTemplate(bodyTemplate);
      connector.setBodyTemplateText(bodyTemplateText);
      connector.setAuthType(authType);
      connector.setSecretRefs(secretRefs(secretResult.secretValues()));
    }

    TargetApiConnector saved = targetApiConnectorRepository.save(connector);
    
    if (parsed != null && secretResult != null) {
      connectorSecretService.saveSecrets(saved, secretResult.secretValues());
    }

    // 5. Build response
    TargetApiConnectorResponse connectorResponse = targetApiConnectorMapper.toResponse(saved);

    TargetConnectorRequestPreview preview = null;
    String extractedAnswer = null;
    Map<String, Object> rawResponse = null;
    Long latencyMs = null;

    if (clientResult != null && parsed != null) {
      preview = new TargetConnectorRequestPreview(
          parsed.method(), parsed.url(), maskHeaders(rawHeaders), parsed.isJsonBody() ? parsed.bodyJson() : parsed.bodyRaw());
      rawResponse = clientResult.rawResponse();
      extractedAnswer = extractAnswer(rawResponse, responseSelector);
      latencyMs = clientResult.latencyMs();
    }

    return new CreateConnectorFromCurlResponse(
        connectorResponse,
        preview,
        rawResponse,
        extractedAnswer,
        latencyMs);
  }

  private AuthType detectAuthType(Map<String, String> headers) {
    if (headers == null) {
      return AuthType.NONE;
    }
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      String name = entry.getKey().toLowerCase();
      if (name.equals("authorization")) {
        String value = entry.getValue().toLowerCase();
        if (value.startsWith("bearer ")) {
          return AuthType.BEARER;
        }
        if (value.startsWith("basic ")) {
          return AuthType.BASIC;
        }
        return AuthType.CUSTOM_HEADER;
      }
      if (name.contains("api-key") || name.contains("apikey") || name.contains("x-api-key")) {
        return AuthType.API_KEY;
      }
    }
    return AuthType.NONE;
  }

  private TargetApiConnector findConnector(UUID connectorPublicId, String username) {
    User creator = findCreator(username);
    return targetApiConnectorRepository
        .findByPublicIdAndCreatedBy(connectorPublicId, creator)
        .orElseThrow(() -> new ResourceException(ErrorCode.TARGET_CONNECTOR_NOT_FOUND));
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

  private Object renderValue(Object value, TestTargetConnectorRequest request) {
    if (value instanceof String text) {
      return renderString(text, request);
    }
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> rendered = new LinkedHashMap<>();
      map.forEach((key, item) -> rendered.put(String.valueOf(key), renderValue(item, request)));
      return rendered;
    }
    if (value instanceof List<?> list) {
      return list.stream().map(item -> renderValue(item, request)).toList();
    }
    return value;
  }

  private Object renderString(String value, TestTargetConnectorRequest request) {
    return switch (value) {
      case "{{question}}" -> request.question();
      case "{{precondition}}" -> request.precondition() == null ? Map.of() : request.precondition();
      case "{{metadata}}" -> request.metadata() == null ? Map.of() : request.metadata();
      default ->
          value
              .replace("{{question}}", request.question())
              .replace("{{precondition}}", String.valueOf(request.precondition()))
              .replace("{{metadata}}", String.valueOf(request.metadata()));
    };
  }

  private Map<String, Object> maskHeaders(Map<String, Object> headers) {
    if (headers == null || headers.isEmpty()) {
      return Map.of();
    }
    Map<String, Object> masked = new LinkedHashMap<>();
    headers.forEach((name, value) -> masked.put(name, maskSecretPlaceholders(String.valueOf(value))));
    return masked;
  }

  private String maskSecretPlaceholders(String value) {
    return value.replaceAll("\\{\\{secret:[^}]+}}", "********");
  }

  private String extractAnswer(Map<String, Object> rawResponse, String responseSelector) {
    if ("$.answer".equals(responseSelector) && rawResponse.get("answer") != null) {
      return rawResponse.get("answer").toString();
    }
    return null;
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

  @SuppressWarnings("unchecked")
  private Map<String, Object> resolveSecretPlaceholders(
      Map<String, Object> values, Map<String, String> decryptedSecrets) {
    if (values == null || values.isEmpty() || decryptedSecrets.isEmpty()) {
      return values == null ? Map.of() : values;
    }
    Map<String, Object> resolved = new LinkedHashMap<>();
    values.forEach((key, item) -> resolved.put(key, resolveSecretPlaceholders(item, decryptedSecrets)));
    return resolved;
  }

  @SuppressWarnings("unchecked")
  private Object resolveSecretPlaceholders(Object value, Map<String, String> decryptedSecrets) {
    if (value instanceof String text) {
      return resolveSecretString(text, decryptedSecrets);
    }
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> resolved = new LinkedHashMap<>();
      map.forEach((key, item) -> resolved.put(String.valueOf(key), resolveSecretPlaceholders(item, decryptedSecrets)));
      return resolved;
    }
    if (value instanceof List<?> list) {
      return list.stream().map(item -> resolveSecretPlaceholders(item, decryptedSecrets)).toList();
    }
    return value;
  }

  private String resolveSecretString(String value, Map<String, String> decryptedSecrets) {
    if (value == null || decryptedSecrets.isEmpty()) {
      return value;
    }
    String resolved = value;
    for (Map.Entry<String, String> entry : decryptedSecrets.entrySet()) {
      resolved = resolved.replace("{{secret:" + entry.getKey() + "}}", entry.getValue());
    }
    return resolved;
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
