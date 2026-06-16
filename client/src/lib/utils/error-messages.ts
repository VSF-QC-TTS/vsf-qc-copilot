import type { ApiError } from "@/lib/api/types";

type ErrorTranslator = (key: string, values?: Record<string, string | number>) => string;

/**
 * Map a backend error code to an i18n message key under `errors.*`.
 * Falls back to generic error key if the code is unknown.
 */
export function getErrorMessageKey(error: ApiError): string {
  const knownCodes = [
    // Validation & Request
    "VALIDATION_ERROR",
    "MISSING_PARAMETER",
    "HTTP_MESSAGE_NOT_READABLE",

    // Auth
    "UNAUTHORIZED",
    "BAD_CREDENTIALS",
    "INVALID_CREDENTIALS",
    "ACCESS_TOKEN_EXPIRED",
    "INVALID_ACCESS_TOKEN",
    "INVALID_REFRESH_TOKEN",
    "REFRESH_TOKEN_EXPIRED",
    "ACCESS_DENIED",
    "ACCOUNT_LOCKED",
    "EMAIL_NOT_VERIFIED",
    "INVALID_EMAIL_VERIFICATION_TOKEN",
    "EMAIL_VERIFICATION_TOKEN_EXPIRED",
    "EMAIL_VERIFICATION_TOKEN_USED",
    "INVALID_PASSWORD_RESET_TOKEN",
    "PASSWORD_RESET_TOKEN_EXPIRED",
    "PASSWORD_RESET_TOKEN_USED",
    "WRONG_PASSWORD",
    "OAUTH_PASSWORD_NOT_ALLOWED",

    // User & Email
    "EMAIL_NOT_FOUND",
    "USER_NOT_FOUND",
    "RESOURCE_NOT_FOUND",
    "EMAIL_ALREADY_EXISTS",
    "USERNAME_ALREADY_EXISTS",

    // Projects
    "PROJECT_NOT_FOUND",
    "TARGET_CONNECTOR_NOT_FOUND",
    "CONNECTOR_NOT_FOUND",
    "JUDGE_MODEL_NOT_FOUND",
    "JUDGE_MODEL_INACTIVE",
    "CURL_PARSE_ERROR",
    "TARGET_CONNECTOR_TEST_FAILED",
    "TARGET_CONNECTOR_RESPONSE_EXTRACTION_FAILED",
    "DATASET_NOT_FOUND",
    "DATASET_APPROVAL_INVALID",
    "DATASET_ARCHIVED",
    "TEST_CASE_NOT_FOUND",
    "RUBRIC_NOT_FOUND",
    "RUBRIC_ARCHIVED",
    "RUBRIC_VERSION_NOT_FOUND",
    "RUBRIC_VERSION_IMMUTABLE",
    "RUBRIC_VERSION_PUBLISH_INVALID",
    "RUBRIC_CRITERION_NOT_FOUND",
    "RUBRIC_CRITERION_METRIC_KEY_CONFLICT",
    "RUBRIC_GENERATION_FAILED",

    // Evaluation
    "DATASET_NOT_APPROVED",
    "DATASET_NO_ACTIVE_CASES",
    "DATASET_TOO_MANY_CASES",
    "TEST_CASE_LIMIT_EXCEEDED",
    "RUBRIC_VERSION_NOT_PUBLISHED",
    "CONNECTOR_NOT_ACTIVE",
    "EVALUATION_RUN_NOT_FOUND",
    "EVALUATION_NOT_FOUND",
    "JOB_NOT_FOUND",
    "EVALUATION_RESULT_NOT_FOUND",
    "REVIEW_DECISION_NOT_FOUND",
    "REVIEW_DECISION_STATUS_INVALID",
    "PIC_BUG_USER_NOT_FOUND",
    "EXPORT_FILE_NOT_FOUND",
    "EXPORT_FILE_NOT_READY",
    "RED_TEAM_RUN_NOT_FOUND",

    // Import
    "IMPORT_FILE_EMPTY",
    "IMPORT_FILE_TOO_LARGE",
    "IMPORT_FILE_INVALID_FORMAT",
    "IMPORT_TOO_MANY_ROWS",

    // Quick Evaluate
    "QUICK_EVALUATE_AMBIGUOUS",

    // User Administration
    "CANNOT_DELETE_SELF",
    "CANNOT_MODIFY_SUPER_ADMIN",

    // OTP
    "INVALID_OTP",

    // HTTP / Infrastructure
    "METHOD_NOT_ALLOWED",
    "UNSUPPORTED_MEDIA_TYPE",
    "RATE_LIMIT_EXCEEDED",
    "EMAIL_SEND_FAILED",
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

function translateValidationMessage(msg: string, tErrors?: ErrorTranslator): string {
  if (!tErrors) return msg;

  const cleanMsg = msg.trim().replace(/\.$/, "");
  const customMap: Record<string, string> = {
    "Email must be a valid email address": "validation_custom.email_valid",
    "Metric key must start with a lowercase letter and contain lowercase letters, numbers, or underscores": "validation_custom.metric_key_format"
  };

  if (customMap[cleanMsg]) {
    try {
      const translated = tErrors(customMap[cleanMsg]);
      if (translated && !translated.startsWith("errors.")) {
        return translated;
      }
    } catch {
      // ignore
    }
  }

  const translateField = (field: string): string => {
    const key = field.toLowerCase().trim().replace(/\s+/g, "_");
    try {
      const translated = tErrors(`validation_fields.${key}`);
      if (translated && !translated.startsWith("errors.")) {
        return translated;
      }
    } catch {
      // ignore
    }
    return field;
  };

  const rules = [
    {
      regex: /^(.+) is required\.$/i,
      translate: (match: RegExpMatchArray) => {
        const field = translateField(match[1]);
        return tErrors("validation_rules.required", { field });
      }
    },
    {
      regex: /^(.+) must not be blank\.$/i,
      translate: (match: RegExpMatchArray) => {
        const field = translateField(match[1]);
        return tErrors("validation_rules.not_blank", { field });
      }
    },
    {
      regex: /^(.+) must be at most (\d+) characters\.$/i,
      translate: (match: RegExpMatchArray) => {
        const field = translateField(match[1]);
        const num = match[2];
        return tErrors("validation_rules.at_most_chars", { field, num });
      }
    },
    {
      regex: /^(.+) must be between (\d+) and (\d+) characters\.$/i,
      translate: (match: RegExpMatchArray) => {
        const field = translateField(match[1]);
        const num1 = match[2];
        const num2 = match[3];
        return tErrors("validation_rules.between_chars", { field, num1, num2 });
      }
    },
    {
      regex: /^(.+) must be at least (\d+)\.$/i,
      translate: (match: RegExpMatchArray) => {
        const field = translateField(match[1]);
        const num = match[2];
        return tErrors("validation_rules.at_least_val", { field, num });
      }
    },
    {
      regex: /^(.+) must be at most (\d+)\.$/i,
      translate: (match: RegExpMatchArray) => {
        const field = translateField(match[1]);
        const num = match[2];
        return tErrors("validation_rules.at_most_val", { field, num });
      }
    }
  ];

  for (const rule of rules) {
    const match = msg.match(rule.regex);
    if (match) {
      try {
        const result = rule.translate(match);
        if (result && !result.startsWith("errors.")) {
          return result;
        }
      } catch {
        // ignore
      }
    }
  }

  return msg;
}

function backendValidationMessage(error: ApiError, tErrors?: ErrorTranslator): string | null {
  if (!error.errors || error.errors.length === 0) {
    return null;
  }

  return error.errors
    .map((item) => {
      if (!item.message) return item.field ? `${item.field}: ` : "";
      const translatedMsg = translateValidationMessage(item.message, tErrors);
      return item.field ? `${item.field}: ${translatedMsg}` : translatedMsg;
    })
    .join("\n");
}

export function getBackendErrorMessage(error: ApiError, tErrors?: ErrorTranslator): string | null {
  return backendValidationMessage(error, tErrors) ?? error.message ?? error.detail ?? null;
}

export function getErrorMessage(error: unknown, tErrors: ErrorTranslator): string {
  if (!isApiError(error)) {
    return tErrors("network");
  }

  const messageKey = getErrorMessageKey(error).replace(/^errors\./, "");
  const backendMessage = getBackendErrorMessage(error, tErrors);

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
