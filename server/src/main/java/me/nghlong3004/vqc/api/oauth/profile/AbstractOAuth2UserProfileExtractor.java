package me.nghlong3004.vqc.api.oauth.profile;

import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
abstract class AbstractOAuth2UserProfileExtractor implements OAuth2UserProfileExtractor {

  protected String stringAttribute(OAuth2User user, String name) {
    Object value = user.getAttribute(name);
    if (value == null) {
      return null;
    }
    String text = value.toString().trim();
    return text.isBlank() ? null : text;
  }

  protected NameParts splitName(String fullName) {
    if (fullName == null || fullName.isBlank()) {
      return new NameParts("", "");
    }
    String[] parts = fullName.trim().split("\\s+", 2);
    return new NameParts(parts[0], parts.length > 1 ? parts[1] : "");
  }

  protected record NameParts(String firstName, String lastName) {}
}
