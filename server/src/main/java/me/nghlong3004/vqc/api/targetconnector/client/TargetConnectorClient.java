package me.nghlong3004.vqc.api.targetconnector.client;

/**
 * Executes HTTP requests against a target API connector.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface TargetConnectorClient {

  /**
   * Executes a target connector request with timeout and retry.
   *
   * @param request rendered target request
   * @param timeoutSeconds request timeout in seconds
   * @param retryCount number of retry attempts on transient failures (0 = no retry)
   * @return raw target response and latency
   */
  TargetConnectorClientResult execute(
      TargetConnectorClientRequest request, int timeoutSeconds, int retryCount);
}

