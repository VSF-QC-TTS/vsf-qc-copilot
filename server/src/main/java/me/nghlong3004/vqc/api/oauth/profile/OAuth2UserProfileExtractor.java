package me.nghlong3004.vqc.api.oauth.profile;

import me.nghlong3004.vqc.api.oauth.AuthProvider;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
public interface OAuth2UserProfileExtractor {

  boolean supports(AuthProvider provider);

  OAuth2UserProfile extract(OAuth2User user);
}
