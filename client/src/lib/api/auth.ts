import { apiClient } from "@/lib/api/client";
import type {
  LoginRequest,
  LoginResponse,
  UserResponse,
} from "@/lib/api/types";

const publicAuthRequest = {
  auth: "none",
  _skipRefresh: true,
} as const;

/** POST /api/v1/auth/login */
export async function loginUser(data: LoginRequest): Promise<LoginResponse> {
  return apiClient.post<LoginResponse>(
    "/api/v1/auth/login",
    data,
    publicAuthRequest,
  );
}

/** POST /api/v1/auth/register */
export async function registerUser(data: {
  email: string;
  password: string;
  fullName: string;
}): Promise<UserResponse> {
  return apiClient.post<UserResponse>(
    "/api/v1/auth/register",
    data,
    publicAuthRequest,
  );
}

/** POST /api/v1/auth/verify-email */
export async function verifyEmail(token: string): Promise<UserResponse> {
  return apiClient.post<UserResponse>(
    "/api/v1/auth/verify-email",
    { token },
    publicAuthRequest,
  );
}

/** POST /api/v1/auth/forgot-password — always 204 */
export async function forgotPassword(email: string): Promise<void> {
  return apiClient.post<void>(
    "/api/v1/auth/forgot-password",
    { email },
    publicAuthRequest,
  );
}

/** POST /api/v1/auth/reset-password */
export async function resetPassword(data: {
  token: string;
  newPassword: string;
}): Promise<void> {
  return apiClient.post<void>(
    "/api/v1/auth/reset-password",
    data,
    publicAuthRequest,
  );
}

/** POST /api/v1/auth/refresh-token — uses HttpOnly cookie */
export async function refreshToken(): Promise<LoginResponse> {
  return apiClient.post<LoginResponse>(
    "/api/v1/auth/refresh-token",
    undefined,
    publicAuthRequest,
  );
}

/** GET /api/v1/users/me */
export async function getMe(): Promise<UserResponse> {
  return apiClient.get<UserResponse>("/api/v1/users/me");
}

/** POST /api/v1/auth/logout */
export async function logoutUser(): Promise<void> {
  return apiClient.post<void>(
    "/api/v1/auth/logout",
    undefined,
    publicAuthRequest,
  );
}
