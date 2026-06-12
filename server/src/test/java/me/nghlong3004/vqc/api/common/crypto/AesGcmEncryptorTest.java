package me.nghlong3004.vqc.api.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
class AesGcmEncryptorTest {

  // 256-bit test key (64 hex chars)
  private static final String TEST_HEX_KEY =
      "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

  private final AesGcmEncryptor encryptor = new AesGcmEncryptor(TEST_HEX_KEY);

  @Test
  void encryptAndDecryptRoundTrip() {
    String plaintext = "sk-proj-abc123-secret-api-key";
    String encrypted = encryptor.encrypt(plaintext);
    assertThat(encrypted).isNotEqualTo(plaintext);
    assertThat(encryptor.decrypt(encrypted)).isEqualTo(plaintext);
  }

  @Test
  void encryptProducesDifferentCiphertextEachTime() {
    String plaintext = "same-secret";
    String first = encryptor.encrypt(plaintext);
    String second = encryptor.encrypt(plaintext);
    assertThat(first).isNotEqualTo(second);
    // Both should decrypt to the same value
    assertThat(encryptor.decrypt(first)).isEqualTo(plaintext);
    assertThat(encryptor.decrypt(second)).isEqualTo(plaintext);
  }

  @Test
  void decryptWithWrongKeyFails() {
    String encrypted = encryptor.encrypt("secret");
    String otherKey = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
    AesGcmEncryptor otherEncryptor = new AesGcmEncryptor(otherKey);
    assertThatThrownBy(() -> otherEncryptor.decrypt(encrypted))
        .isInstanceOf(CryptoException.class);
  }

  @Test
  void decryptTamperedCiphertextFails() {
    String encrypted = encryptor.encrypt("secret");
    // Tamper with the ciphertext
    char[] chars = encrypted.toCharArray();
    chars[20] = (chars[20] == 'A') ? 'B' : 'A';
    String tampered = new String(chars);
    assertThatThrownBy(() -> encryptor.decrypt(tampered))
        .isInstanceOf(Exception.class);
  }

  @Test
  void handlesEmptyString() {
    String encrypted = encryptor.encrypt("");
    assertThat(encryptor.decrypt(encrypted)).isEmpty();
  }

  @Test
  void handlesUnicodeContent() {
    String plaintext = "mật_khẩu_bí_mật_🔐";
    String encrypted = encryptor.encrypt(plaintext);
    assertThat(encryptor.decrypt(encrypted)).isEqualTo(plaintext);
  }

  @Test
  void rejectsBlankKey() {
    assertThatThrownBy(() -> new AesGcmEncryptor(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must not be blank");
  }

  @Test
  void rejectsShortKey() {
    assertThatThrownBy(() -> new AesGcmEncryptor("0123456789abcdef"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("64-char hex string");
  }

  @Test
  void rejectsNullKey() {
    assertThatThrownBy(() -> new AesGcmEncryptor(null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
