'use client';

import { useTranslations } from 'next-intl';
import { useLocale } from 'next-intl';
import { useTheme } from 'next-themes';
import { useEffect, useState } from 'react';
import {
  EnvelopeIcon,
  ShieldIcon,
  ClockIcon,
  TranslateIcon,
  SunIcon,
  MoonIcon,
  DesktopIcon,
  PaletteIcon,
} from '@phosphor-icons/react';
import { motion } from 'motion/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { PageShell } from '@/components/layout/page-shell';
import { StatusBadge } from '@/components/ui/status-badge';
import { UserAvatar } from '@/components/ui/user-avatar';
import { useAuthStore } from '@/lib/store/auth-store';
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

const localeLabels: Record<Locale, { short: string; full: string }> = {
  vi: { short: 'VI', full: 'Tiếng Việt' },
  en: { short: 'EN', full: 'English' },
};

// ---------------------------------------------------------------------------
// Date formatter
// ---------------------------------------------------------------------------

function formatDateTime(iso: string, locale: string): string {
  return new Date(iso).toLocaleString(locale === 'vi' ? 'vi-VN' : 'en-US', {
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
  { value: 'light', icon: SunIcon, labelKey: 'themeLight' },
  { value: 'dark', icon: MoonIcon, labelKey: 'themeDark' },
  { value: 'system', icon: DesktopIcon, labelKey: 'themeSystem' },
];

// ---------------------------------------------------------------------------
// Stagger animation variants
// ---------------------------------------------------------------------------

const containerVariants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: { staggerChildren: 0.08 },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 12 },
  show: {
    opacity: 1,
    y: 0,
    transition: { type: 'spring' as const, stiffness: 120, damping: 18 },
  },
};

// ---------------------------------------------------------------------------
// Page component
// ---------------------------------------------------------------------------

export default function SettingsPage() {
  const t = useTranslations('settings');
  const tCommon = useTranslations('common');
  const tRoles = useTranslations('roles');
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
    // eslint-disable-next-line react-hooks/immutability
    document.cookie = `NEXT_LOCALE=${nextLocale}; path=/; max-age=31536000`;
    router.replace(pathname, { locale: nextLocale });
  };

  // ---------------------------------------------------------------------------
  // Profile info items
  // ---------------------------------------------------------------------------

  const profileItems = user
    ? [
        {
          icon: EnvelopeIcon,
          label: t('email'),
          value: user.email,
        },
        {
          icon: ShieldIcon,
          label: t('role'),
          value: tRoles(user.role),
        },
        {
          icon: ClockIcon,
          label: t('lastLogin'),
          value: user.lastLoginAt
            ? formatDateTime(user.lastLoginAt, locale)
            : tCommon('notAvailable'),
        },
      ]
    : [];

  return (
    <PageShell title={t('title')} description={t('description')}>
      <motion.div
        variants={containerVariants}
        initial="hidden"
        animate="show"
        className="space-y-6"
      >
        {/* ----------------------------------------------------------------- */}
        {/* Profile Section                                                    */}
        {/* ----------------------------------------------------------------- */}
        <motion.div variants={itemVariants} className="rounded-lg border bg-card p-6">
          <h2 className="mb-5 text-lg font-semibold">{t('profile')}</h2>

          {!user ? (
            <div className="flex items-center gap-5">
              <Skeleton className="h-24 w-24 rounded-full" />
              <div className="flex-1 space-y-2">
                <SkeletonText width="w-40" />
                <SkeletonText width="w-56" />
                <SkeletonText width="w-32" />
              </div>
            </div>
          ) : (
            <div className="flex flex-col sm:flex-row items-start gap-6">
              {/* Avatar */}
              <UserAvatar
                displayName={user.displayName}
                avatarUrl={user.avatarUrl}
                size="lg"
              />

              {/* Info */}
              <div className="flex-1 min-w-0 space-y-1">
                <h3 className="text-xl font-semibold truncate">{user.displayName}</h3>
                <div className="flex items-center gap-2">
                  <StatusBadge status={user.status} size="sm" />
                </div>

                <div className="pt-3 space-y-2.5">
                  {profileItems.map((item) => (
                    <div key={item.label} className="flex items-center gap-2.5 text-sm">
                      <item.icon size={16} className="shrink-0 text-muted-foreground" />
                      <span className="text-muted-foreground">{item.label}:</span>
                      <span className="font-medium truncate">{item.value}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </motion.div>

        {/* ----------------------------------------------------------------- */}
        {/* Appearance Section                                                 */}
        {/* ----------------------------------------------------------------- */}
        <motion.div variants={itemVariants} className="rounded-lg border bg-card p-6">
          <div className="flex items-center gap-2 mb-4">
            <PaletteIcon size={20} className="text-muted-foreground" />
            <h2 className="text-lg font-semibold">{t('theme')}</h2>
          </div>
          <div className="flex flex-wrap items-center gap-2">
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
        </motion.div>

        {/* ----------------------------------------------------------------- */}
        {/* Language Section                                                    */}
        {/* ----------------------------------------------------------------- */}
        <motion.div variants={itemVariants} className="rounded-lg border bg-card p-6">
          <div className="flex items-center gap-2 mb-4">
            <TranslateIcon size={20} className="text-muted-foreground" />
            <h2 className="text-lg font-semibold">{t('language')}</h2>
          </div>
          <div className="flex flex-wrap items-center gap-2">
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
                <span className="font-semibold mr-1.5">{localeLabels[l].short}</span>
                {localeLabels[l].full}
              </Button>
            ))}
          </div>
        </motion.div>
      </motion.div>
    </PageShell>
  );
}
