package me.nghlong3004.vqc.api.oauth.userinfo;

import me.nghlong3004.vqc.api.oauth.AuthProvider;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
public interface OAuth2UserEnricher {

  boolean supports(AuthProvider provider);

  OAuth2User enrich(OAuth2UserRequest userRequest, OAuth2User user);
}
