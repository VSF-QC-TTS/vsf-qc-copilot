package me.nghlong3004.vqc.api.oauth;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 5/24/2026
 */
@RequiredArgsConstructor
public enum AuthProvider {
  LOCAL(null),
  GOOGLE("google"),
  GITHUB("github");

  private final String registrationId;

  public boolean supportsRegistrationId(String registrationId) {
    return this.registrationId != null && this.registrationId.equalsIgnoreCase(registrationId);
  }

  public static AuthProvider fromRegistrationId(String registrationId) {
    return Arrays.stream(values())
        .filter(provider -> provider.supportsRegistrationId(registrationId))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Unsupported OAuth2 provider: " + registrationId));
  }
}
