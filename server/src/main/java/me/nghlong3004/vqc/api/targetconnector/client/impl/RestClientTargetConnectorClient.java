package me.nghlong3004.vqc.api.targetconnector.client.impl;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.common.http.HttpRetryPolicy;
import me.nghlong3004.vqc.api.targetconnector.client.TargetConnectorClient;
import me.nghlong3004.vqc.api.targetconnector.client.TargetConnectorClientRequest;
import me.nghlong3004.vqc.api.targetconnector.client.TargetConnectorClientResult;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Executes HTTP requests against a target API connector with per-request timeout and retry.
 *
 * <p>Retry decisions are delegated to {@link HttpRetryPolicy} (SRP: policy decides retryability,
 * this class owns the retry loop).
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestClientTargetConnectorClient implements TargetConnectorClient {

  private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {};

  private final RestClient.Builder restClientBuilder;
  private final HttpRetryPolicy retryPolicy;

  @Override
  public TargetConnectorClientResult execute(
      TargetConnectorClientRequest request, int timeoutSeconds, int retryCount) {
    Instant startedAt = Instant.now();
    RestClient client = buildClientWithTimeout(timeoutSeconds);
    int maxAttempts = 1 + Math.max(retryCount, 0);
    Exception lastException = null;

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        Map<String, Object> response = doExecute(client, request);
        return new TargetConnectorClientResult(
            response == null ? Map.of() : response,
            Duration.between(startedAt, Instant.now()).toMillis());
      } catch (Exception ex) {
        lastException = ex;
        boolean hasMoreAttempts = attempt < maxAttempts;
        if (hasMoreAttempts && retryPolicy.isRetryable(ex)) {
          log.warn(
              "Retry {}/{} for connector call after {}: {}",
              attempt,
              retryCount,
              ex.getClass().getSimpleName(),
              ex.getMessage());
          sleep(retryPolicy.delayMs(attempt));
        } else {
          break;
        }
      }
    }
    throw wrapIfNeeded(lastException);
  }

  private Map<String, Object> doExecute(RestClient client, TargetConnectorClientRequest request) {
    return client
        .method(HttpMethod.valueOf(request.method().name()))
        .uri(request.url())
        .headers(
            headers ->
                request
                    .headers()
                    .forEach((name, value) -> headers.set(name, String.valueOf(value))))
        .body(request.body())
        .retrieve()
        .body(MAP_RESPONSE_TYPE);
  }

  private RestClient buildClientWithTimeout(int timeoutSeconds) {
    Duration timeout = Duration.ofSeconds(timeoutSeconds);
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(timeout);
    return restClientBuilder.clone().requestFactory(factory).build();
  }

  private static void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Retry interrupted", ex);
    }
  }

  private static RuntimeException wrapIfNeeded(Exception ex) {
    if (ex instanceof RuntimeException rte) {
      return rte;
    }
    return new RuntimeException(ex);
  }
}
