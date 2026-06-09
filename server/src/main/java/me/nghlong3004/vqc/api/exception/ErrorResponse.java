package me.nghlong3004.vqc.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Locale;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ErrorResponse", description = "Problem Details API error response")
public record ErrorResponse(
    @Schema(
            description = "Problem type URI.",
            example = "https://vqc.nghlong3004.me/errors/validation-error")
        String type,
    @Schema(description = "Short human-readable problem title.", example = "Validation Error")
        String title,
    @Schema(description = "HTTP status code.", example = "400") int status,
    @Schema(description = "Human-readable problem detail.", example = "Validation failed for input data.")
        String detail,
    @Schema(description = "Request path where the problem occurred.", example = "/api/v1/auth/register")
        String instance,
    @Schema(description = "Stable application error code extension.", example = "VALIDATION_ERROR")
        String code,
    @Schema(description = "Optional validation field errors.", nullable = true)
        List<ErrorItem> errors,
    @Schema(description = "Optional retry-after value in seconds.", example = "60", nullable = true)
        Long retryAfterSeconds) {

  private static final String TYPE_BASE_URL = "https://vqc.nghlong3004.me/errors/";

  public ErrorResponse(String detail, int status, String code) {
    this(typeFromCode(code), titleFromCode(code), status, detail, null, code, null, null);
  }

  public ErrorResponse(String detail, int status, String code, List<String> details) {
    this(typeFromCode(code), titleFromCode(code), status, detail, null, code, toErrors(details), null);
  }

  public ErrorResponse withInstance(String instance) {
    return new ErrorResponse(type, title, status, detail, instance, code, errors, retryAfterSeconds);
  }

  public ErrorResponse withErrors(List<ErrorItem> errors) {
    return new ErrorResponse(type, title, status, detail, instance, code, errors, retryAfterSeconds);
  }

  public ErrorResponse withRetryAfterSeconds(long retryAfterSeconds) {
    return new ErrorResponse(type, title, status, detail, instance, code, errors, retryAfterSeconds);
  }

  public static ErrorItem error(String field, String message) {
    return new ErrorItem(field, message);
  }

  private static List<ErrorItem> toErrors(List<String> details) {
    if (details == null || details.isEmpty()) {
      return null;
    }
    return details.stream().map(detail -> new ErrorItem(null, detail)).toList();
  }

  private static String typeFromCode(String code) {
    return TYPE_BASE_URL + code.toLowerCase(Locale.ROOT).replace('_', '-');
  }

  private static String titleFromCode(String code) {
    String[] parts = code.toLowerCase(Locale.ROOT).split("_");
    StringBuilder title = new StringBuilder();
    for (String part : parts) {
      if (part.isBlank()) {
        continue;
      }
      if (!title.isEmpty()) {
        title.append(' ');
      }
      title.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
    }
    return title.toString();
  }

  @Schema(name = "ErrorItem", description = "Validation field error")
  public record ErrorItem(
      @Schema(description = "Invalid field or parameter name.", example = "email") String field,
      @Schema(description = "Validation error message.", example = "Email must be a valid email address.")
          String message) {
  }
}
