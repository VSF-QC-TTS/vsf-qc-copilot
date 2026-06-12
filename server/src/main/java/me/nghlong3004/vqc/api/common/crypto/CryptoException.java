package me.nghlong3004.vqc.api.common.crypto;

/**
 * Thrown when an encryption or decryption operation fails.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
public class CryptoException extends RuntimeException {

  public CryptoException(String message) {
    super(message);
  }

  public CryptoException(String message, Throwable cause) {
    super(message, cause);
  }
}
