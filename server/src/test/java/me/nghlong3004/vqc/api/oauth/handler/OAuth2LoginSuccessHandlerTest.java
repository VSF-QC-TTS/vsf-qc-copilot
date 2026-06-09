package me.nghlong3004.vqc.api.oauth.handler;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import me.nghlong3004.vqc.api.oauth.AuthProvider;
import me.nghlong3004.vqc.api.oauth.profile.OAuth2UserProfile;
import me.nghlong3004.vqc.api.oauth.profile.OAuth2UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class OAuth2LoginSuccessHandlerTest {

  @Test
  void onAuthenticationSuccessRedirectsWithErrorWhenProviderHasNoEmail() throws Exception {
    OAuth2LoginSuccessHandler handler =
        handler(new OAuth2UserProfile(AuthProvider.GITHUB, null, "Long", "Nguyen", null));
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.onAuthenticationSuccess(
        new MockHttpServletRequest(), response, new TestingAuthenticationToken("user", null));

    assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/login?error=oauth_no_email");
    assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
  }

  @Test
  void onAuthenticationSuccessAddsRefreshCookieInvalidatesSessionAndRedirectsToDashboard()
      throws Exception {
    OAuth2LoginSuccessHandler handler =
        handler(
            new OAuth2UserProfile(
                AuthProvider.GOOGLE,
                "qc.demo@example.com",
                "Long",
                "Nguyen",
                "https://cdn.example.com/avatar.png"));
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpSession session = new MockHttpSession();
    request.setSession(session);
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.onAuthenticationSuccess(request, response, new TestingAuthenticationToken("user", null));

    assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/dashboard");
    assertThat(session.isInvalid()).isTrue();
    assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
        .contains("refresh_token=")
        .contains("HttpOnly")
        .contains("Secure")
        .contains("Path=/api/v1/auth/refresh-token")
        .contains("Max-Age=1800")
        .contains("SameSite=Strict");
  }

  private OAuth2LoginSuccessHandler handler(OAuth2UserProfile profile) {
    OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler(new StubProfileService(profile));
    ReflectionTestUtils.setField(handler, "webBaseUrl", "http://localhost:5173");
    ReflectionTestUtils.setField(handler, "refreshExpirationMinutes", 30);
    ReflectionTestUtils.setField(handler, "cookieSecure", true);
    ReflectionTestUtils.setField(handler, "sameSite", "Strict");
    return handler;
  }

  private static class StubProfileService extends OAuth2UserProfileService {
    private final OAuth2UserProfile profile;

    private StubProfileService(OAuth2UserProfile profile) {
      super(List.of());
      this.profile = profile;
    }

    @Override
    public OAuth2UserProfile extract(Authentication authentication) {
      return profile;
    }
  }
}
