'use client';

import { useState, useRef, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { SignOutIcon, GearSixIcon, ListIcon } from '@phosphor-icons/react';
import Link from 'next/link';
import { cn } from '@/lib/utils';
import { useAuthStore } from '@/lib/store/auth-store';
import { useSidebarStore } from '@/lib/store/sidebar-store';
import { logoutUser } from '@/lib/api/auth';
import { Button } from '@/components/ui/button';
import { LanguageSwitcher } from '@/components/ui/language-switcher';
import { ThemeToggle } from '@/components/ui/theme-toggle';

function getInitials(name: string): string {
  return name
    .split(' ')
    .map((part) => part[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
}

export function Header() {
  const t = useTranslations('header');
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false);
      }
    }
    if (menuOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [menuOpen]);

  async function handleLogout() {
    try {
      await logoutUser();
    } catch {
      // Continue logout even if API call fails
    }
    useAuthStore.getState().logout();
    router.push('/login');
  }

  const initials = user?.displayName ? getInitials(user.displayName) : '?';

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center border-b bg-background/95 backdrop-blur">
      {/* Mobile hamburger */}
      <Button
        variant="ghost"
        size="icon"
        className="ml-2 lg:hidden"
        onClick={() => useSidebarStore.getState().setMobileOpen(true)}
        aria-label={t('menu')}
      >
        <ListIcon size={24} />
      </Button>

      {/* Breadcrumb placeholder */}
      <div className="flex-1 px-4">
        {/* Breadcrumb will be implemented later */}
      </div>

      {/* Right side controls */}
      <div className="flex items-center gap-1 pr-4">
        <LanguageSwitcher />
        <ThemeToggle />

        {/* User menu */}
        <div className="relative" ref={menuRef}>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setMenuOpen((v) => !v)}
            aria-label={t('userMenu')}
            className="ml-1"
          >
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-xs font-semibold text-primary-foreground">
              {initials}
            </div>
          </Button>

          {menuOpen && (
            <div
              className={cn(
                'absolute right-0 top-full mt-1 w-56 rounded-md border bg-popover p-1 shadow-md',
                'animate-in fade-in-0 zoom-in-95',
              )}
            >
              {/* User info */}
              <div className="px-3 py-2">
                <p className="text-sm font-medium">{user?.displayName ?? ''}</p>
                <p className="text-xs text-muted-foreground">{user?.email ?? ''}</p>
                {user?.role && (
                  <p className="mt-0.5 text-xs text-muted-foreground">{user.role}</p>
                )}
              </div>

              <div className="my-1 h-px bg-border" />

              {/* Settings link */}
              <Link
                href="/settings"
                onClick={() => setMenuOpen(false)}
                className="flex items-center gap-2 rounded-sm px-3 py-2 text-sm hover:bg-accent hover:text-accent-foreground"
              >
                <GearSixIcon size={16} />
                {t('settings')}
              </Link>

              <div className="my-1 h-px bg-border" />

              {/* Logout */}
              <button
                onClick={handleLogout}
                className="flex w-full items-center gap-2 rounded-sm px-3 py-2 text-sm text-destructive hover:bg-destructive/10"
              >
                <SignOutIcon size={16} />
                {t('logout')}
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
