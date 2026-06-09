package me.nghlong3004.vqc.api.targetconnector.service;

import java.util.UUID;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.targetconnector.request.CreateTargetApiConnectorRequest;
import me.nghlong3004.vqc.api.targetconnector.request.TestTargetConnectorRequest;
import me.nghlong3004.vqc.api.targetconnector.request.UpdateTargetApiConnectorRequest;
import me.nghlong3004.vqc.api.targetconnector.response.TargetApiConnectorPageResponse;
import me.nghlong3004.vqc.api.targetconnector.response.TargetApiConnectorResponse;
import me.nghlong3004.vqc.api.targetconnector.response.TestTargetConnectorResponse;
import org.springframework.data.domain.Pageable;

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

  /**
   * Lists target connectors under a project owned by the authenticated user.
   *
   * @param projectPublicId public project identifier
   * @param pageable page and sort request
   * @param username normalized or raw username from the authenticated principal
   * @return paginated public {@link TargetApiConnectorPageResponse}
   */
  TargetApiConnectorPageResponse listConnectors(
      UUID projectPublicId, Pageable pageable, String username);

  /**
   * Gets a target connector owned by the authenticated user.
   *
   * @param connectorPublicId public connector identifier
   * @param username normalized or raw username from the authenticated principal
   * @return public {@link TargetApiConnectorResponse}
   */
  TargetApiConnectorResponse getConnector(UUID connectorPublicId, String username);

  /**
   * Updates a target connector owned by the authenticated user.
   *
   * @param connectorPublicId public connector identifier
   * @param request validated update connector request
   * @param username normalized or raw username from the authenticated principal
   * @return public {@link TargetApiConnectorResponse}
   */
  TargetApiConnectorResponse updateConnector(
      UUID connectorPublicId, UpdateTargetApiConnectorRequest request, String username);

  /**
   * Executes a test call against a target connector.
   *
   * @param connectorPublicId public connector identifier
   * @param request validated test-run request
   * @param username normalized or raw username from the authenticated principal
   * @return target connector test-run response
   */
  TestTargetConnectorResponse testConnector(
      UUID connectorPublicId, TestTargetConnectorRequest request, String username);
}
