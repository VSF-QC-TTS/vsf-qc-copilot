"use client";

import { useEffect } from "react";
import { useAuthStore } from "@/lib/store/auth-store";
import { setTokenGetter, setClearAuth, setOnRefreshed } from "@/lib/api/client";

/**
 * Wires the auth store with the API client on mount.
 * This must be rendered inside the provider tree (after QueryProvider).
 */
export function AuthBootstrap({ children }: { children: React.ReactNode }) {
  useEffect(() => {
    // Wire token getter so API client can attach Authorization headers
    setTokenGetter(() => useAuthStore.getState().accessToken);

    // Wire logout so API client can clear auth on refresh failure
    setClearAuth(() => useAuthStore.getState().logout());

    // Wire token update so API client can persist refreshed tokens
    setOnRefreshed((token: string) => useAuthStore.getState().setToken(token));
  }, []);

  return <>{children}</>;
}
