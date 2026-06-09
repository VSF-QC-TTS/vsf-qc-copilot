package me.nghlong3004.vqc.api.oauth.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import me.nghlong3004.vqc.api.oauth.AuthProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class OAuth2UserProfileServiceTest {

  @Test
  void extractUsesExtractorMatchingRegistrationId() {
    OAuth2UserProfile expected =
        new OAuth2UserProfile(AuthProvider.GITHUB, "qc.demo@example.com", "Long", "Nguyen", null);
    OAuth2UserProfileService service =
        new OAuth2UserProfileService(
            List.of(
                new StubExtractor(AuthProvider.GOOGLE, null),
                new StubExtractor(AuthProvider.GITHUB, expected)));

    OAuth2UserProfile profile = service.extract(oauthAuthentication("github"));

    assertThat(profile).isSameAs(expected);
  }

  @Test
  void extractRejectsNonOauthAuthentication() {
    OAuth2UserProfileService service = new OAuth2UserProfileService(List.of());

    assertThatThrownBy(
            () -> service.extract(new TestingAuthenticationToken("qc.demo@example.com", "password")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unsupported OAuth2 authentication principal");
  }

  @Test
  void extractRejectsRegistrationIdWithoutExtractor() {
    OAuth2UserProfileService service = new OAuth2UserProfileService(List.of());

    assertThatThrownBy(() -> service.extract(oauthAuthentication("google")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unsupported OAuth2 provider: GOOGLE");
  }

  private OAuth2AuthenticationToken oauthAuthentication(String registrationId) {
    OAuth2User user =
        new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("sub", "oauth-id"), "sub");
    return new OAuth2AuthenticationToken(user, user.getAuthorities(), registrationId);
  }

  private record StubExtractor(AuthProvider supportedProvider, OAuth2UserProfile profile)
      implements OAuth2UserProfileExtractor {

    @Override
    public boolean supports(AuthProvider provider) {
      return supportedProvider == provider;
    }

    @Override
    public OAuth2UserProfile extract(OAuth2User user) {
      return profile;
    }
  }
}
