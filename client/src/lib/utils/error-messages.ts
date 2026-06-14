import type { ApiError } from "@/lib/api/types";

type ErrorTranslator = (key: string) => string;

/**
 * Map a backend error code to an i18n message key under `errors.*`.
 * Falls back to generic error key if the code is unknown.
 */
export function getErrorMessageKey(error: ApiError): string {
  const knownCodes = [
    "BAD_CREDENTIALS",
    "INVALID_CREDENTIALS",
    "EMAIL_ALREADY_EXISTS",
    "USER_NOT_FOUND",
    "ACCOUNT_DISABLED",
    "EMAIL_NOT_VERIFIED",
    "TOKEN_EXPIRED",
    "TOKEN_INVALID",
    "PROJECT_NOT_FOUND",
    "DATASET_NOT_FOUND",
    "CONNECTOR_NOT_FOUND",
    "RUBRIC_NOT_FOUND",
    "EVALUATION_NOT_FOUND",
    "DATASET_NOT_DRAFT",
    "DATASET_NOT_APPROVED",
    "RUBRIC_VERSION_NOT_PUBLISHED",
    "TEST_CASE_LIMIT_EXCEEDED",
    "VALIDATION_ERROR",
    "MISSING_PARAMETER",
    "HTTP_MESSAGE_NOT_READABLE",
    "RESOURCE_NOT_FOUND",
    "METHOD_NOT_ALLOWED",
    "UNSUPPORTED_MEDIA_TYPE",
    "ACCESS_DENIED",
    "INTERNAL_SERVER_ERROR",
  ];

  if (knownCodes.includes(error.code)) {
    return `errors.${error.code}`;
  }

  // HTTP status fallbacks
  if (error.status === 401) return "errors.unauthorized";
  if (error.status === 404) return "errors.notFound";
  if (error.status === 422) return "errors.validation";

  return "errors.generic";
}

export function isApiError(error: unknown): error is ApiError {
  return (
    typeof error === "object" &&
    error !== null &&
    "status" in error &&
    "code" in error &&
    "message" in error
  );
}

function backendValidationMessage(error: ApiError): string | null {
  if (!error.errors || error.errors.length === 0) {
    return null;
  }

  return error.errors
    .map((item) => (item.field ? `${item.field}: ${item.message}` : item.message))
    .join("\n");
}

export function getBackendErrorMessage(error: ApiError): string | null {
  return backendValidationMessage(error) ?? error.message ?? error.detail ?? null;
}

export function getErrorMessage(error: unknown, tErrors: ErrorTranslator): string {
  if (!isApiError(error)) {
    return tErrors("network");
  }

  const messageKey = getErrorMessageKey(error).replace(/^errors\./, "");
  const backendMessage = getBackendErrorMessage(error);

  if (
    backendMessage &&
    (messageKey === "generic" ||
      messageKey === "validation" ||
      messageKey === "VALIDATION_ERROR")
  ) {
    return backendMessage;
  }

  try {
    return tErrors(messageKey);
  } catch {
    return backendMessage ?? tErrors("generic");
  }
}
