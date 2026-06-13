import type { ApiError } from "@/lib/api/types";

/**
 * Map a backend error code to an i18n message key under `errors.*`.
 * Falls back to generic error key if the code is unknown.
 */
export function getErrorMessageKey(error: ApiError): string {
  const knownCodes = [
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
