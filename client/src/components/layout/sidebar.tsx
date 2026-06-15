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
  XIcon,
} from '@phosphor-icons/react';
import { cn } from '@/lib/utils';
import { useSidebarStore } from '@/lib/store/sidebar-store';
import { Button } from '@/components/ui/button';
import Image from 'next/image';
import { motion, AnimatePresence } from 'motion/react';

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
      <div className="flex h-16 items-center justify-center border-b px-4">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg overflow-hidden bg-card border shadow-xs">
          <Image 
            src="/logo.png" 
            alt="Logo" 
            width={32} 
            height={32} 
            className="object-cover" 
          />
        </div>
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
      <AnimatePresence>
        {isMobileOpen && (
          <div className="fixed inset-0 z-50 lg:hidden">
            {/* Backdrop */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.2 }}
              className="fixed inset-0 bg-black/50"
              onClick={() => setMobileOpen(false)}
              aria-hidden="true"
            />

            {/* Slide-in panel */}
            <motion.aside
              initial={{ x: '-100%' }}
              animate={{ x: 0 }}
              exit={{ x: '-100%' }}
              transition={{ type: 'tween', ease: 'easeInOut', duration: 0.25 }}
              className="fixed left-0 top-0 z-50 flex min-h-[100dvh] w-60 flex-col border-r bg-card shadow-lg"
            >
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
            </motion.aside>
          </div>
        )}
      </AnimatePresence>
    </>
  );
}
