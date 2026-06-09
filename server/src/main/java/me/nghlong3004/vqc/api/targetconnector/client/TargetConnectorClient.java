package me.nghlong3004.vqc.api.targetconnector.client;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface TargetConnectorClient {

  /**
   * Executes a target connector request.
   *
   * @param request rendered target request
   * @param timeoutSeconds request timeout in seconds
   * @return raw target response and latency
   */
  TargetConnectorClientResult execute(TargetConnectorClientRequest request, int timeoutSeconds);
}
