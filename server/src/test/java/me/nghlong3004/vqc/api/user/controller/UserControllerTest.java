package me.nghlong3004.vqc.api.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.exception.GlobalException;
import me.nghlong3004.vqc.api.user.enums.Role;
import me.nghlong3004.vqc.api.user.enums.UserStatus;
import me.nghlong3004.vqc.api.user.response.UserResponse;
import me.nghlong3004.vqc.api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@WebMvcTest(
    controllers = UserController.class,
    excludeAutoConfiguration = {
      OAuth2ClientAutoConfiguration.class,
      OAuth2ClientWebSecurityAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalException.class, UserControllerTest.MockBeans.class})
class UserControllerTest {

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void resetTestDoubles() {
    RecordingUserService.reset();
  }

  @Test
  void currentUserReturnsAuthenticatedUser() throws Exception {
    RecordingUserService.currentUserResponse =
        new UserResponse(
            UUID.fromString("7b7b7d42-5f42-4c5a-9281-8d1d36f6f59d"),
            "qc.demo@example.com",
            "QC Demo",
            Role.QC_MEMBER,
            UserStatus.ACTIVE,
            OffsetDateTime.parse("2026-06-09T10:00:00Z"));

    mockMvc
        .perform(
            get("/api/v1/users/me")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value("7b7b7d42-5f42-4c5a-9281-8d1d36f6f59d"))
        .andExpect(jsonPath("$.email").value("qc.demo@example.com"))
        .andExpect(jsonPath("$.displayName").value("QC Demo"))
        .andExpect(jsonPath("$.role").value("QC_MEMBER"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    assertThat(RecordingUserService.username).isEqualTo("qc.demo@example.com");
  }

  @TestConfiguration
  static class MockBeans {

    @Bean
    RecordingUserService userService() {
      return new RecordingUserService();
    }
  }

  static class RecordingUserService implements UserService {
    private static String username;
    private static UserResponse currentUserResponse;

    @Override
    public UserResponse register(me.nghlong3004.vqc.api.auth.request.RegisterRequest request) {
      throw new AssertionError("Register should not be called");
    }

    @Override
    public UserResponse getCurrentUser(String username) {
      RecordingUserService.username = username;
      return currentUserResponse;
    }

    private static void reset() {
      username = null;
      currentUserResponse = null;
    }
  }
}
