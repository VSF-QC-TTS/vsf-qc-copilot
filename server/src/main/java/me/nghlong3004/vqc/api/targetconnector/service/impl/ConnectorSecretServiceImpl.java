package me.nghlong3004.vqc.api.targetconnector.service.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.common.crypto.AesGcmEncryptor;
import me.nghlong3004.vqc.api.targetconnector.entity.ConnectorSecret;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.targetconnector.repository.ConnectorSecretRepository;
import me.nghlong3004.vqc.api.targetconnector.service.ConnectorSecretService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Encrypts, persists, and decrypts connector secrets using AES-256-GCM.
 *
 * <p>Each call to {@link #saveSecrets} replaces all existing secrets for the connector. Masked
 * values are stored alongside ciphertext for safe display in API responses.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectorSecretServiceImpl implements ConnectorSecretService {

  private final ConnectorSecretRepository connectorSecretRepository;
  private final AesGcmEncryptor aesGcmEncryptor;

  @Override
  @Transactional
  public void saveSecrets(TargetApiConnector connector, Map<String, String> secretValues) {
    if (secretValues == null || secretValues.isEmpty()) {
      return;
    }
    connectorSecretRepository.deleteByConnector(connector);
    connectorSecretRepository.flush();
    for (Map.Entry<String, String> entry : secretValues.entrySet()) {
      String key = entry.getKey();
      String rawValue = entry.getValue();
      if (rawValue == null || rawValue.isBlank()) {
        continue;
      }
      ConnectorSecret secret =
          ConnectorSecret.builder()
              .connector(connector)
              .secretKey(key)
              .encryptedValue(aesGcmEncryptor.encrypt(rawValue.trim()))
              .maskedValue(mask(rawValue.trim()))
              .build();
      connectorSecretRepository.save(secret);
    }
    log.debug(
        "Saved {} encrypted secrets for connector {}",
        secretValues.size(),
        connector.getPublicId());
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, String> decryptSecrets(TargetApiConnector connector) {
    var secrets = connectorSecretRepository.findByConnector(connector);
    if (secrets.isEmpty()) {
      return Map.of();
    }
    Map<String, String> decrypted = new LinkedHashMap<>();
    for (ConnectorSecret secret : secrets) {
      decrypted.put(secret.getSecretKey(), aesGcmEncryptor.decrypt(secret.getEncryptedValue()));
    }
    return decrypted;
  }

  private String mask(String value) {
    if (value.length() <= 4) {
      return "****";
    }
    return "****" + value.substring(value.length() - 4);
  }
}
