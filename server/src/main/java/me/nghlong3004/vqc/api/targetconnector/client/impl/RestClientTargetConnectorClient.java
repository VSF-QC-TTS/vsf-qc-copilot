package me.nghlong3004.vqc.api.targetconnector.client.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.targetconnector.client.TargetConnectorClient;
import me.nghlong3004.vqc.api.targetconnector.client.TargetConnectorClientRequest;
import me.nghlong3004.vqc.api.targetconnector.client.TargetConnectorClientResult;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Component
@RequiredArgsConstructor
public class RestClientTargetConnectorClient implements TargetConnectorClient {

  private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {};

  private final RestClient.Builder restClientBuilder;

  @Override
  public TargetConnectorClientResult execute(
      TargetConnectorClientRequest request, int timeoutSeconds) {
    Instant startedAt = Instant.now();
    Map<String, Object> response =
        restClientBuilder
            .build()
            .method(HttpMethod.valueOf(request.method().name()))
            .uri(request.url())
            .headers(headers -> request.headers().forEach((name, value) -> headers.set(name, String.valueOf(value))))
            .body(request.body())
            .retrieve()
            .body(MAP_RESPONSE_TYPE);
    return new TargetConnectorClientResult(
        response == null ? Map.of() : response, Duration.between(startedAt, Instant.now()).toMillis());
  }
}
