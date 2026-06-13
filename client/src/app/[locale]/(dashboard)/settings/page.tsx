'use client';

import { useTranslations } from 'next-intl';
import { useLocale } from 'next-intl';
import { useTheme } from 'next-themes';
import { useEffect, useState } from 'react';
import {
  User,
  Envelope,
  Shield,
  Clock,
  Translate,
  Sun,
  Moon,
  Desktop,
} from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { PageShell } from '@/components/layout/page-shell';
import { StatusBadge } from '@/components/ui/status-badge';
import { useAuthStore } from '@/lib/store/auth-store';
import type { UserRole } from '@/lib/api/types';
import { usePathname, useRouter } from '@/i18n/navigation';
import type { Locale } from '@/i18n/config';
import { locales } from '@/i18n/config';
import {
  Skeleton,
  SkeletonText,
} from '@/components/feedback/loading-skeleton';

// ---------------------------------------------------------------------------
// Locale label map
// ---------------------------------------------------------------------------

const localeLabels: Record<Locale, string> = {
  vi: 'Tiếng Việt',
  en: 'English',
};

const roleLabels: Record<UserRole, string> = {
  QC_MEMBER: 'QC Member',
  QC_LEAD: 'QC Lead',
  ADMIN: 'Admin',
};

// ---------------------------------------------------------------------------
// Date formatter
// ---------------------------------------------------------------------------

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

// ---------------------------------------------------------------------------
// Theme option type
// ---------------------------------------------------------------------------

type ThemeOption = 'light' | 'dark' | 'system';

const themeOptions: {
  value: ThemeOption;
  icon: React.ElementType;
  labelKey: string;
}[] = [
  { value: 'light', icon: Sun, labelKey: 'themeLight' },
  { value: 'dark', icon: Moon, labelKey: 'themeDark' },
  { value: 'system', icon: Desktop, labelKey: 'themeSystem' },
];

// ---------------------------------------------------------------------------
// Page component
// ---------------------------------------------------------------------------

export default function SettingsPage() {
  const t = useTranslations('settings');
  const tCommon = useTranslations('common');
  const user = useAuthStore((s) => s.user);

  // Theme
  const { theme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);
  useEffect(() => {
    const raf = requestAnimationFrame(() => setMounted(true));
    return () => cancelAnimationFrame(raf);
  }, []);

  // Locale
  const locale = useLocale() as Locale;
  const router = useRouter();
  const pathname = usePathname();

  const handleLocaleChange = (nextLocale: Locale) => {
    router.replace(pathname, { locale: nextLocale });
  };

  return (
    <PageShell title={t('title')}>
      {/* Profile card */}
      <div className="rounded-lg border bg-card p-6">
        <h2 className="mb-4 text-lg font-semibold">{t('profile')}</h2>

        {!user ? (
          <div className="space-y-4">
            {Array.from({ length: 5 }, (_, i) => (
              <div key={i} className="flex items-center gap-3">
                <Skeleton className="h-5 w-5 shrink-0" />
                <div className="flex-1 space-y-1">
                  <SkeletonText width="w-20" />
                  <SkeletonText width="w-40" />
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2">
            {/* Display Name */}
            <div className="flex items-start gap-3">
              <User size={20} className="mt-0.5 shrink-0 text-muted-foreground" />
              <div>
                <p className="text-sm text-muted-foreground">{t('displayName')}</p>
                <p className="font-medium">{user.displayName}</p>
              </div>
            </div>

            {/* Email */}
            <div className="flex items-start gap-3">
              <Envelope size={20} className="mt-0.5 shrink-0 text-muted-foreground" />
              <div>
                <p className="text-sm text-muted-foreground">{t('email')}</p>
                <p className="font-medium">{user.email}</p>
              </div>
            </div>

            {/* Role */}
            <div className="flex items-start gap-3">
              <Shield size={20} className="mt-0.5 shrink-0 text-muted-foreground" />
              <div>
                <p className="text-sm text-muted-foreground">{t('role')}</p>
                <p className="font-medium">{roleLabels[user.role]}</p>
              </div>
            </div>

            {/* Status */}
            <div className="flex items-start gap-3">
              <div className="mt-0.5 h-5 w-5 shrink-0" />
              <div>
                <p className="text-sm text-muted-foreground">{t('status')}</p>
                <StatusBadge status={user.status} size="sm" />
              </div>
            </div>

            {/* Last Login */}
            <div className="flex items-start gap-3 sm:col-span-2">
              <Clock size={20} className="mt-0.5 shrink-0 text-muted-foreground" />
              <div>
                <p className="text-sm text-muted-foreground">{t('lastLogin')}</p>
                <p className="font-medium">
                  {user.lastLoginAt
                    ? formatDateTime(user.lastLoginAt)
                    : tCommon('notAvailable')}
                </p>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Language card */}
      <div className="rounded-lg border bg-card p-6">
        <div className="flex items-center gap-2 mb-4">
          <Translate size={20} className="text-muted-foreground" />
          <h2 className="text-lg font-semibold">{t('language')}</h2>
        </div>
        <div className="flex items-center gap-2">
          {locales.map((l) => (
            <Button
              key={l}
              variant="outline"
              size="sm"
              className={cn(
                locale === l && 'bg-accent text-accent-foreground',
              )}
              onClick={() => handleLocaleChange(l)}
            >
              {localeLabels[l]}
            </Button>
          ))}
        </div>
      </div>

      {/* Theme card */}
      <div className="rounded-lg border bg-card p-6">
        <div className="flex items-center gap-2 mb-4">
          <Sun size={20} className="text-muted-foreground" />
          <h2 className="text-lg font-semibold">{t('theme')}</h2>
        </div>
        <div className="flex items-center gap-2">
          {themeOptions.map((opt) => {
            const Icon = opt.icon;
            return (
              <Button
                key={opt.value}
                variant="outline"
                size="sm"
                disabled={!mounted}
                className={cn(
                  mounted &&
                    theme === opt.value &&
                    'bg-accent text-accent-foreground',
                )}
                onClick={() => setTheme(opt.value)}
              >
                <Icon size={16} className="mr-1.5" />
                {t(opt.labelKey)}
              </Button>
            );
          })}
        </div>
      </div>
    </PageShell>
  );
}
