package me.nghlong3004.vqc.api.mockchatbot.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import me.nghlong3004.vqc.api.exception.GlobalException;
import me.nghlong3004.vqc.api.mockchatbot.request.MockChatRequest;
import me.nghlong3004.vqc.api.mockchatbot.response.MockChatResponse;
import me.nghlong3004.vqc.api.mockchatbot.service.MockChatbotService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@WebMvcTest(
    controllers = MockChatbotController.class,
    excludeAutoConfiguration = {
      OAuth2ClientAutoConfiguration.class,
      OAuth2ClientWebSecurityAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalException.class, MockChatbotControllerTest.MockBeans.class})
class MockChatbotControllerTest {

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void resetTestDoubles() {
    RecordingMockChatbotService.reset();
  }

  @Test
  void chatReturnsMockAnswer() throws Exception {
    RecordingMockChatbotService.response = new MockChatResponse("Today you walked 8,200 steps.");

    mockMvc
        .perform(
            post("/mock-chatbot/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "message": "How many steps did I walk today?",
                      "context": {
                        "steps": 8200
                      },
                      "metadata": {
                        "expectedStatus": "PASS"
                      }
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.answer").value("Today you walked 8,200 steps."));

    assertThat(RecordingMockChatbotService.request.message())
        .isEqualTo("How many steps did I walk today?");
    assertThat(RecordingMockChatbotService.request.context()).containsEntry("steps", 8200);
    assertThat(RecordingMockChatbotService.request.metadata()).containsEntry("expectedStatus", "PASS");
  }

  @Test
  void chatReturnsValidationProblemDetails() throws Exception {
    mockMvc
        .perform(
            post("/mock-chatbot/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "message": " "
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.instance").value("/mock-chatbot/chat"))
        .andExpect(jsonPath("$.errors[*].field", hasItem("message")));

    assertThat(RecordingMockChatbotService.request).isNull();
  }

  @TestConfiguration
  static class MockBeans {

    @Bean
    RecordingMockChatbotService mockChatbotService() {
      return new RecordingMockChatbotService();
    }
  }

  static class RecordingMockChatbotService implements MockChatbotService {
    private static MockChatRequest request;
    private static MockChatResponse response;

    @Override
    public MockChatResponse chat(MockChatRequest request) {
      RecordingMockChatbotService.request = request;
      return response;
    }

    private static void reset() {
      request = null;
      response = null;
    }
  }
}
