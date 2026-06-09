package me.nghlong3004.vqc.api.oauth.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import me.nghlong3004.vqc.api.oauth.AuthProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class OAuth2UserProfileExtractorTest {

  @Test
  void googleExtractorNormalizesStringAttributes() {
    GoogleOAuth2UserProfileExtractor extractor = new GoogleOAuth2UserProfileExtractor();

    OAuth2UserProfile profile =
        extractor.extract(
            oauthUser(
                "sub",
                Map.of(
                    "sub", "google-id",
                    "email", " qc.demo@example.com ",
                    "given_name", " Long ",
                    "family_name", " ",
                    "picture", "https://cdn.example.com/avatar.png")));

    assertThat(extractor.supports(AuthProvider.GOOGLE)).isTrue();
    assertThat(extractor.supports(AuthProvider.GITHUB)).isFalse();
    assertThat(profile.provider()).isEqualTo(AuthProvider.GOOGLE);
    assertThat(profile.email()).isEqualTo("qc.demo@example.com");
    assertThat(profile.firstName()).isEqualTo("Long");
    assertThat(profile.lastName()).isNull();
    assertThat(profile.avatarUrl()).isEqualTo("https://cdn.example.com/avatar.png");
  }

  @Test
  void githubExtractorSplitsDisplayNameAndUsesLoginFallback() {
    GithubOAuth2UserProfileExtractor extractor = new GithubOAuth2UserProfileExtractor();

    OAuth2UserProfile namedProfile =
        extractor.extract(
            oauthUser(
                "id",
                Map.of(
                    "id", 1,
                    "email", "qc.demo@example.com",
                    "name", "Long Nguyen Hoang",
                    "login", "nghlong3004",
                    "avatar_url", "https://avatars.example.com/u/1")));
    OAuth2UserProfile fallbackProfile =
        extractor.extract(oauthUser("id", Map.of("id", 2, "login", "nghlong3004", "name", " ")));

    assertThat(extractor.supports(AuthProvider.GITHUB)).isTrue();
    assertThat(extractor.supports(AuthProvider.GOOGLE)).isFalse();
    assertThat(namedProfile.provider()).isEqualTo(AuthProvider.GITHUB);
    assertThat(namedProfile.firstName()).isEqualTo("Long");
    assertThat(namedProfile.lastName()).isEqualTo("Nguyen Hoang");
    assertThat(namedProfile.avatarUrl()).isEqualTo("https://avatars.example.com/u/1");
    assertThat(fallbackProfile.firstName()).isEqualTo("nghlong3004");
    assertThat(fallbackProfile.lastName()).isEmpty();
  }

  private OAuth2User oauthUser(String nameAttributeKey, Map<String, Object> attributes) {
    return new DefaultOAuth2User(
        List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, nameAttributeKey);
  }
}
