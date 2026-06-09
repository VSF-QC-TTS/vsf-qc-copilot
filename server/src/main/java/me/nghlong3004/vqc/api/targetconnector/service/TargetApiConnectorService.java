package me.nghlong3004.vqc.api.targetconnector.service;

import java.util.UUID;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.targetconnector.request.CreateTargetApiConnectorRequest;
import me.nghlong3004.vqc.api.targetconnector.response.TargetApiConnectorResponse;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface TargetApiConnectorService {

  /**
   * Creates a {@link TargetApiConnector} under a project owned by the authenticated user.
   *
   * @param projectPublicId public project identifier
   * @param request validated create connector request
   * @param username normalized or raw username from the authenticated principal
   * @return public {@link TargetApiConnectorResponse}
   */
  TargetApiConnectorResponse createConnector(
      UUID projectPublicId, CreateTargetApiConnectorRequest request, String username);
}
