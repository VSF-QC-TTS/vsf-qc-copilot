package me.nghlong3004.vqc.api.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class AuthProviderTest {

  @Test
  void supportsRegistrationIdIgnoresCaseAndExcludesLocal() {
    assertThat(AuthProvider.GOOGLE.supportsRegistrationId("GoOgLe")).isTrue();
    assertThat(AuthProvider.GITHUB.supportsRegistrationId("github")).isTrue();
    assertThat(AuthProvider.LOCAL.supportsRegistrationId("local")).isFalse();
  }

  @Test
  void fromRegistrationIdResolvesSupportedProviders() {
    assertThat(AuthProvider.fromRegistrationId("google")).isEqualTo(AuthProvider.GOOGLE);
    assertThat(AuthProvider.fromRegistrationId("GITHUB")).isEqualTo(AuthProvider.GITHUB);
  }

  @Test
  void fromRegistrationIdRejectsUnsupportedProvider() {
    assertThatThrownBy(() -> AuthProvider.fromRegistrationId("facebook"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unsupported OAuth2 provider: facebook");
  }
}
