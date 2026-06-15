'use client';

import { useState } from 'react';
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
  const [isHovered, setIsHovered] = useState(false);
  const pathname = usePathname();
  const tNav = useTranslations('navigation');
  const tSidebar = useTranslations('sidebar');

  const isExpanded = !isCollapsed || isHovered;

  const renderNavContent = (expanded: boolean) => (
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
              title={!expanded ? tNav(key) : undefined}
              onClick={() => setMobileOpen(false)}
              className={cn(
                'flex items-center rounded-md font-medium transition-colors relative group whitespace-nowrap',
                'hover:bg-accent hover:text-accent-foreground',
                isActive ? 'bg-accent text-accent-foreground' : 'text-muted-foreground',
                expanded ? 'px-3 py-2 gap-3' : 'justify-center p-2',
              )}
            >
              <span className="flex h-5 w-5 shrink-0 items-center justify-center">
                <Icon size={20} weight={isActive ? 'fill' : 'regular'} />
              </span>
              <span className={cn(
                'truncate transition-all duration-300',
                expanded ? 'opacity-100 w-auto' : 'lg:opacity-0 lg:w-0 lg:overflow-hidden opacity-100 w-auto'
              )}>
                {tNav(key)}
              </span>
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
          className={cn('w-full', !expanded ? 'justify-center' : 'justify-start gap-2')}
          aria-label={isCollapsed ? tSidebar('expand') : tSidebar('collapse')}
        >
          {isCollapsed ? <CaretRightIcon size={16} /> : <CaretLeftIcon size={16} />}
          {expanded && <span>{tSidebar('collapse')}</span>}
        </Button>
      </div>
    </>
  );

  return (
    <>
      {/* Desktop sidebar */}
      <aside
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
        className={cn(
          'hidden lg:flex lg:flex-col',
          'absolute inset-y-0 left-0 z-40',
          'min-h-[100dvh] shrink-0 border-r bg-card transition-[width,box-shadow] duration-300 ease-in-out',
          isExpanded ? 'w-60 shadow-lg' : 'w-16',
        )}
      >
        {renderNavContent(isExpanded)}
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
              {renderNavContent(true)}
            </motion.aside>
          </div>
        )}
      </AnimatePresence>
    </>
  );
}
