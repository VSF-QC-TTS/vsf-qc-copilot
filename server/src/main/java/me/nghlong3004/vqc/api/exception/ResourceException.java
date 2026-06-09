package me.nghlong3004.vqc.api.exception;

import java.util.List;
import lombok.Getter;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Getter
public class ResourceException extends RuntimeException {

  private final ErrorResponse response;

  public ResourceException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.response = errorCode.toErrorResponse();
  }

  public ResourceException(ErrorCode errorCode, List<String> details) {
    super(errorCode.getMessage());
    this.response =
        new ErrorResponse(
            errorCode.getMessage(), errorCode.getStatus(), errorCode.getCode(), details);
  }

  public ResourceException(String message, int status, String code) {
    super(message);
    this.response = new ErrorResponse(message, status, code);
  }

  public ResourceException(ErrorCode errorCode, long retryAfterSeconds) {
    super(errorCode.getMessage());
    this.response = errorCode.toErrorResponse().withRetryAfterSeconds(retryAfterSeconds);
  }
}
