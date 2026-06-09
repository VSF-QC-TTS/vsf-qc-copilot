package me.nghlong3004.vqc.api.auth.token;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public interface OpaqueTokenService {

  /**
   * Generates a URL-safe random token for one-time email flows.
   *
   * @return raw token that may be sent to a user
   */
  String generateRawToken();

  /**
   * Hashes a raw token before persistence.
   *
   * @param rawToken token value received from or sent to the user
   * @return deterministic SHA-256 hex hash
   */
  String hash(String rawToken);
}
