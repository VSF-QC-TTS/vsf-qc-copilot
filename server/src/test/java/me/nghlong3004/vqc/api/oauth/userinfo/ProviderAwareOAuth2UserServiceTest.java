package me.nghlong3004.vqc.api.oauth.userinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.nghlong3004.vqc.api.oauth.AuthProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class ProviderAwareOAuth2UserServiceTest {

  @Test
  void loadUserAppliesMatchingEnrichersInOrder() {
    OAuth2User baseUser = oauthUser(Map.of("id", 1, "login", "nghlong3004"));
    RecordingDelegate delegate = new RecordingDelegate(baseUser);
    RecordingEnricher githubEmail = new RecordingEnricher(AuthProvider.GITHUB, "email", "qc@example.com");
    RecordingEnricher googleName = new RecordingEnricher(AuthProvider.GOOGLE, "name", "Long");
    RecordingEnricher githubAvatar =
        new RecordingEnricher(AuthProvider.GITHUB, "avatar_url", "https://avatars.example.com/u/1");
    ProviderAwareOAuth2UserService service =
        new ProviderAwareOAuth2UserService(delegate, List.of(githubEmail, googleName, githubAvatar));

    OAuth2User loadedUser = service.loadUser(userRequest("github"));

    assertThat(delegate.request.getClientRegistration().getRegistrationId()).isEqualTo("github");
    assertThat(loadedUser.getAttributes().get("email")).isEqualTo("qc@example.com");
    assertThat(loadedUser.getAttributes().get("avatar_url"))
        .isEqualTo("https://avatars.example.com/u/1");
    assertThat(loadedUser.getAttributes().get("name")).isNull();
    assertThat(githubEmail.called).isTrue();
    assertThat(googleName.called).isFalse();
    assertThat(githubAvatar.called).isTrue();
  }

  @Test
  void loadUserRejectsUnsupportedRegistrationId() {
    OAuth2User baseUser = oauthUser(Map.of("id", 1));
    ProviderAwareOAuth2UserService service =
        new ProviderAwareOAuth2UserService(new RecordingDelegate(baseUser), List.of());

    assertThatThrownBy(() -> service.loadUser(userRequest("facebook")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unsupported OAuth2 provider: facebook");
  }

  private OAuth2UserRequest userRequest(String registrationId) {
    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "access-token",
            Instant.parse("2026-06-10T00:00:00Z"),
            Instant.parse("2026-06-10T01:00:00Z"),
            Set.of("user:email"));
    return new OAuth2UserRequest(clientRegistration(registrationId), accessToken);
  }

  private ClientRegistration clientRegistration(String registrationId) {
    return ClientRegistration.withRegistrationId(registrationId)
        .clientId("client-id")
        .clientSecret("client-secret")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("http://localhost/login/oauth2/code/" + registrationId)
        .authorizationUri("https://provider.example.com/oauth/authorize")
        .tokenUri("https://provider.example.com/oauth/token")
        .userInfoUri("https://provider.example.com/user")
        .userNameAttributeName("id")
        .clientName(registrationId)
        .build();
  }

  private OAuth2User oauthUser(Map<String, Object> attributes) {
    return new DefaultOAuth2User(
        List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "id");
  }

  private static class RecordingDelegate implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final OAuth2User user;
    private OAuth2UserRequest request;

    private RecordingDelegate(OAuth2User user) {
      this.user = user;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
      this.request = userRequest;
      return user;
    }
  }

  private static class RecordingEnricher implements OAuth2UserEnricher {
    private final AuthProvider provider;
    private final String attributeName;
    private final Object attributeValue;
    private boolean called;

    private RecordingEnricher(AuthProvider provider, String attributeName, Object attributeValue) {
      this.provider = provider;
      this.attributeName = attributeName;
      this.attributeValue = attributeValue;
    }

    @Override
    public boolean supports(AuthProvider provider) {
      return this.provider == provider;
    }

    @Override
    public OAuth2User enrich(OAuth2UserRequest userRequest, OAuth2User user) {
      called = true;
      Map<String, Object> attributes = new HashMap<>(user.getAttributes());
      attributes.put(attributeName, attributeValue);
      return new DefaultOAuth2User(user.getAuthorities(), attributes, "id");
    }
  }
}
