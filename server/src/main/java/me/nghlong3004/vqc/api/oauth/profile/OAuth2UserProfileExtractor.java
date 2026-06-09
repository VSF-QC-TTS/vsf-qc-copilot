package me.nghlong3004.vqc.api.oauth.profile;

import me.nghlong3004.vqc.api.oauth.AuthProvider;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
public interface OAuth2UserProfileExtractor {

  /**
   * Checks whether this extractor supports the given {@link AuthProvider}.
   *
   * @param provider OAuth provider resolved for the current login
   * @return true when this extractor can map the provider profile
   */
  boolean supports(AuthProvider provider);

  /**
   * Extracts the normalized {@link OAuth2UserProfile} from a provider {@link OAuth2User}.
   *
   * @param user provider-specific {@link OAuth2User}
   * @return normalized {@link OAuth2UserProfile}
   */
  OAuth2UserProfile extract(OAuth2User user);
}
