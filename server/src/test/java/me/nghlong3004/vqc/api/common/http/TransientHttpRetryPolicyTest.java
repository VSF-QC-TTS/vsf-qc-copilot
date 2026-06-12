package me.nghlong3004.vqc.api.common.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.http.HttpStatusCode;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
class TransientHttpRetryPolicyTest {

  private final TransientHttpRetryPolicy policy = new TransientHttpRetryPolicy();

  @Test
  void serverErrorIsRetryable() {
    var ex = HttpServerErrorException.create(HttpStatusCode.valueOf(500), "Internal Server Error", null, null, null);
    assertThat(policy.isRetryable(ex)).isTrue();
  }

  @Test
  void badGatewayIsRetryable() {
    var ex = HttpServerErrorException.create(HttpStatusCode.valueOf(502), "Bad Gateway", null, null, null);
    assertThat(policy.isRetryable(ex)).isTrue();
  }

  @Test
  void serviceUnavailableIsRetryable() {
    var ex = HttpServerErrorException.create(HttpStatusCode.valueOf(503), "Service Unavailable", null, null, null);
    assertThat(policy.isRetryable(ex)).isTrue();
  }

  @Test
  void resourceAccessExceptionIsRetryable() {
    var ex = new ResourceAccessException("Connection refused");
    assertThat(policy.isRetryable(ex)).isTrue();
  }

  @Test
  void tooManyRequestsIsRetryable() {
    var ex = HttpClientErrorException.create(HttpStatusCode.valueOf(429), "Too Many Requests", null, null, null);
    assertThat(policy.isRetryable(ex)).isTrue();
  }

  @Test
  void badRequestIsNotRetryable() {
    var ex = HttpClientErrorException.create(HttpStatusCode.valueOf(400), "Bad Request", null, null, null);
    assertThat(policy.isRetryable(ex)).isFalse();
  }

  @Test
  void notFoundIsNotRetryable() {
    var ex = HttpClientErrorException.create(HttpStatusCode.valueOf(404), "Not Found", null, null, null);
    assertThat(policy.isRetryable(ex)).isFalse();
  }

  @Test
  void unauthorizedIsNotRetryable() {
    var ex = HttpClientErrorException.create(HttpStatusCode.valueOf(401), "Unauthorized", null, null, null);
    assertThat(policy.isRetryable(ex)).isFalse();
  }

  @Test
  void genericExceptionIsNotRetryable() {
    var ex = new RuntimeException("unexpected");
    assertThat(policy.isRetryable(ex)).isFalse();
  }

  @Test
  void delayIsFixedOneSecond() {
    assertThat(policy.delayMs(1)).isEqualTo(1_000L);
    assertThat(policy.delayMs(2)).isEqualTo(1_000L);
    assertThat(policy.delayMs(5)).isEqualTo(1_000L);
  }
}
