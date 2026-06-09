package me.nghlong3004.vqc.api.mockchatbot.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import me.nghlong3004.vqc.api.mockchatbot.request.MockChatRequest;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class MockChatbotServiceImplTest {

  private final MockChatbotServiceImpl mockChatbotService = new MockChatbotServiceImpl();

  @Test
  void chatReturnsCorrectStepAnswerByDefault() {
    var response =
        mockChatbotService.chat(
            new MockChatRequest(
                "How many steps did I walk today?", Map.of("steps", 8200), Map.of()));

    assertThat(response.answer()).isEqualTo("Today you walked 8,200 steps.");
  }

  @Test
  void chatUsesMetadataToSimulateNonPassingAnswers() {
    assertThat(
            mockChatbotService
                .chat(
                    new MockChatRequest(
                        "How many steps?", Map.of("steps", 8200), Map.of("expectedStatus", "FAIL")))
                .answer())
        .isEqualTo("Today you walked 0 steps.");
    assertThat(
            mockChatbotService
                .chat(
                    new MockChatRequest(
                        "How many steps?", Map.of("steps", 8200), Map.of("expectedStatus", "WARNING")))
                .answer())
        .contains("need more details");
    assertThat(
            mockChatbotService
                .chat(
                    new MockChatRequest(
                        "How many steps?", Map.of("steps", 8200), Map.of("expectedStatus", "ERROR")))
                .answer())
        .isEmpty();
  }

  @Test
  void chatFallsBackToEchoStyleAnswerWithoutSteps() {
    var response =
        mockChatbotService.chat(new MockChatRequest(" What can you do? ", Map.of(), null));

    assertThat(response.answer()).isEqualTo("Mock answer: What can you do?");
  }
}
