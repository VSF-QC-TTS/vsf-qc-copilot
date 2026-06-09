package me.nghlong3004.vqc.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    String message, int status, String code, List<String> details, Long retryAfterSeconds) {
  public ErrorResponse(String message, int status, String code) {
    this(message, status, code, null, null);
  }

  public ErrorResponse(String message, int status, String code, List<String> details) {
    this(message, status, code, details, null);
  }
}
