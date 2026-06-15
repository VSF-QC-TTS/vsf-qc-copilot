'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import Image from 'next/image';
import { motion } from 'motion/react';
import { useAuthStore } from '@/lib/store/auth-store';
import { refreshToken } from '@/lib/api/auth';
import { setTokenGetter, setClearAuth, setOnRefreshed } from '@/lib/api/client';
import { APP_NAME } from '@/lib/branding';

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
      <div className="flex min-h-screen items-center justify-center bg-background select-none">
        <div className="flex flex-col items-center gap-6">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ 
              opacity: [0.7, 1, 0.7],
              scale: [0.98, 1.02, 0.98]
            }}
            transition={{
              duration: 2.5,
              repeat: Infinity,
              ease: "easeInOut"
            }}
            className="flex items-center gap-3"
          >
            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl overflow-hidden shadow-md border bg-card">
              <Image 
                src="/logo.png" 
                alt="Logo" 
                width={48} 
                height={48} 
                priority 
                className="object-cover" 
              />
            </div>
            <span className="text-xl font-bold tracking-tight text-foreground">
              {APP_NAME}
            </span>
          </motion.div>
          <div className="flex flex-col items-center gap-2">
            <div className="h-1 w-36 overflow-hidden rounded-full bg-muted">
              <motion.div
                initial={{ left: "-100%" }}
                animate={{ left: "100%" }}
                transition={{
                  duration: 1.5,
                  repeat: Infinity,
                  ease: "easeInOut",
                }}
                className="relative h-full w-full bg-primary"
              />
            </div>
            <p className="text-xs font-medium tracking-wide text-muted-foreground animate-pulse">
              {t('loading')}
            </p>
          </div>
        </div>
      </div>
    );
  }

  if (status === 'unauthenticated') {
    return null;
  }

  return <>{children}</>;
}
