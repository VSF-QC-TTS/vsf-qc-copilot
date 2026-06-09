package me.nghlong3004.vqc.api.mockchatbot.service;

import me.nghlong3004.vqc.api.mockchatbot.request.MockChatRequest;
import me.nghlong3004.vqc.api.mockchatbot.response.MockChatResponse;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface MockChatbotService {

  /**
   * Produces a deterministic mock chatbot answer.
   *
   * @param request mock chat request
   * @return mock chat response
   */
  MockChatResponse chat(MockChatRequest request);
}
