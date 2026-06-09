package me.nghlong3004.vqc.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
class ErrorResponseTest {

  @Test
  void toErrorResponseBuildsProblemDetailsShape() {
    ErrorResponse response =
        ErrorCode.VALIDATION_ERROR
            .toErrorResponse()
            .withInstance("/api/v1/auth/register")
            .withErrors(List.of(ErrorResponse.error("email", "Email must be valid.")));

    assertThat(response.type()).isEqualTo("https://vqc.nghlong3004.me/errors/validation-error");
    assertThat(response.title()).isEqualTo("Validation Error");
    assertThat(response.status()).isEqualTo(400);
    assertThat(response.detail()).isEqualTo("Validation failed for input data.");
    assertThat(response.instance()).isEqualTo("/api/v1/auth/register");
    assertThat(response.code()).isEqualTo("VALIDATION_ERROR");
    assertThat(response.errors()).containsExactly(ErrorResponse.error("email", "Email must be valid."));
  }
}
