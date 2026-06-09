package me.nghlong3004.vqc.api.mockchatbot.service.impl;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import me.nghlong3004.vqc.api.mockchatbot.request.MockChatRequest;
import me.nghlong3004.vqc.api.mockchatbot.response.MockChatResponse;
import me.nghlong3004.vqc.api.mockchatbot.service.MockChatbotService;
import org.springframework.stereotype.Service;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Service
public class MockChatbotServiceImpl implements MockChatbotService {

  @Override
  public MockChatResponse chat(MockChatRequest request) {
    String expectedStatus = metadataValue(request.metadata(), "expectedStatus");
    return switch (expectedStatus.toUpperCase(Locale.ROOT)) {
      case "FAIL" -> new MockChatResponse("Today you walked 0 steps.");
      case "WARNING" -> new MockChatResponse("You walked some steps today, but I need more details.");
      case "ERROR" -> new MockChatResponse("");
      default -> new MockChatResponse(passAnswer(request));
    };
  }

  private String passAnswer(MockChatRequest request) {
    Object steps = request.context() == null ? null : request.context().get("steps");
    if (steps instanceof Number number) {
      String formattedSteps = NumberFormat.getIntegerInstance(Locale.US).format(number.longValue());
      return "Today you walked " + formattedSteps + " steps.";
    }
    return "Mock answer: " + request.message().trim();
  }

  private String metadataValue(Map<String, Object> metadata, String key) {
    if (metadata == null || metadata.get(key) == null) {
      return "PASS";
    }
    return metadata.get(key).toString();
  }
}
