import type { ApiError, RefreshTokenResponse } from "./types";

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------
const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "";
const REFRESH_URL = `${BASE_URL}/api/v1/auth/refresh-token`;
const ACCESS_TOKEN_EXPIRED_CODE = "ACCESS_TOKEN_EXPIRED";

// ---------------------------------------------------------------------------
// Token getter — set externally to avoid circular dependency with auth store
// ---------------------------------------------------------------------------
let getTokenFn: (() => string | null) | null = null;
let clearAuthFn: (() => void) | null = null;
let onRefreshedFn: ((token: string) => void) | null = null;
let onApiErrorFn: ((error: ApiError) => void) | null = null;

/** Register token accessor (called once from auth store / provider init). */
export function setTokenGetter(fn: () => string | null) {
  getTokenFn = fn;
}

/** Register a callback to clear auth state on refresh failure. */
export function setClearAuth(fn: () => void) {
  clearAuthFn = fn;
}

/** Register a callback to persist a newly refreshed token. */
export function setOnRefreshed(fn: (token: string) => void) {
  onRefreshedFn = fn;
}

/** Register a callback to surface normalized API errors in the UI. */
export function setOnApiError(fn: ((error: ApiError) => void) | null) {
  onApiErrorFn = fn;
}

// ---------------------------------------------------------------------------
// Refresh mutex
// ---------------------------------------------------------------------------
let isRefreshing = false;
let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  // If a refresh is already in-flight, piggy-back on the same promise.
  if (isRefreshing && refreshPromise) {
    return refreshPromise;
  }

  isRefreshing = true;
  refreshPromise = (async () => {
    try {
      const res = await fetch(REFRESH_URL, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
      });

      if (!res.ok) {
        return null;
      }

      const data: RefreshTokenResponse = await res.json();
      onRefreshedFn?.(data.accessToken);
      return data.accessToken;
    } catch {
      return null;
    } finally {
      isRefreshing = false;
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

// ---------------------------------------------------------------------------
// Error normalisation
// ---------------------------------------------------------------------------
type ProblemDetailsBody = {
  status?: unknown;
  code?: unknown;
  message?: unknown;
  detail?: unknown;
  title?: unknown;
  errors?: unknown;
};

function isFieldError(item: unknown): item is { field?: string; message: string } {
  return (
    typeof item === "object" &&
    item !== null &&
    "message" in item &&
    typeof (item as { message?: unknown }).message === "string"
  );
}

function normalizeFieldErrors(errors: unknown): ApiError["errors"] | undefined {
  if (!Array.isArray(errors)) {
    return undefined;
  }

  const normalized = errors.filter(isFieldError).map((item) => ({
    field: typeof item.field === "string" ? item.field : "",
    message: item.message,
  }));

  return normalized.length > 0 ? normalized : undefined;
}

function isApiErrorBody(body: unknown): body is ProblemDetailsBody {
  return (
    typeof body === "object" &&
    body !== null &&
    "status" in body &&
    "code" in body
  );
}

function fieldErrorMessage(errors: ApiError["errors"]): string | null {
  if (!errors || errors.length === 0) {
    return null;
  }

  return errors
    .map((error) => (error.field ? `${error.field}: ${error.message}` : error.message))
    .join("\n");
}

function normalizeApiErrorBody(body: ProblemDetailsBody, res: Response): ApiError {
  const errors = normalizeFieldErrors(body.errors);
  const detail = typeof body.detail === "string" ? body.detail : undefined;
  const title = typeof body.title === "string" ? body.title : undefined;
  const message =
    (typeof body.message === "string" && body.message) ||
    fieldErrorMessage(errors) ||
    detail ||
    title ||
    res.statusText ||
    "An unknown error occurred";

  return {
    status: typeof body.status === "number" ? body.status : res.status,
    code: typeof body.code === "string" ? body.code : "UNKNOWN_ERROR",
    message,
    title,
    detail,
    errors,
  };
}

async function toApiError(res: Response): Promise<ApiError> {
  try {
    const body: unknown = await res.json();
    if (isApiErrorBody(body)) {
      return normalizeApiErrorBody(body, res);
    }
  } catch {
    // Response body was not JSON — fall through.
  }

  return {
    status: res.status,
    code: "UNKNOWN_ERROR",
    message: res.statusText || "An unknown error occurred",
  };
}

function notifyApiError(error: ApiError) {
  if (typeof window !== "undefined") {
    onApiErrorFn?.(error);
  }
}

function toHeaderRecord(headers?: HeadersInit): Record<string, string> {
  const record: Record<string, string> = {};

  if (!headers) {
    return record;
  }

  new Headers(headers).forEach((value, key) => {
    record[key] = value;
  });

  return record;
}

function shouldRefreshAccessToken(
  error: ApiError,
  token: string | null,
  skipRefresh: boolean | undefined,
  authMode: RequestOptions["auth"],
): boolean {
  return (
    Boolean(token) &&
    authMode !== "none" &&
    !skipRefresh &&
    error.status === 401 &&
    error.code === ACCESS_TOKEN_EXPIRED_CODE
  );
}

// ---------------------------------------------------------------------------
// Core request function
// ---------------------------------------------------------------------------
type RequestOptions = Omit<RequestInit, "method" | "body"> & {
  /** Skip the automatic 401-refresh-retry cycle (used internally). */
  _skipRefresh?: boolean;
  /** Set to "none" for public auth endpoints that must not attach or refresh access tokens. */
  auth?: "auto" | "none";
};

async function request<T>(
  method: string,
  path: string,
  body?: unknown,
  opts: RequestOptions = {},
): Promise<T> {
  const {
    _skipRefresh,
    auth = "auto",
    headers: extraHeaders,
    ...restOpts
  } = opts;

  const token = auth === "none" ? null : (getTokenFn?.() ?? null);

  const headers: Record<string, string> = {
    ...(body !== undefined && { "Content-Type": "application/json" }),
    ...(token && { Authorization: `Bearer ${token}` }),
    ...toHeaderRecord(extraHeaders),
  };

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    credentials: "include",
    body: body !== undefined ? JSON.stringify(body) : undefined,
    ...restOpts,
  });

  // -- Happy path --
  if (res.ok) {
    // 204 No Content — nothing to parse.
    if (res.status === 204) {
      return undefined as T;
    }
    return (await res.json()) as T;
  }

  const apiError = await toApiError(res);

  // -- Expired access token: attempt silent refresh (once) --
  if (shouldRefreshAccessToken(apiError, token, _skipRefresh, auth)) {
    const newToken = await refreshAccessToken();

    if (newToken) {
      // Retry the original request with the fresh token.
      return request<T>(method, path, body, {
        ...opts,
        _skipRefresh: true,
        headers: {
          ...toHeaderRecord(extraHeaders),
          Authorization: `Bearer ${newToken}`,
        },
      });
    }

    // Refresh failed — clear auth, redirect to login.
    clearAuthFn?.();

    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }
  }

  // -- All other errors --
  notifyApiError(apiError);
  throw apiError;
}

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------
export function get<T>(path: string, opts?: RequestOptions) {
  return request<T>("GET", path, undefined, opts);
}

export function post<T>(path: string, body?: unknown, opts?: RequestOptions) {
  return request<T>("POST", path, body, opts);
}

export function put<T>(path: string, body?: unknown, opts?: RequestOptions) {
  return request<T>("PUT", path, body, opts);
}

export function patch<T>(path: string, body?: unknown, opts?: RequestOptions) {
  return request<T>("PATCH", path, body, opts);
}

export function del<T>(path: string, opts?: RequestOptions) {
  return request<T>("DELETE", path, undefined, opts);
}

/** Namespace export for ergonomic usage: `apiClient.get(...)` */
export const apiClient = { get, post, put, patch, del };
