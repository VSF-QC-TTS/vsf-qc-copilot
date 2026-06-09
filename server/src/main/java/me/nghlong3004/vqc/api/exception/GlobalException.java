package me.nghlong3004.vqc.api.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
  public ResponseEntity<ErrorResponse> handleResourceException(final ResourceException e) {
    log.warn("Resource exception occurred: {}", e.getMessage());
    final var errorCode = e.getResponse();
    return new ResponseEntity<>(errorCode, HttpStatus.valueOf(errorCode.status()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex) {
    var fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .toList();
    log.debug("Validation failed: {}", fieldErrors);
    var errorCode = ErrorCode.VALIDATION_ERROR;
    return new ResponseEntity<>(
        new ErrorResponse(
            errorCode.getMessage(), errorCode.getStatus(), errorCode.getCode(), fieldErrors),
        HttpStatus.valueOf(errorCode.getStatus()));
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(
      HandlerMethodValidationException ex) {
    var details =
        ex.getParameterValidationResults().stream()
            .flatMap(
                result ->
                    result.getResolvableErrors().stream()
                        .map(
                            error ->
                                result.getMethodParameter().getParameterName()
                                    + ": "
                                    + error.getDefaultMessage()))
            .toList();
    log.debug("Handler method validation failed: {}", details);
    var errorCode = ErrorCode.VALIDATION_ERROR;
    return new ResponseEntity<>(
        new ErrorResponse(
            errorCode.getMessage(), errorCode.getStatus(), errorCode.getCode(), details),
        HttpStatus.valueOf(errorCode.getStatus()));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
    var details =
        ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .toList();
    log.debug("Constraint violation: {}", details);
    var errorCode = ErrorCode.VALIDATION_ERROR;
    return new ResponseEntity<>(
        new ErrorResponse(
            errorCode.getMessage(), errorCode.getStatus(), errorCode.getCode(), details),
        HttpStatus.valueOf(errorCode.getStatus()));
  }

  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    MissingServletRequestPartException.class
  })
  public ResponseEntity<ErrorResponse> handleMissingParameterException(Exception e) {
    log.debug("Missing request parameter/part: {}", e.getMessage());
    return buildResponse(ErrorCode.MISSING_PARAMETER);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatchException(
      MethodArgumentTypeMismatchException e) {
    log.debug("Type mismatch for parameter '{}': {}", e.getName(), e.getMessage());
    return buildResponse(ErrorCode.VALIDATION_ERROR);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException e) {
    log.debug("Malformed JSON request: {}", e.getMessage());
    return buildResponse(ErrorCode.HTTP_MESSAGE_NOT_READABLE);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
    log.warn("Access denied: {}", e.getMessage());
    return buildResponse(ErrorCode.ACCESS_DENIED);
  }

  @ExceptionHandler({NoHandlerFoundException.class, NoSuchElementException.class})
  public ResponseEntity<ErrorResponse> handleNotFoundException(Exception e) {
    log.debug("Resource not found: {}", e.getMessage());
    return buildResponse(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
      HttpRequestMethodNotSupportedException e) {
    log.debug("Method not allowed: {}", e.getMessage());
    return buildResponse(ErrorCode.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
      HttpMediaTypeNotSupportedException e) {
    log.debug("{}", e.getMessage());
    return buildResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleRuntimeException(final Exception e) {
    log.error("Unhandled Exception: ", e);
    return buildResponse(ErrorCode.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<ErrorResponse> buildResponse(ErrorCode errorCode) {
    return new ResponseEntity<>(
        errorCode.toErrorResponse(), HttpStatus.valueOf(errorCode.getStatus()));
  }
}
