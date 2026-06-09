package me.nghlong3004.vqc.api.oauth.profile;

import java.util.List;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.oauth.AuthProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Service
@RequiredArgsConstructor
public class OAuth2UserProfileService {

  private final List<OAuth2UserProfileExtractor> extractors;

  public OAuth2UserProfile extract(Authentication authentication) {
    if (!(authentication instanceof OAuth2AuthenticationToken token)
        || !(authentication.getPrincipal() instanceof OAuth2User user)) {
      throw new IllegalArgumentException("Unsupported OAuth2 authentication principal");
    }

    AuthProvider provider =
        AuthProvider.fromRegistrationId(token.getAuthorizedClientRegistrationId());
    return extractors.stream()
        .filter(extractor -> extractor.supports(provider))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported OAuth2 provider: " + provider))
        .extract(user);
  }
}
