package me.nghlong3004.vqc.api.mockchatbot.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "MockChatRequest", description = "Mock chatbot request payload")
public record MockChatRequest(
    @Schema(description = "User message.", example = "How many steps did I walk today?")
        @NotBlank(message = "Message is required.")
        String message,
    @Schema(description = "Optional target context.", nullable = true) Map<String, Object> context,
    @Schema(description = "Optional metadata controlling mock behavior.", nullable = true)
        Map<String, Object> metadata) {}
