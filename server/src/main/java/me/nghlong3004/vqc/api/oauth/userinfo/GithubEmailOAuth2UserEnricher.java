package me.nghlong3004.vqc.api.oauth.userinfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.oauth.AuthProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Slf4j
@Component
public class GithubEmailOAuth2UserEnricher implements OAuth2UserEnricher {

  private static final String GITHUB_EMAILS_URL = "https://api.github.com/user/emails";
  private static final String GITHUB_USER_NAME_ATTRIBUTE = "id";

  private final RestTemplate restTemplate;

  public GithubEmailOAuth2UserEnricher() {
    this(new RestTemplate());
  }

  GithubEmailOAuth2UserEnricher(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public boolean supports(AuthProvider provider) {
    return provider == AuthProvider.GITHUB;
  }

  @Override
  public OAuth2User enrich(OAuth2UserRequest userRequest, OAuth2User user) {
    if (user.getAttribute("email") != null) {
      return user;
    }

    String email = fetchPrimaryVerifiedEmail(userRequest);
    if (email == null) {
      return user;
    }

    Map<String, Object> attributes = new HashMap<>(user.getAttributes());
    attributes.put("email", email);
    return new DefaultOAuth2User(user.getAuthorities(), attributes, GITHUB_USER_NAME_ATTRIBUTE);
  }

  private String fetchPrimaryVerifiedEmail(OAuth2UserRequest userRequest) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(userRequest.getAccessToken().getTokenValue());
    headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");

    try {
      ResponseEntity<List<Map<String, Object>>> response =
          restTemplate.exchange(
              GITHUB_EMAILS_URL,
              HttpMethod.GET,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      List<Map<String, Object>> emails = response.getBody();
      if (emails == null || emails.isEmpty()) {
        return null;
      }

      return emails.stream()
          .filter(this::isVerified)
          .filter(email -> Boolean.TRUE.equals(email.get("primary")))
          .findFirst()
          .or(() -> emails.stream().filter(this::isVerified).findFirst())
          .map(email -> (String) email.get("email"))
          .orElse(null);
    } catch (RestClientException exc) {
      log.warn("Failed to fetch GitHub primary email: {}", exc.getMessage());
      return null;
    }
  }

  private boolean isVerified(Map<String, Object> email) {
    return Boolean.TRUE.equals(email.get("verified")) && email.get("email") instanceof String;
  }
}
