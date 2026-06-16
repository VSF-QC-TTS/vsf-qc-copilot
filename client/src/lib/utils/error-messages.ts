import type { ApiError } from "@/lib/api/types";

type ErrorTranslator = (key: string, values?: any) => string;

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
