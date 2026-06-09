package me.nghlong3004.vqc.api.user.enums;

import org.springframework.security.core.GrantedAuthority;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public enum Role implements GrantedAuthority {
  QC_MEMBER,
  QC_LEAD,
  ADMIN;

  @Override
  public String getAuthority() {
    return "ROLE_" + name();
  }
}
