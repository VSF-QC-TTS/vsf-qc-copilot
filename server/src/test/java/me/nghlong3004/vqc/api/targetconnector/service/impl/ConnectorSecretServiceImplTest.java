package me.nghlong3004.vqc.api.targetconnector.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import me.nghlong3004.vqc.api.common.crypto.AesGcmEncryptor;
import me.nghlong3004.vqc.api.targetconnector.entity.ConnectorSecret;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.targetconnector.repository.ConnectorSecretRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
@ExtendWith(MockitoExtension.class)
class ConnectorSecretServiceImplTest {

  private static final String TEST_HEX_KEY =
      "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

  @Mock private ConnectorSecretRepository connectorSecretRepository;
  @Mock private AesGcmEncryptor aesGcmEncryptor;

  @InjectMocks private ConnectorSecretServiceImpl connectorSecretService;

  @Test
  void saveSecretsEncryptsAndPersists() {
    TargetApiConnector connector = TargetApiConnector.builder().id(1L).build();
    when(aesGcmEncryptor.encrypt("sk-abc123")).thenReturn("encrypted-value");

    connectorSecretService.saveSecrets(connector, Map.of("API_TOKEN", "sk-abc123"));

    verify(connectorSecretRepository).deleteByConnector(connector);
    verify(connectorSecretRepository).flush();
    ArgumentCaptor<ConnectorSecret> captor = ArgumentCaptor.forClass(ConnectorSecret.class);
    verify(connectorSecretRepository).save(captor.capture());
    ConnectorSecret saved = captor.getValue();
    assertThat(saved.getSecretKey()).isEqualTo("API_TOKEN");
    assertThat(saved.getEncryptedValue()).isEqualTo("encrypted-value");
    assertThat(saved.getMaskedValue()).isEqualTo("****c123");
    assertThat(saved.getConnector()).isSameAs(connector);
  }

  @Test
  void saveSecretsSkipsNullMap() {
    TargetApiConnector connector = TargetApiConnector.builder().id(1L).build();

    connectorSecretService.saveSecrets(connector, null);

    verify(connectorSecretRepository, never()).deleteByConnector(any());
    verify(connectorSecretRepository, never()).save(any());
  }

  @Test
  void saveSecretsSkipsEmptyMap() {
    TargetApiConnector connector = TargetApiConnector.builder().id(1L).build();

    connectorSecretService.saveSecrets(connector, Map.of());

    verify(connectorSecretRepository, never()).deleteByConnector(any());
    verify(connectorSecretRepository, never()).save(any());
  }

  @Test
  void saveSecretsSkipsBlankValues() {
    TargetApiConnector connector = TargetApiConnector.builder().id(1L).build();

    connectorSecretService.saveSecrets(connector, Map.of("API_TOKEN", "   "));

    verify(connectorSecretRepository).deleteByConnector(connector);
    verify(connectorSecretRepository).flush();
    verify(connectorSecretRepository, never()).save(any());
  }

  @Test
  void saveSecretsReplacesExisting() {
    TargetApiConnector connector = TargetApiConnector.builder().id(1L).build();
    when(aesGcmEncryptor.encrypt("new-value")).thenReturn("encrypted-new");

    connectorSecretService.saveSecrets(connector, Map.of("TOKEN", "new-value"));

    verify(connectorSecretRepository).deleteByConnector(connector);
    verify(connectorSecretRepository).flush();
    verify(connectorSecretRepository).save(any(ConnectorSecret.class));
  }

  @Test
  void decryptSecretsReturnsDecryptedMap() {
    TargetApiConnector connector = TargetApiConnector.builder().id(1L).build();
    ConnectorSecret secret =
        ConnectorSecret.builder()
            .connector(connector)
            .secretKey("API_TOKEN")
            .encryptedValue("encrypted-value")
            .maskedValue("****c123")
            .build();
    when(connectorSecretRepository.findByConnector(connector)).thenReturn(List.of(secret));
    when(aesGcmEncryptor.decrypt("encrypted-value")).thenReturn("sk-abc123");

    Map<String, String> result = connectorSecretService.decryptSecrets(connector);

    assertThat(result).containsExactly(Map.entry("API_TOKEN", "sk-abc123"));
  }

  @Test
  void decryptSecretsReturnsEmptyMapWhenNoSecrets() {
    TargetApiConnector connector = TargetApiConnector.builder().id(1L).build();
    when(connectorSecretRepository.findByConnector(connector)).thenReturn(List.of());

    Map<String, String> result = connectorSecretService.decryptSecrets(connector);

    assertThat(result).isEmpty();
  }

  @Test
  void saveSecretsMasksShortValue() {
    TargetApiConnector connector = TargetApiConnector.builder().id(1L).build();
    when(aesGcmEncryptor.encrypt("abc")).thenReturn("encrypted-short");

    connectorSecretService.saveSecrets(connector, Map.of("KEY", "abc"));

    ArgumentCaptor<ConnectorSecret> captor = ArgumentCaptor.forClass(ConnectorSecret.class);
    verify(connectorSecretRepository).save(captor.capture());
    assertThat(captor.getValue().getMaskedValue()).isEqualTo("****");
  }

  @Test
  void realEncryptDecryptRoundTrip() {
    AesGcmEncryptor realEncryptor = new AesGcmEncryptor(TEST_HEX_KEY);
    ConnectorSecretServiceImpl realService =
        new ConnectorSecretServiceImpl(connectorSecretRepository, realEncryptor);

    TargetApiConnector connector = TargetApiConnector.builder().id(1L).build();
    String rawSecret = "sk-proj-abc123-secret-api-key";

    when(connectorSecretRepository.findByConnector(connector))
        .thenAnswer(
            inv -> {
              ArgumentCaptor<ConnectorSecret> captor =
                  ArgumentCaptor.forClass(ConnectorSecret.class);
              verify(connectorSecretRepository).save(captor.capture());
              ConnectorSecret saved = captor.getValue();
              return List.of(saved);
            });

    realService.saveSecrets(connector, Map.of("API_KEY", rawSecret));
    Map<String, String> decrypted = realService.decryptSecrets(connector);
    assertThat(decrypted).containsEntry("API_KEY", rawSecret);
  }
}
