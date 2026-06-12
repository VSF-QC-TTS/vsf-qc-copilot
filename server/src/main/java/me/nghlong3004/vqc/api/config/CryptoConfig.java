package me.nghlong3004.vqc.api.config;

import me.nghlong3004.vqc.api.common.crypto.AesGcmEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the AES-256-GCM encryptor bean using the {@code VQC_SECRET_ENCRYPTION_KEY} env var.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
@Configuration
public class CryptoConfig {

  @Bean
  public AesGcmEncryptor aesGcmEncryptor(
      @Value("${vqc.security.secret-encryption-key}") String hexKey) {
    return new AesGcmEncryptor(hexKey);
  }
}
