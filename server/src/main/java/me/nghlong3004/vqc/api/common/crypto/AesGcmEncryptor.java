package me.nghlong3004.vqc.api.common.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

/**
 * AES-256-GCM encryption utility for connector secrets.
 *
 * <p>Each encrypt call generates a random 12-byte IV (nonce), prepended to the ciphertext and
 * returned as a single Base64 string: {@code base64(IV || ciphertext || tag)}.
 *
 * <p>The key is derived from a hex-encoded 256-bit {@code VQC_SECRET_ENCRYPTION_KEY} environment
 * variable.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
@Slf4j
public class AesGcmEncryptor {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_IV_BYTES = 12;
  private static final int GCM_TAG_BITS = 128;

  private final SecretKey secretKey;
  private final SecureRandom random = new SecureRandom();

  public AesGcmEncryptor(String hexKey) {
    if (hexKey == null || hexKey.isBlank()) {
      throw new IllegalArgumentException(
          "VQC_SECRET_ENCRYPTION_KEY must not be blank. Provide a 64-char hex-encoded 256-bit key.");
    }
    byte[] keyBytes = hexToBytes(hexKey.strip());
    if (keyBytes.length != 32) {
      throw new IllegalArgumentException(
          "VQC_SECRET_ENCRYPTION_KEY must be a 64-char hex string (256-bit key). Got "
              + (hexKey.strip().length())
              + " chars.");
    }
    this.secretKey = new SecretKeySpec(keyBytes, "AES");
  }

  /**
   * Encrypts plaintext using AES-256-GCM with a random IV.
   *
   * @param plaintext the string to encrypt
   * @return Base64-encoded {@code IV || ciphertext || tag}
   */
  public String encrypt(String plaintext) {
    try {
      byte[] iv = new byte[GCM_IV_BYTES];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      byte[] combined = ByteBuffer.allocate(iv.length + ciphertext.length)
          .put(iv)
          .put(ciphertext)
          .array();
      return Base64.getEncoder().encodeToString(combined);
    } catch (GeneralSecurityException ex) {
      throw new CryptoException("AES-GCM encryption failed", ex);
    }
  }

  /**
   * Decrypts a Base64-encoded AES-256-GCM ciphertext produced by {@link #encrypt(String)}.
   *
   * @param cipherBase64 Base64-encoded {@code IV || ciphertext || tag}
   * @return the original plaintext
   */
  public String decrypt(String cipherBase64) {
    try {
      byte[] combined = Base64.getDecoder().decode(cipherBase64);
      if (combined.length < GCM_IV_BYTES + 1) {
        throw new CryptoException("Ciphertext too short to contain IV + data");
      }
      ByteBuffer buffer = ByteBuffer.wrap(combined);
      byte[] iv = new byte[GCM_IV_BYTES];
      buffer.get(iv);
      byte[] ciphertext = new byte[buffer.remaining()];
      buffer.get(ciphertext);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException ex) {
      throw new CryptoException("AES-GCM decryption failed", ex);
    }
  }

  private static byte[] hexToBytes(String hex) {
    int len = hex.length();
    byte[] out = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      out[i / 2] =
          (byte) ((Character.digit(hex.charAt(i), 16) << 4)
              + Character.digit(hex.charAt(i + 1), 16));
    }
    return out;
  }
}
