package me.nghlong3004.vqc.api.targetconnector.client.impl;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.targetconnector.client.TargetConnectorClient;
import me.nghlong3004.vqc.api.targetconnector.client.TargetConnectorClientRequest;
import me.nghlong3004.vqc.api.targetconnector.client.TargetConnectorClientResult;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Executes HTTP requests against a target API connector with per-request timeout.
 *
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
    RestClient client = buildClientWithTimeout(timeoutSeconds);
    Map<String, Object> response =
        client
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
    return new TargetConnectorClientResult(
        response == null ? Map.of() : response,
        Duration.between(startedAt, Instant.now()).toMillis());
  }

  private RestClient buildClientWithTimeout(int timeoutSeconds) {
    Duration timeout = Duration.ofSeconds(timeoutSeconds);
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(timeout);
    return restClientBuilder.clone().requestFactory(factory).build();
  }
}
