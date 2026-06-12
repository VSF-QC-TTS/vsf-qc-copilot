package me.nghlong3004.vqc.api.targetconnector.service;

import java.util.Map;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;

/**
 * Manages encrypted connector secrets (API keys, bearer tokens, etc.).
 *
 * <p>Raw secret values are encrypted with AES-256-GCM before persistence and decrypted only when
 * needed at runtime (e.g. Promptfoo evaluation, connector test-run).
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
public interface ConnectorSecretService {

  /**
   * Encrypt and persist secrets for a connector. Replaces all existing secrets for the connector.
   *
   * @param connector the target connector entity
   * @param secretValues raw secret key-value pairs (e.g. {@code CHATBOT_API_TOKEN -> sk-abc123})
   */
  void saveSecrets(TargetApiConnector connector, Map<String, String> secretValues);

  /**
   * Decrypt and return all persisted secrets for a connector.
   *
   * @param connector the target connector entity
   * @return map of secret key to decrypted plaintext value; empty map if no secrets exist
   */
  Map<String, String> decryptSecrets(TargetApiConnector connector);
}
