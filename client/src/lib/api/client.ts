import axios, { AxiosError, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios';
import createAuthRefreshInterceptor from 'axios-auth-refresh';
import type { ApiError, RefreshTokenResponse } from './types';

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------
const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? '';
const REFRESH_URL = `${BASE_URL}/api/v1/auth/refresh-token`;
const ACCESS_TOKEN_EXPIRED_CODE = 'ACCESS_TOKEN_EXPIRED';

// ---------------------------------------------------------------------------
// Token getter — set externally to avoid circular dependency with auth store
// ---------------------------------------------------------------------------
let getTokenFn: (() => string | null) | null = null;
let clearAuthFn: (() => void) | null = null;
let onRefreshedFn: ((token: string) => void) | null = null;
let onApiErrorFn: ((error: ApiError) => void) | null = null;

export function setTokenGetter(fn: () => string | null) { getTokenFn = fn; }
export function setClearAuth(fn: () => void) { clearAuthFn = fn; }
export function setOnRefreshed(fn: (token: string) => void) { onRefreshedFn = fn; }
export function setOnApiError(fn: ((error: ApiError) => void) | null) { onApiErrorFn = fn; }

// ---------------------------------------------------------------------------
// Axios Instance
// ---------------------------------------------------------------------------

const axiosInstance = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
});

// ---------------------------------------------------------------------------
// Interceptors
// ---------------------------------------------------------------------------

// 1. Attach Access Token
axiosInstance.interceptors.request.use(
  (config) => {
    const customConfig = config as InternalAxiosRequestConfig & { _authMode?: string };
    if (customConfig._authMode === 'none') return config;
    const token = getTokenFn?.();
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 2. Token Refresh Logic
const refreshAuthLogic = async (failedRequest: AxiosError) => {
  try {
    const res = await axios.post<RefreshTokenResponse>(REFRESH_URL, undefined, {
      withCredentials: true,
    });
    
    const newToken = res.data.accessToken;
    if (!newToken) throw new Error('No token returned');

    onRefreshedFn?.(newToken);
    if (failedRequest.response) {
      failedRequest.response.config.headers.Authorization = `Bearer ${newToken}`;
    }
    
    return Promise.resolve();
  } catch (error) {
    clearAuthFn?.();
    if (typeof window !== 'undefined') window.location.href = '/login';
    return Promise.reject(error);
  }
};

// 3. Register Auth Refresh Interceptor
createAuthRefreshInterceptor(axiosInstance, refreshAuthLogic, {
  statusCodes: [401],
  shouldRefresh: (error: AxiosError) => {
    const body = error.response?.data as ProblemDetailsBody | undefined;
    return body?.code === ACCESS_TOKEN_EXPIRED_CODE;
  },
});

// 4. Response Error Normalization
axiosInstance.interceptors.response.use(
  (response) => {
    if (response.status === 204) return { ...response, data: undefined };
    return response;
  },
  (error: AxiosError) => {
    if (
      typeof window !== 'undefined' &&
      window.location.pathname !== '/login' && 
      error.config?.url === REFRESH_URL
    ) {
        return Promise.reject(error);
    }

    const apiError = toApiError(error);
    if (apiError.code !== ACCESS_TOKEN_EXPIRED_CODE) {
      notifyApiError(apiError);
    }
    return Promise.reject(apiError);
  }
);

// ---------------------------------------------------------------------------
// Error normalisation
// ---------------------------------------------------------------------------
type ProblemDetailsBody = {
  status?: unknown; code?: unknown; message?: unknown;
  detail?: unknown; title?: unknown; errors?: unknown;
};

function isFieldError(item: unknown): item is { field?: string; message: string } {
  return typeof item === 'object' && item !== null && 'message' in item && typeof (item as Record<string, unknown>).message === 'string';
}

function normalizeFieldErrors(errors: unknown): ApiError['errors'] | undefined {
  if (!Array.isArray(errors)) return undefined;
  const normalized = errors.filter(isFieldError).map((item) => ({
    field: typeof item.field === 'string' ? item.field : '',
    message: item.message,
  }));
  return normalized.length > 0 ? normalized : undefined;
}

function isApiErrorBody(body: unknown): body is ProblemDetailsBody {
  return typeof body === 'object' && body !== null && 'status' in body && 'code' in body;
}

function fieldErrorMessage(errors: ApiError['errors']): string | null {
  if (!errors || errors.length === 0) return null;
  return errors.map((error) => (error.field ? `${error.field}: ${error.message}` : error.message)).join('\n');
}

function normalizeApiErrorBody(body: ProblemDetailsBody, status: number, statusText: string): ApiError {
  const errors = normalizeFieldErrors(body.errors);
  const detail = typeof body.detail === 'string' ? body.detail : undefined;
  const title = typeof body.title === 'string' ? body.title : undefined;
  const message =
    (typeof body.message === 'string' && body.message) ||
    fieldErrorMessage(errors) || detail || title || statusText || 'An unknown error occurred';

  return {
    status: typeof body.status === 'number' ? body.status : status,
    code: typeof body.code === 'string' ? body.code : 'UNKNOWN_ERROR',
    message, title, detail, errors,
  };
}

function toApiError(error: AxiosError): ApiError {
  const status = error.response?.status ?? 500;
  const statusText = error.response?.statusText ?? error.message;

  if (error.response?.data && isApiErrorBody(error.response.data)) {
    return normalizeApiErrorBody(error.response.data, status, statusText);
  }

  return { status, code: 'UNKNOWN_ERROR', message: statusText || 'An unknown error occurred' };
}

function notifyApiError(error: ApiError) {
  if (typeof window !== 'undefined') onApiErrorFn?.(error);
}

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

export type RequestOptions = Omit<RequestInit, 'method' | 'body'> & {
  auth?: 'auto' | 'none';
  _skipRefresh?: boolean;
};

function toAxiosConfig(opts?: RequestOptions): AxiosRequestConfig {
  if (!opts) return {};
  const { auth, headers, signal, credentials, ...rest } = opts;
  return {
    headers: headers as AxiosRequestConfig['headers'],
    signal,
    withCredentials: credentials !== 'omit', // fetch defaults 'same-origin', axios defaults true if mapped this way, but we configured axiosInstance to always send credentials
    _authMode: auth,
    ...rest
  } as AxiosRequestConfig;
}

export async function get<T>(path: string, opts?: RequestOptions): Promise<T> {
  const res = await axiosInstance.get<T>(path, toAxiosConfig(opts));
  return res.data;
}

export async function post<T>(path: string, body?: unknown, opts?: RequestOptions): Promise<T> {
  const res = await axiosInstance.post<T>(path, body, toAxiosConfig(opts));
  return res.data;
}

export async function put<T>(path: string, body?: unknown, opts?: RequestOptions): Promise<T> {
  const res = await axiosInstance.put<T>(path, body, toAxiosConfig(opts));
  return res.data;
}

export async function patch<T>(path: string, body?: unknown, opts?: RequestOptions): Promise<T> {
  const res = await axiosInstance.patch<T>(path, body, toAxiosConfig(opts));
  return res.data;
}

export async function del<T>(path: string, opts?: RequestOptions): Promise<T> {
  const res = await axiosInstance.delete<T>(path, toAxiosConfig(opts));
  return res.data;
}

export const apiClient = { get, post, put, patch, del };
