'use client';

import { useTranslations } from 'next-intl';
import { motion } from 'motion/react';
import dynamic from 'next/dynamic';

import { cn } from '@/lib/utils';

const RedTeamResultsChart = dynamic(() => import('@/components/red-team/results-chart'), {
  ssr: false,
  loading: () => <div className="h-28 w-28 animate-pulse bg-muted/50 rounded-full" />,
});

// ---------------------------------------------------------------------------
// Motion variants
// ---------------------------------------------------------------------------

const itemVariants = {
  hidden: { opacity: 0, y: 12 },
  show: { opacity: 1, y: 0, transition: { type: 'spring' as const, stiffness: 100, damping: 15 } },
};

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface ResultStatsGridProps {
  stats: { successes: number; failures: number; errors: number };
  vulnerabilityRate: number;
  totalTests: number;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function formatNumber(num: number | undefined): string {
  if (num === undefined) return '0';
  return new Intl.NumberFormat().format(num);
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function ResultStatsGrid({ stats, vulnerabilityRate, totalTests }: ResultStatsGridProps) {
  const t = useTranslations('redTeam');

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {/* Circular Risk Score Gauge */}
      <motion.div variants={itemVariants} className="rounded-xl border bg-card p-5 flex flex-col items-center justify-center relative col-span-1 min-h-[190px]">
        <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2 self-start">
          {t('results.vulnerabilityScore')}
        </span>
        <RedTeamResultsChart
          successes={stats.successes}
          failures={stats.failures}
          errors={stats.errors}
          vulnerabilityRate={vulnerabilityRate}
        />
      </motion.div>

      {/* Attacks Counter */}
      <motion.div variants={itemVariants} className="rounded-xl border bg-card p-5 flex flex-col justify-between min-h-[190px]">
        <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
          {t('results.totalTests')}
        </span>
        <div className="my-auto">
          <span className="text-4xl font-extrabold text-foreground tracking-tight">
            {formatNumber(totalTests)}
          </span>
        </div>
        <div className="text-[10px] text-muted-foreground flex justify-between border-t pt-2 font-mono">
          <span>{t('results.pluginPowered')}</span>
        </div>
      </motion.div>

      {/* Exploited Counter */}
      <motion.div variants={itemVariants} className="rounded-xl border bg-card p-5 flex flex-col justify-between min-h-[190px]">
        <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
          {t('results.exploited')}
        </span>
        <div className="my-auto">
          <span className={cn(
            'text-4xl font-extrabold tracking-tight',
            stats.failures > 0 ? 'text-red-600 dark:text-red-500' : 'text-emerald-600 dark:text-emerald-500'
          )}>
            {formatNumber(stats.failures)}
          </span>
        </div>
        <div className="text-[10px] text-muted-foreground flex justify-between border-t pt-2 font-mono">
          <span>{t('results.exploitedDesc')}</span>
        </div>
      </motion.div>

      {/* Shielded Counter */}
      <motion.div variants={itemVariants} className="rounded-xl border bg-card p-5 flex flex-col justify-between min-h-[190px]">
        <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
          {t('results.shielded')}
        </span>
        <div className="my-auto">
          <span className="text-4xl font-extrabold text-emerald-600 dark:text-emerald-500 tracking-tight">
            {formatNumber(stats.successes)}
          </span>
        </div>
        <div className="text-[10px] text-muted-foreground flex justify-between border-t pt-2 font-mono">
          <span>{t('results.shieldedDesc')}</span>
        </div>
      </motion.div>
    </div>
  );
}
