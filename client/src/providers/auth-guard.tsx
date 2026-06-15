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
  const [progress, setProgress] = useState(0);
  const [targetStatus, setTargetStatus] = useState<'authenticated' | 'unauthenticated' | null>(null);

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
        setTargetStatus('authenticated');
      } catch {
        if (cancelled) return;
        logout();
        setTargetStatus('unauthenticated');
      }
    }

    bootstrap();
    return () => {
      cancelled = true;
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Simulate progress bar
  useEffect(() => {
    if (status !== 'loading') return;

    const timer = setInterval(() => {
      setProgress((prev) => {
        if (prev >= 100) {
          clearInterval(timer);
          return 100;
        }

        // If targetStatus is determined, we speed up to 100%
        if (targetStatus) {
          const next = prev + 15;
          return next >= 100 ? 100 : next;
        }

        // Cap at 90% until api resolves
        if (prev >= 90) {
          return 90;
        }

        const diff = Math.random() * 8 + 4;
        return Math.min(prev + diff, 90);
      });
    }, 80);

    return () => clearInterval(timer);
  }, [status, targetStatus]);

  // Transition status once progress reaches 100% and target is resolved
  useEffect(() => {
    if (progress === 100 && targetStatus) {
      const timeout = setTimeout(() => {
        setStatus(targetStatus);
      }, 150); // small delay to let user see 100%
      return () => clearTimeout(timeout);
    }
  }, [progress, targetStatus]);

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
            <div className="h-1.5 w-40 overflow-hidden rounded-full bg-muted">
              <div 
                className="h-full bg-primary transition-all duration-150 ease-out"
                style={{ width: `${progress}%` }}
              />
            </div>
            <div className="flex justify-between w-40 text-[10px] text-muted-foreground font-mono">
              <span>{t('loading')}</span>
              <span>{Math.round(progress)}%</span>
            </div>
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
