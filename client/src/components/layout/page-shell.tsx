'use client';

import { ArrowLeftIcon } from '@phosphor-icons/react';
import { motion, useReducedMotion, type Variants } from 'motion/react';

import { Link } from '@/i18n/navigation';
import { cn } from '@/lib/utils';

interface PageShellProps {
  title: string;
  description?: string;
  backHref?: string;
  backLabel?: string;
  actions?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
}

const shellVariants: Variants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: {
      staggerChildren: 0.08,
      delayChildren: 0.03,
    },
  },
};

const headerVariants: Variants = {
  hidden: { opacity: 0, y: 12 },
  show: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.28, ease: 'easeOut' },
  },
};

const contentVariants: Variants = {
  hidden: { opacity: 0, y: 16 },
  show: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.36, ease: 'easeOut' },
  },
};

export function PageShell({
  title,
  description,
  backHref,
  backLabel = 'Back',
  actions,
  children,
  className,
}: PageShellProps) {
  const shouldReduceMotion = useReducedMotion();

  return (
    <motion.section
      className={cn('space-y-6', className)}
      variants={shellVariants}
      initial={shouldReduceMotion ? false : 'hidden'}
      animate="show"
    >
      {backHref && (
        <motion.div variants={headerVariants}>
          <Link
            href={backHref}
            className="inline-flex w-fit items-center gap-1.5 rounded-md px-1 py-1 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
          >
            <ArrowLeftIcon className="size-4" weight="bold" />
            {backLabel}
          </Link>
        </motion.div>
      )}
      <motion.div
        className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between"
        variants={headerVariants}
      >
        <div className="min-w-0 space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
          {description && (
            <p className="max-w-3xl text-sm text-muted-foreground">{description}</p>
          )}
        </div>
        {actions && (
          <div className="flex w-full flex-wrap items-center gap-2 sm:w-auto sm:justify-end">
            {actions}
          </div>
        )}
      </motion.div>
      <motion.div className="space-y-6" variants={contentVariants}>
        {children}
      </motion.div>
    </motion.section>
  );
}
