'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useLocale } from 'next-intl';
import { useAuthStore } from '@/lib/store/auth-store';
import { refreshToken, getMe } from '@/lib/api/auth';
import { setTokenGetter, setClearAuth, setOnRefreshed } from '@/lib/api/client';

type AuthGuardProps = {
  children: React.ReactNode;
};

export function AuthGuard({ children }: AuthGuardProps) {
  const router = useRouter();
  const locale = useLocale();
  const { isAuthenticated, login, logout, setToken } = useAuthStore();
  const [status, setStatus] = useState<'loading' | 'authenticated' | 'unauthenticated'>(
    isAuthenticated ? 'authenticated' : 'loading'
  );

  // Wire api client token getter/setter (once)
  useEffect(() => {
    setTokenGetter(() => useAuthStore.getState().accessToken);
    setClearAuth(() => useAuthStore.getState().logout());
    setOnRefreshed((token: string) => useAuthStore.getState().setToken(token));
  }, []);

  // Bootstrap auth on mount
  useEffect(() => {
    if (isAuthenticated) {
      setStatus('authenticated');
      return;
    }

    let cancelled = false;

    async function bootstrap() {
      try {
        // Attempt silent refresh using HttpOnly cookie
        const refreshRes = await refreshToken();
        if (cancelled) return;

        // Store token and fetch user profile
        setToken(refreshRes.accessToken);

        const user = await getMe();
        if (cancelled) return;

        login(refreshRes.accessToken, user);
        setStatus('authenticated');
      } catch {
        if (cancelled) return;
        logout();
        setStatus('unauthenticated');
      }
    }

    bootstrap();
    return () => {
      cancelled = true;
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Redirect when unauthenticated
  useEffect(() => {
    if (status === 'unauthenticated') {
      router.replace(`/${locale}/login`);
    }
  }, [status, router, locale]);

  if (status === 'loading') {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-3">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-muted border-t-primary" />
          <p className="text-sm text-muted-foreground">Loading...</p>
        </div>
      </div>
    );
  }

  if (status === 'unauthenticated') {
    return null;
  }

  return <>{children}</>;
}
