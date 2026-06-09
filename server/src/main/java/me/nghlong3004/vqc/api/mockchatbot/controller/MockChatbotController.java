package me.nghlong3004.vqc.api.mockchatbot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.exception.ErrorResponse;
import me.nghlong3004.vqc.api.mockchatbot.request.MockChatRequest;
import me.nghlong3004.vqc.api.mockchatbot.response.MockChatResponse;
import me.nghlong3004.vqc.api.mockchatbot.service.MockChatbotService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Mock Chatbot", description = "Local target API fallback for demos")
public class MockChatbotController {

  private final MockChatbotService mockChatbotService;

  @Operation(summary = "Mock chatbot chat", description = "Returns a deterministic mock answer.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Mock answer",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = MockChatResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Validation failed",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(
      value = "/mock-chatbot/chat",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public MockChatResponse chat(@Valid @RequestBody MockChatRequest request) {
    return mockChatbotService.chat(request);
  }
}
