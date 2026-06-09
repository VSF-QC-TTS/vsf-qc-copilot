package me.nghlong3004.vqc.api.auth.cookie;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class RefreshTokenCookieFactoryTest {

  @Test
  void clearRefreshTokenCookieExpiresCookieWithSameSecurityAttributes() {
    RefreshTokenCookieFactory factory = new RefreshTokenCookieFactory();
    ReflectionTestUtils.setField(factory, "cookieSecure", true);
    ReflectionTestUtils.setField(factory, "sameSite", "Strict");

    var cookie = factory.clearRefreshTokenCookie();

    assertThat(cookie.getName()).isEqualTo("refresh_token");
    assertThat(cookie.getValue()).isEmpty();
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.isSecure()).isTrue();
    assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
    assertThat(cookie.getMaxAge().getSeconds()).isZero();
    assertThat(cookie.getSameSite()).isEqualTo("Strict");
  }
}
