package me.nghlong3004.vqc.api.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // ── Validation & Request ──
  VALIDATION_ERROR(
      HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", "Validation failed for input data."),
  MISSING_PARAMETER(
      HttpStatus.BAD_REQUEST.value(), "MISSING_PARAMETER", "The required parameter is missing."),
  HTTP_MESSAGE_NOT_READABLE(
      HttpStatus.BAD_REQUEST.value(), "HTTP_MESSAGE_NOT_READABLE", "Malformed JSON request."),

  // ── Auth ──
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED", "Full authentication is required."),
  BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED.value(), "BAD_CREDENTIALS", "Invalid email or password."),
  ACCESS_TOKEN_EXPIRED(
      HttpStatus.UNAUTHORIZED.value(), "ACCESS_TOKEN_EXPIRED", "Access token has expired."),
  INVALID_ACCESS_TOKEN(
      HttpStatus.UNAUTHORIZED.value(), "INVALID_ACCESS_TOKEN", "Access token is invalid."),
  INVALID_REFRESH_TOKEN(
      HttpStatus.UNAUTHORIZED.value(), "INVALID_REFRESH_TOKEN", "Refresh token is invalid."),
  REFRESH_TOKEN_EXPIRED(
      HttpStatus.UNAUTHORIZED.value(), "REFRESH_TOKEN_EXPIRED", "Refresh token has expired."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN.value(), "ACCESS_DENIED", "You do not have permission."),
  ACCOUNT_LOCKED(HttpStatus.FORBIDDEN.value(), "ACCOUNT_LOCKED", "User account is locked."),
  EMAIL_NOT_VERIFIED(
      HttpStatus.FORBIDDEN.value(), "EMAIL_NOT_VERIFIED", "Account email has not been verified"),
  WRONG_PASSWORD(
      HttpStatus.BAD_REQUEST.value(), "WRONG_PASSWORD", "Current password is incorrect."),
  OAUTH_PASSWORD_NOT_ALLOWED(
      HttpStatus.BAD_REQUEST.value(),
      "OAUTH_PASSWORD_NOT_ALLOWED",
      "Password change is not available for OAuth accounts."),

  // ── User & Email ──
  EMAIL_NOT_FOUND(
      HttpStatus.NOT_FOUND.value(), "EMAIL_NOT_FOUND", "No user found with the provided email."),
  USER_NOT_FOUND(
      HttpStatus.NOT_FOUND.value(), "USER_NOT_FOUND", "No user found with the provided ID."),
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "RESOURCE_NOT_FOUND", "Resources not found."),
  EMAIL_ALREADY(HttpStatus.CONFLICT.value(), "EMAIL_ALREADY_EXISTS", "Email is already in use."),
  USERNAME_ALREADY_EXISTS(
      HttpStatus.CONFLICT.value(), "USERNAME_ALREADY_EXISTS", "Username is already in use."),
  CANNOT_DELETE_SELF(
      HttpStatus.BAD_REQUEST.value(), "CANNOT_DELETE_SELF", "You cannot disable your own account."),
  CANNOT_MODIFY_SUPER_ADMIN(
      HttpStatus.FORBIDDEN.value(),
      "CANNOT_MODIFY_SUPER_ADMIN",
      "Cannot modify or disable a Super Admin account."),

  // ── OTP ──
  INVALID_OTP(HttpStatus.BAD_REQUEST.value(), "INVALID_OTP", "OTP is invalid or has expired."),

  // ── HTTP / Infrastructure ──
  METHOD_NOT_ALLOWED(
      HttpStatus.METHOD_NOT_ALLOWED.value(), "METHOD_NOT_ALLOWED", "Request method not supported."),
  UNSUPPORTED_MEDIA_TYPE(
      HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
      "UNSUPPORTED_MEDIA_TYPE",
      "Content type not supported."),
  RATE_LIMIT_EXCEEDED(
      HttpStatus.TOO_MANY_REQUESTS.value(), "RATE_LIMIT_EXCEEDED", "Too many requests."),
  EMAIL_SEND_FAILED(
      HttpStatus.INTERNAL_SERVER_ERROR.value(), "EMAIL_SEND_FAILED", "Failed to send email."),
  INTERNAL_SERVER_ERROR(
      HttpStatus.INTERNAL_SERVER_ERROR.value(),
      "INTERNAL_SERVER_ERROR",
      "Unexpected server error."),
  ;

  private final int status;
  private final String code;
  private final String message;

  public ErrorResponse toErrorResponse() {
    return new ErrorResponse(message, status, code);
  }
}
