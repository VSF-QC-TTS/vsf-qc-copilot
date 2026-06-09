package me.nghlong3004.vqc.api.user.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
class RoleTest {

  @Test
  void getAuthorityPrefixesRoleNames() {
    assertThat(Role.QC_MEMBER.getAuthority()).isEqualTo("ROLE_QC_MEMBER");
    assertThat(Role.QC_LEAD.getAuthority()).isEqualTo("ROLE_QC_LEAD");
    assertThat(Role.ADMIN.getAuthority()).isEqualTo("ROLE_ADMIN");
  }
}
