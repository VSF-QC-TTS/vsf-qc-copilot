package me.nghlong3004.vqc.api.oauth.userinfo;

import java.util.List;
import me.nghlong3004.vqc.api.oauth.AuthProvider;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Service
public class ProviderAwareOAuth2UserService
    implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;
  private final List<OAuth2UserEnricher> enrichers;

  public ProviderAwareOAuth2UserService(List<OAuth2UserEnricher> enrichers) {
    this(new DefaultOAuth2UserService(), enrichers);
  }

  ProviderAwareOAuth2UserService(
      OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate,
      List<OAuth2UserEnricher> enrichers) {
    this.delegate = delegate;
    this.enrichers = enrichers;
  }

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User user = delegate.loadUser(userRequest);
    AuthProvider provider =
        AuthProvider.fromRegistrationId(userRequest.getClientRegistration().getRegistrationId());

    OAuth2User enrichedUser = user;
    for (OAuth2UserEnricher enricher : enrichers) {
      if (enricher.supports(provider)) {
        enrichedUser = enricher.enrich(userRequest, enrichedUser);
      }
    }
    return enrichedUser;
  }
}
