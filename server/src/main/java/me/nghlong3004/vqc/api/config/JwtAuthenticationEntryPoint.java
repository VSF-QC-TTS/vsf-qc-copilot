package me.nghlong3004.vqc.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      @NonNull HttpServletRequest request,
      HttpServletResponse response,
      @NonNull AuthenticationException authException)
      throws IOException {

    ErrorCode errorCode = resolveErrorCode(authException);

    log.debug("Authentication failed [{}]: {}", errorCode.getCode(), authException.getMessage());

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(errorCode.getStatus());
    objectMapper.writeValue(response.getOutputStream(), errorCode.toErrorResponse());
  }

  private ErrorCode resolveErrorCode(AuthenticationException ex) {
    if (ex instanceof InvalidBearerTokenException) {
      String message = ex.getMessage();
      if (message != null && message.toLowerCase().contains("expired")) {
        return ErrorCode.ACCESS_TOKEN_EXPIRED;
      }
      return ErrorCode.INVALID_ACCESS_TOKEN;
    }
    return ErrorCode.UNAUTHORIZED;
  }
}
