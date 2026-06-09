package me.nghlong3004.vqc.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Slf4j
@RestControllerAdvice
public class GlobalException {

  @ExceptionHandler(ResourceException.class)
  public ResponseEntity<ErrorResponse> handleResourceException(
      final ResourceException e, HttpServletRequest request) {
    log.warn("Resource exception occurred: {}", e.getMessage());
    final var errorCode = e.getResponse().withInstance(request.getRequestURI());
    return problemResponse(errorCode);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    var fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> ErrorResponse.error(error.getField(), error.getDefaultMessage()))
            .toList();
    log.debug("Validation failed: {}", fieldErrors);
    var errorCode = ErrorCode.VALIDATION_ERROR;
    return problemResponse(
        errorCode.toErrorResponse().withErrors(fieldErrors).withInstance(request.getRequestURI()));
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(
      HandlerMethodValidationException ex, HttpServletRequest request) {
    var details =
        ex.getParameterValidationResults().stream()
            .flatMap(
                result ->
                    result.getResolvableErrors().stream()
                        .map(
                            error ->
                                ErrorResponse.error(
                                    result.getMethodParameter().getParameterName(),
                                    error.getDefaultMessage())))
            .toList();
    log.debug("Handler method validation failed: {}", details);
    var errorCode = ErrorCode.VALIDATION_ERROR;
    return problemResponse(
        errorCode.toErrorResponse().withErrors(details).withInstance(request.getRequestURI()));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {
    var details =
        ex.getConstraintViolations().stream()
            .map(v -> ErrorResponse.error(v.getPropertyPath().toString(), v.getMessage()))
            .toList();
    log.debug("Constraint violation: {}", details);
    var errorCode = ErrorCode.VALIDATION_ERROR;
    return problemResponse(
        errorCode.toErrorResponse().withErrors(details).withInstance(request.getRequestURI()));
  }

  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    MissingServletRequestPartException.class
  })
  public ResponseEntity<ErrorResponse> handleMissingParameterException(
      Exception e, HttpServletRequest request) {
    log.debug("Missing request parameter/part: {}", e.getMessage());
    return buildResponse(ErrorCode.MISSING_PARAMETER, request);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatchException(
      MethodArgumentTypeMismatchException e, HttpServletRequest request) {
    log.debug("Type mismatch for parameter '{}': {}", e.getName(), e.getMessage());
    return buildResponse(ErrorCode.VALIDATION_ERROR, request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException e, HttpServletRequest request) {
    log.debug("Malformed JSON request: {}", e.getMessage());
    return buildResponse(ErrorCode.HTTP_MESSAGE_NOT_READABLE, request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(
      AccessDeniedException e, HttpServletRequest request) {
    log.warn("Access denied: {}", e.getMessage());
    return buildResponse(ErrorCode.ACCESS_DENIED, request);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(
      AuthenticationException e, HttpServletRequest request) {
    log.warn("Authentication failed: {}", e.getMessage());
    return buildResponse(ErrorCode.BAD_CREDENTIALS, request);
  }

  @ExceptionHandler({NoHandlerFoundException.class, NoSuchElementException.class})
  public ResponseEntity<ErrorResponse> handleNotFoundException(Exception e, HttpServletRequest request) {
    log.debug("Resource not found: {}", e.getMessage());
    return buildResponse(ErrorCode.RESOURCE_NOT_FOUND, request);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
      HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
    log.debug("Method not allowed: {}", e.getMessage());
    return buildResponse(ErrorCode.METHOD_NOT_ALLOWED, request);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
      HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
    log.debug("{}", e.getMessage());
    return buildResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE, request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleRuntimeException(
      final Exception e, HttpServletRequest request) {
    log.error("Unhandled Exception: ", e);
    return buildResponse(ErrorCode.INTERNAL_SERVER_ERROR, request);
  }

  private ResponseEntity<ErrorResponse> buildResponse(ErrorCode errorCode, HttpServletRequest request) {
    return problemResponse(errorCode.toErrorResponse(request.getRequestURI()));
  }

  private ResponseEntity<ErrorResponse> problemResponse(ErrorResponse errorResponse) {
    return ResponseEntity.status(HttpStatus.valueOf(errorResponse.status()))
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(errorResponse);
  }
}
