package me.nghlong3004.vqc.api.common.http;

/**
 * Decides whether an HTTP request should be retried after a failure and the delay between attempts.
 *
 * <p>Single Responsibility: the policy only classifies errors and computes delay; the caller owns
 * the retry loop.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
public interface HttpRetryPolicy {

  /**
   * Returns {@code true} if the given exception represents a transient failure that may succeed on
   * retry.
   *
   * @param ex the exception thrown by the HTTP call
   * @return whether the request should be retried
   */
  boolean isRetryable(Exception ex);

  /**
   * Returns the delay in milliseconds before the given retry attempt.
   *
   * @param attempt 1-based retry attempt number
   * @return delay in milliseconds
   */
  long delayMs(int attempt);
}
