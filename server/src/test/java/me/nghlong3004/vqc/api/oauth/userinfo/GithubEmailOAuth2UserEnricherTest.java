package me.nghlong3004.vqc.api.oauth.userinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.nghlong3004.vqc.api.oauth.AuthProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class GithubEmailOAuth2UserEnricherTest {

  @Test
  void supportsOnlyGithubProvider() {
    GithubEmailOAuth2UserEnricher enricher = new GithubEmailOAuth2UserEnricher();

    assertThat(enricher.supports(AuthProvider.GITHUB)).isTrue();
    assertThat(enricher.supports(AuthProvider.GOOGLE)).isFalse();
  }

  @Test
  void enrichReturnsOriginalUserWhenEmailAlreadyExists() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    GithubEmailOAuth2UserEnricher enricher = new GithubEmailOAuth2UserEnricher(restTemplate);
    OAuth2User user = oauthUser(Map.of("id", 1, "email", "public@example.com"));

    OAuth2User enrichedUser = enricher.enrich(userRequest(), user);

    assertThat(enrichedUser).isSameAs(user);
    server.verify();
  }

  @Test
  void enrichAddsPrimaryVerifiedEmailFromGithubApi() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    server
        .expect(once(), requestTo("https://api.github.com/user/emails"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
        .andExpect(header(HttpHeaders.ACCEPT, "application/vnd.github+json"))
        .andRespond(
            withSuccess(
                """
                [
                  {"email": "secondary@example.com", "verified": true, "primary": false},
                  {"email": "primary@example.com", "verified": true, "primary": true}
                ]
                """,
                MediaType.APPLICATION_JSON));
    GithubEmailOAuth2UserEnricher enricher = new GithubEmailOAuth2UserEnricher(restTemplate);

    OAuth2User enrichedUser = enricher.enrich(userRequest(), oauthUser(Map.of("id", 1)));

    assertThat(enrichedUser.getAttributes().get("email")).isEqualTo("primary@example.com");
    server.verify();
  }

  @Test
  void enrichReturnsOriginalUserWhenGithubApiFails() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    server
        .expect(once(), requestTo("https://api.github.com/user/emails"))
        .andRespond(withServerError());
    GithubEmailOAuth2UserEnricher enricher = new GithubEmailOAuth2UserEnricher(restTemplate);
    OAuth2User user = oauthUser(Map.of("id", 1));

    OAuth2User enrichedUser = enricher.enrich(userRequest(), user);

    assertThat(enrichedUser).isSameAs(user);
    server.verify();
  }

  private OAuth2UserRequest userRequest() {
    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "access-token",
            Instant.parse("2026-06-10T00:00:00Z"),
            Instant.parse("2026-06-10T01:00:00Z"),
            Set.of("user:email"));
    return new OAuth2UserRequest(clientRegistration(), accessToken);
  }

  private ClientRegistration clientRegistration() {
    return ClientRegistration.withRegistrationId("github")
        .clientId("client-id")
        .clientSecret("client-secret")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("http://localhost/login/oauth2/code/github")
        .authorizationUri("https://github.com/login/oauth/authorize")
        .tokenUri("https://github.com/login/oauth/access_token")
        .userInfoUri("https://api.github.com/user")
        .userNameAttributeName("id")
        .clientName("GitHub")
        .build();
  }

  private OAuth2User oauthUser(Map<String, Object> attributes) {
    return new DefaultOAuth2User(
        List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "id");
  }
}
