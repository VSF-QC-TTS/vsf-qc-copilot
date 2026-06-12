package me.nghlong3004.vqc.api.common.http;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Retries on transient HTTP errors: 5xx server errors, connection/timeout failures, and 429 Too
 * Many Requests. Does not retry on other 4xx client errors, parse errors, or auth failures.
 *
 * <p>Uses a fixed 1-second delay between retries — sufficient for MVP demo scale without
 * exponential backoff complexity.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
@Component
public class TransientHttpRetryPolicy implements HttpRetryPolicy {

  private static final long FIXED_DELAY_MS = 1_000L;

  @Override
  public boolean isRetryable(Exception ex) {
    if (ex instanceof HttpServerErrorException) {
      return true;
    }
    if (ex instanceof ResourceAccessException) {
      return true;
    }
    if (ex instanceof HttpClientErrorException clientEx) {
      return clientEx.getStatusCode().value() == 429;
    }
    return false;
  }

  @Override
  public long delayMs(int attempt) {
    return FIXED_DELAY_MS;
  }
}
