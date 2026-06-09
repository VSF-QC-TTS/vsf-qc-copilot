package me.nghlong3004.vqc.api.oauth.profile;

import me.nghlong3004.vqc.api.oauth.AuthProvider;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Component
public class GoogleOAuth2UserProfileExtractor extends AbstractOAuth2UserProfileExtractor {

  private static final AuthProvider PROVIDER = AuthProvider.GOOGLE;

  @Override
  public boolean supports(AuthProvider provider) {
    return PROVIDER == provider;
  }

  @Override
  public OAuth2UserProfile extract(OAuth2User user) {
    return new OAuth2UserProfile(
        PROVIDER,
        stringAttribute(user, "email"),
        stringAttribute(user, "given_name"),
        stringAttribute(user, "family_name"),
        stringAttribute(user, "picture"));
  }
}
