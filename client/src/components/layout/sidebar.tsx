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
  const [ignoreHover, setIgnoreHover] = useState(false);
  const pathname = usePathname();
  const tNav = useTranslations('navigation');
  const tSidebar = useTranslations('sidebar');

  const isExpanded = !isCollapsed || (isHovered && !ignoreHover);

  const handleToggle = (e: React.MouseEvent) => {
    e.stopPropagation(); // Avoid triggering hover events
    if (!isCollapsed) {
      setIgnoreHover(true);
    }
    toggle();
  };

  const handleMouseLeave = () => {
    setIsHovered(false);
    setIgnoreHover(false);
  };

  const renderNavContent = (expanded: boolean) => (
    <>
      {/* Logo */}
      <div className="flex h-16 items-center border-b px-3 gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg overflow-hidden bg-card border shadow-xs">
          <Image 
            src="/logo.png" 
            alt="Logo" 
            width={40} 
            height={40} 
            className="object-cover" 
          />
        </div>
        <span className={cn(
          'font-bold text-sm text-foreground truncate transition-[opacity,width] duration-300 ease-in-out',
          expanded ? 'opacity-100 w-auto' : 'lg:opacity-0 lg:w-0 lg:overflow-hidden opacity-100 w-auto'
        )}>
          VF QC Copilot
        </span>
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
                'flex w-full items-center rounded-md font-medium transition-colors relative group whitespace-nowrap h-10',
                'hover:bg-accent hover:text-accent-foreground',
                isActive ? 'bg-accent text-accent-foreground' : 'text-muted-foreground',
              )}
            >
              <span className="w-12 h-10 flex items-center justify-center flex-shrink-0">
                <Icon size={20} weight={isActive ? 'fill' : 'regular'} />
              </span>
              <span className={cn(
                'truncate transition-[opacity,width] duration-300 ease-in-out',
                expanded ? 'opacity-100 w-auto pr-3' : 'opacity-0 w-0 overflow-hidden pr-0'
              )}>
                {tNav(key)}
              </span>
            </Link>
          );
        })}
      </nav>
    </>
  );

  return (
    <>
      {/* Desktop sidebar */}
      <aside
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={handleMouseLeave}
        className={cn(
          'hidden lg:flex lg:flex-col overflow-visible group',
          'fixed inset-y-0 left-0 z-40',
          'h-[100dvh] shrink-0 border-r bg-card transition-[width,box-shadow] duration-300 ease-in-out',
          isExpanded ? 'w-60 shadow-lg' : 'w-16',
        )}
      >
        <div className="w-full h-full flex flex-col overflow-hidden">
          {renderNavContent(isExpanded)}
        </div>

        {/* Floating edge toggle button (only visible on hover) */}
        <button
          onClick={handleToggle}
          className="hidden lg:flex absolute top-5 -right-2.5 z-50 h-5 w-5 items-center justify-center rounded-full border bg-card text-muted-foreground shadow-xs hover:bg-accent hover:text-foreground transition-all duration-200 cursor-pointer opacity-0 group-hover:opacity-100"
          aria-label={isCollapsed ? tSidebar('expand') : tSidebar('collapse')}
        >
          {isCollapsed ? <CaretRightIcon size={10} weight="bold" /> : <CaretLeftIcon size={10} weight="bold" />}
        </button>
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
              <div className="w-full h-full flex flex-col overflow-hidden">
                {renderNavContent(true)}
              </div>
            </motion.aside>
          </div>
        )}
      </AnimatePresence>
    </>
  );
}
