package me.nghlong3004.vqc.api.mockchatbot.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "MockChatResponse", description = "Mock chatbot response payload")
public record MockChatResponse(
    @Schema(description = "Mock chatbot answer.", example = "Today you walked 8,200 steps.")
        String answer) {}
