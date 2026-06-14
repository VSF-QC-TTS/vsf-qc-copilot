'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useAuthStore } from '@/lib/store/auth-store';
import { refreshToken } from '@/lib/api/auth';
import { setTokenGetter, setClearAuth, setOnRefreshed } from '@/lib/api/client';

type AuthGuardProps = {
  children: React.ReactNode;
};

export function AuthGuard({ children }: AuthGuardProps) {
  const t = useTranslations('common');
  const router = useRouter();
  const { isAuthenticated, login, logout } = useAuthStore();
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
      return;
    }

    let cancelled = false;

    async function bootstrap() {
      try {
        // Attempt silent refresh using HttpOnly cookie
        const refreshRes = await refreshToken();
        if (cancelled) return;

        login(refreshRes.accessToken, refreshRes.user);
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
      router.replace('/login');
    }
  }, [status, router]);

  if (status === 'loading') {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-3">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-muted border-t-primary" />
          <p className="text-sm text-muted-foreground">{t('loading')}</p>
        </div>
      </div>
    );
  }

  if (status === 'unauthenticated') {
    return null;
  }

  return <>{children}</>;
}
