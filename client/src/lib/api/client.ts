import type { ApiError, RefreshTokenResponse } from './types';

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------
const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? '';
const REFRESH_URL = `${BASE_URL}/api/v1/auth/refresh-token`;

// ---------------------------------------------------------------------------
// Token getter — set externally to avoid circular dependency with auth store
// ---------------------------------------------------------------------------
let getTokenFn: (() => string | null) | null = null;
let clearAuthFn: (() => void) | null = null;
let onRefreshedFn: ((token: string) => void) | null = null;

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
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
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
function isApiErrorBody(body: unknown): body is ApiError {
  return (
    typeof body === 'object' &&
    body !== null &&
    'status' in body &&
    'code' in body &&
    'message' in body
  );
}

async function toApiError(res: Response): Promise<ApiError> {
  try {
    const body: unknown = await res.json();
    if (isApiErrorBody(body)) {
      return body;
    }
  } catch {
    // Response body was not JSON — fall through.
  }

  return {
    status: res.status,
    code: 'UNKNOWN_ERROR',
    message: res.statusText || 'An unknown error occurred',
  };
}

// ---------------------------------------------------------------------------
// Core request function
// ---------------------------------------------------------------------------
type RequestOptions = Omit<RequestInit, 'method' | 'body'> & {
  /** Skip the automatic 401-refresh-retry cycle (used internally). */
  _skipRefresh?: boolean;
};

async function request<T>(
  method: string,
  path: string,
  body?: unknown,
  opts: RequestOptions = {},
): Promise<T> {
  const { _skipRefresh, headers: extraHeaders, ...restOpts } = opts;

  const token = getTokenFn?.() ?? null;

  const headers: Record<string, string> = {
    ...(body !== undefined && { 'Content-Type': 'application/json' }),
    ...(token && { Authorization: `Bearer ${token}` }),
    ...(extraHeaders as Record<string, string>),
  };

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    credentials: 'include',
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

  // -- 401: attempt silent refresh (once) --
  if (res.status === 401 && !_skipRefresh) {
    const newToken = await refreshAccessToken();

    if (newToken) {
      // Retry the original request with the fresh token.
      return request<T>(method, path, body, {
        ...opts,
        _skipRefresh: true,
        headers: {
          ...(extraHeaders as Record<string, string>),
          Authorization: `Bearer ${newToken}`,
        },
      });
    }

    // Refresh failed — clear auth, redirect to login.
    clearAuthFn?.();

    if (typeof window !== 'undefined') {
      window.location.href = '/login';
    }
  }

  // -- All other errors --
  const apiError = await toApiError(res);
  throw apiError;
}

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------
export function get<T>(path: string, opts?: RequestOptions) {
  return request<T>('GET', path, undefined, opts);
}

export function post<T>(path: string, body?: unknown, opts?: RequestOptions) {
  return request<T>('POST', path, body, opts);
}

export function put<T>(path: string, body?: unknown, opts?: RequestOptions) {
  return request<T>('PUT', path, body, opts);
}

export function patch<T>(path: string, body?: unknown, opts?: RequestOptions) {
  return request<T>('PATCH', path, body, opts);
}

export function del<T>(path: string, opts?: RequestOptions) {
  return request<T>('DELETE', path, undefined, opts);
}

/** Namespace export for ergonomic usage: `apiClient.get(...)` */
export const apiClient = { get, post, put, patch, del };

