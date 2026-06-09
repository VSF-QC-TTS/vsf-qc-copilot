package me.nghlong3004.vqc.api.oauth.userinfo;

import me.nghlong3004.vqc.api.oauth.AuthProvider;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
public interface OAuth2UserEnricher {

  /**
   * Checks whether this enricher supports the given {@link AuthProvider}.
   *
   * @param provider OAuth provider resolved for the current login
   * @return true when this enricher can load additional user information
   */
  boolean supports(AuthProvider provider);

  /**
   * Enriches a provider {@link OAuth2User} using request-specific OAuth context.
   *
   * @param userRequest current {@link OAuth2UserRequest}
   * @param user provider {@link OAuth2User}
   * @return enriched {@link OAuth2User}
   */
  OAuth2User enrich(OAuth2UserRequest userRequest, OAuth2User user);
}
