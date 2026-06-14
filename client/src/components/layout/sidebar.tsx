'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useTranslations } from 'next-intl';
import {
  HouseIcon,
  BriefcaseIcon,
  ListChecksIcon,
  GearIcon,
  CaretLeftIcon,
  CaretRightIcon,
  ListIcon,
  XIcon,
} from '@phosphor-icons/react';
import { cn } from '@/lib/utils';
import { useSidebarStore } from '@/lib/store/sidebar-store';
import { Button } from '@/components/ui/button';
import { APP_MONOGRAM, APP_NAME } from '@/lib/branding';

const items = [
  { key: 'dashboard', href: '/dashboard', icon: HouseIcon },
  { key: 'projects', href: '/projects', icon: BriefcaseIcon },
  { key: 'rubrics', href: '/rubrics', icon: ListChecksIcon },
  { key: 'settings', href: '/settings', icon: GearIcon },
] as const;

export function Sidebar() {
  const { isCollapsed, isMobileOpen, toggle, setMobileOpen } = useSidebarStore();
  const pathname = usePathname();
  const tNav = useTranslations('navigation');
  const tSidebar = useTranslations('sidebar');

  const navContent = (
    <>
      {/* Logo */}
      <div className="flex h-16 items-center gap-3 border-b px-4">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-primary">
          <span className="text-xs font-bold text-primary-foreground">
            {APP_MONOGRAM}
          </span>
        </div>
        {!isCollapsed && (
          <span className="text-sm font-semibold leading-tight tracking-tight">
            {APP_NAME}
          </span>
        )}
      </div>

      {/* Navigation items */}
      <nav className="flex-1 space-y-1 p-2">
        {items.map(({ key, href, icon: Icon }) => {
          const isActive = pathname.includes(href);
          return (
            <Link
              key={key}
              href={href}
              title={isCollapsed ? tNav(key) : undefined}
              onClick={() => setMobileOpen(false)}
              className={cn(
                'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                'hover:bg-accent hover:text-accent-foreground',
                isActive && 'bg-accent text-accent-foreground',
                isCollapsed && 'justify-center px-2',
              )}
            >
              <Icon size={20} weight={isActive ? 'fill' : 'regular'} />
              {!isCollapsed && <span>{tNav(key)}</span>}
            </Link>
          );
        })}
      </nav>

      {/* Collapse toggle (desktop only) */}
      <div className="hidden border-t p-2 lg:block">
        <Button
          variant="ghost"
          size="sm"
          onClick={toggle}
          className={cn('w-full', isCollapsed ? 'justify-center' : 'justify-start gap-2')}
          aria-label={isCollapsed ? tSidebar('expand') : tSidebar('collapse')}
        >
          {isCollapsed ? <CaretRightIcon size={16} /> : <CaretLeftIcon size={16} />}
          {!isCollapsed && <span>{tSidebar('collapse')}</span>}
        </Button>
      </div>
    </>
  );

  return (
    <>
      {/* Desktop sidebar */}
      <aside
        className={cn(
          'hidden lg:flex lg:flex-col',
          'min-h-[100dvh] shrink-0 border-r bg-card transition-[width] duration-200',
          isCollapsed ? 'w-16' : 'w-60',
        )}
      >
        {navContent}
      </aside>

      {/* Mobile overlay */}
      {isMobileOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          {/* Backdrop */}
          <div
            className="fixed inset-0 bg-black/50"
            onClick={() => setMobileOpen(false)}
            aria-hidden="true"
          />

          {/* Slide-in panel */}
          <aside className="fixed left-0 top-0 z-50 flex min-h-[100dvh] w-60 flex-col border-r bg-card shadow-lg animate-in slide-in-from-left duration-200">
            {/* Close button */}
            <div className="absolute right-2 top-4">
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setMobileOpen(false)}
                aria-label={tSidebar('collapse')}
              >
                <XIcon size={20} />
              </Button>
            </div>
            {navContent}
          </aside>
        </div>
      )}

      {/* Mobile hamburger button */}
      <Button
        variant="ghost"
        size="icon"
        className="fixed left-3 top-3 z-40 lg:hidden"
        onClick={() => setMobileOpen(true)}
        aria-label={tSidebar('expand')}
      >
        <ListIcon size={24} />
      </Button>
    </>
  );
}
