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
  INVALID_EMAIL_VERIFICATION_TOKEN(
      HttpStatus.BAD_REQUEST.value(),
      "INVALID_EMAIL_VERIFICATION_TOKEN",
      "Email verification token is invalid."),
  EMAIL_VERIFICATION_TOKEN_EXPIRED(
      HttpStatus.BAD_REQUEST.value(),
      "EMAIL_VERIFICATION_TOKEN_EXPIRED",
      "Email verification token has expired."),
  EMAIL_VERIFICATION_TOKEN_USED(
      HttpStatus.BAD_REQUEST.value(),
      "EMAIL_VERIFICATION_TOKEN_USED",
      "Email verification token has already been used."),
  INVALID_PASSWORD_RESET_TOKEN(
      HttpStatus.BAD_REQUEST.value(),
      "INVALID_PASSWORD_RESET_TOKEN",
      "Password reset token is invalid."),
  PASSWORD_RESET_TOKEN_EXPIRED(
      HttpStatus.BAD_REQUEST.value(),
      "PASSWORD_RESET_TOKEN_EXPIRED",
      "Password reset token has expired."),
  PASSWORD_RESET_TOKEN_USED(
      HttpStatus.BAD_REQUEST.value(),
      "PASSWORD_RESET_TOKEN_USED",
      "Password reset token has already been used."),
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

  // ── Projects ──
  PROJECT_NOT_FOUND(
      HttpStatus.NOT_FOUND.value(), "PROJECT_NOT_FOUND", "No project found with the provided ID."),
  TARGET_CONNECTOR_NOT_FOUND(
      HttpStatus.NOT_FOUND.value(),
      "TARGET_CONNECTOR_NOT_FOUND",
      "No target connector found with the provided ID."),
  JUDGE_MODEL_NOT_FOUND(
      HttpStatus.NOT_FOUND.value(),
      "JUDGE_MODEL_NOT_FOUND",
      "No judge model found with the provided ID."),
  JUDGE_MODEL_INACTIVE(
      422,
      "JUDGE_MODEL_INACTIVE",
      "Judge model must be active before starting an evaluation run."),
  CURL_PARSE_ERROR(
      HttpStatus.BAD_REQUEST.value(),
      "CURL_PARSE_ERROR",
      "Failed to parse the provided cURL command."),
  TARGET_CONNECTOR_TEST_FAILED(
      422,
      "TARGET_CONNECTOR_TEST_FAILED",
      "The target API call failed. Please verify your cURL command and try again."),
  DATASET_NOT_FOUND(
      HttpStatus.NOT_FOUND.value(),
      "DATASET_NOT_FOUND",
      "No dataset found with the provided ID."),
  DATASET_APPROVAL_INVALID(
      422,
      "DATASET_APPROVAL_INVALID",
      "Dataset cannot be approved with the current test case count."),
  DATASET_ARCHIVED(
      HttpStatus.CONFLICT.value(),
      "DATASET_ARCHIVED",
      "Archived dataset cannot be modified."),
  TEST_CASE_NOT_FOUND(
      HttpStatus.NOT_FOUND.value(),
      "TEST_CASE_NOT_FOUND",
      "No test case found with the provided ID."),
  RUBRIC_NOT_FOUND(
      HttpStatus.NOT_FOUND.value(), "RUBRIC_NOT_FOUND", "No rubric found with the provided ID."),
  RUBRIC_ARCHIVED(
      HttpStatus.CONFLICT.value(), "RUBRIC_ARCHIVED", "Archived rubric cannot be modified."),
  RUBRIC_VERSION_NOT_FOUND(
      HttpStatus.NOT_FOUND.value(),
      "RUBRIC_VERSION_NOT_FOUND",
      "No rubric version found with the provided ID."),
  RUBRIC_VERSION_IMMUTABLE(
      HttpStatus.CONFLICT.value(),
      "RUBRIC_VERSION_IMMUTABLE",
      "Rubric version cannot be modified in its current status."),
  RUBRIC_VERSION_PUBLISH_INVALID(
      422,
      "RUBRIC_VERSION_PUBLISH_INVALID",
      "Rubric version cannot be published with the current criteria."),
  RUBRIC_CRITERION_NOT_FOUND(
      HttpStatus.NOT_FOUND.value(),
      "RUBRIC_CRITERION_NOT_FOUND",
      "No rubric criterion found with the provided ID."),
  RUBRIC_CRITERION_METRIC_KEY_CONFLICT(
      HttpStatus.CONFLICT.value(),
      "RUBRIC_CRITERION_METRIC_KEY_CONFLICT",
      "Rubric criterion metric key already exists in this version."),
  RUBRIC_GENERATION_FAILED(
      422,
      "RUBRIC_GENERATION_FAILED",
      "AI could not generate a valid rubric preview."),

  // ── Evaluation ──
  DATASET_NOT_APPROVED(
      422,
      "DATASET_NOT_APPROVED",
      "Dataset must be APPROVED before starting an evaluation run."),
  DATASET_NO_ACTIVE_CASES(
      422,
      "DATASET_NO_ACTIVE_CASES",
      "Dataset must have at least one active test case."),
  DATASET_TOO_MANY_CASES(
      422,
      "DATASET_TOO_MANY_CASES",
      "Dataset exceeds the maximum number of active test cases (100)."),
  RUBRIC_VERSION_NOT_PUBLISHED(
      422,
      "RUBRIC_VERSION_NOT_PUBLISHED",
      "Rubric version must be PUBLISHED before starting an evaluation run."),
  CONNECTOR_NOT_ACTIVE(
      422,
      "CONNECTOR_NOT_ACTIVE",
      "Target connector must be active before starting an evaluation run."),
  EVALUATION_RUN_NOT_FOUND(
      HttpStatus.NOT_FOUND.value(),
      "EVALUATION_RUN_NOT_FOUND",
      "No evaluation run found with the provided ID."),
  JOB_NOT_FOUND(
      HttpStatus.NOT_FOUND.value(),
      "JOB_NOT_FOUND",
      "No job found with the provided ID."),
  EVALUATION_RESULT_NOT_FOUND(
      HttpStatus.NOT_FOUND.value(),
      "EVALUATION_RESULT_NOT_FOUND",
      "No evaluation result found with the provided ID."),
  REVIEW_DECISION_NOT_FOUND(
      HttpStatus.NOT_FOUND.value(),
      "REVIEW_DECISION_NOT_FOUND",
      "No review decision found with the provided ID."),
  REVIEW_DECISION_STATUS_INVALID(
      HttpStatus.BAD_REQUEST.value(),
      "REVIEW_DECISION_STATUS_INVALID",
      "Review decision status is not allowed for write operations."),
  PIC_BUG_USER_NOT_FOUND(
      HttpStatus.NOT_FOUND.value(),
      "PIC_BUG_USER_NOT_FOUND",
      "No active PIC bug user found with the provided ID."),
  EXPORT_FILE_NOT_FOUND(
      HttpStatus.NOT_FOUND.value(),
      "EXPORT_FILE_NOT_FOUND",
      "No export file found with the provided ID."),
  EXPORT_FILE_NOT_READY(
      HttpStatus.CONFLICT.value(),
      "EXPORT_FILE_NOT_READY",
      "Export file is not ready for download."),

  // ── Import ──
  IMPORT_FILE_EMPTY(
      HttpStatus.BAD_REQUEST.value(),
      "IMPORT_FILE_EMPTY",
      "Import file is empty or contains no data rows."),
  IMPORT_FILE_TOO_LARGE(
      HttpStatus.BAD_REQUEST.value(),
      "IMPORT_FILE_TOO_LARGE",
      "Import file exceeds the maximum allowed size (5 MB)."),
  IMPORT_FILE_INVALID_FORMAT(
      HttpStatus.BAD_REQUEST.value(),
      "IMPORT_FILE_INVALID_FORMAT",
      "Import file format is not supported. Use .xlsx or .csv."),
  IMPORT_TOO_MANY_ROWS(
      422,
      "IMPORT_TOO_MANY_ROWS",
      "Import would exceed the maximum number of test cases (100) for this dataset."),

  // ── Quick Evaluate ──
  QUICK_EVALUATE_AMBIGUOUS(
      422,
      "QUICK_EVALUATE_AMBIGUOUS",
      "Cannot auto-resolve: expected exactly one candidate but found zero or multiple."),

  // ── User Administration ──
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

  public ErrorResponse toErrorResponse(String instance) {
    return toErrorResponse().withInstance(instance);
  }
}
