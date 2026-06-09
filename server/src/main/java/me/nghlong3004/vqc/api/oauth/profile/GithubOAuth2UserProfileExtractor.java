package me.nghlong3004.vqc.api.oauth.profile;

import me.nghlong3004.vqc.api.oauth.AuthProvider;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Component
public class GithubOAuth2UserProfileExtractor extends AbstractOAuth2UserProfileExtractor {

  private static final AuthProvider PROVIDER = AuthProvider.GITHUB;

  @Override
  public boolean supports(AuthProvider provider) {
    return PROVIDER == provider;
  }

  @Override
  public OAuth2UserProfile extract(OAuth2User user) {
    NameParts name = splitName(stringAttribute(user, "name"));
    String login = stringAttribute(user, "login");
    return new OAuth2UserProfile(
        PROVIDER,
        stringAttribute(user, "email"),
        name.firstName().isBlank() ? login : name.firstName(),
        name.lastName(),
        stringAttribute(user, "avatar_url"));
  }
}
