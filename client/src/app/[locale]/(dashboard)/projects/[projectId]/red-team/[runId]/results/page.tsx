'use client';

import { useState, useMemo } from 'react';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useQuery } from '@tanstack/react-query';
import { type ColumnDef } from '@tanstack/react-table';
import {
  ShieldWarningIcon,
  ShieldCheckIcon,
  CircleNotchIcon,
  WarningCircleIcon,
} from '@phosphor-icons/react';
import { motion } from 'motion/react';

import { cn } from '@/lib/utils';
import { PageShell } from '@/components/layout/page-shell';
import { DataTable } from '@/components/data-table/data-table';
import { DataTablePagination } from '@/components/data-table/data-table-pagination';
import { Badge } from '@/components/ui/badge';
import { getRedTeamRun, getRedTeamResults } from '@/lib/api/redteam';
import type { RedTeamRunResponse, RedTeamResultResponse } from '@/lib/api/types';
import { ResultStatsGrid } from '@/components/red-team/result-stats-grid';
import { AttackDetailDrawer, type AttackResultItem } from '@/components/red-team/attack-detail-drawer';

// ---------------------------------------------------------------------------
// Motion variants
// ---------------------------------------------------------------------------

const containerVariants = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.08 } },
};

const itemVariants = {
  hidden: { opacity: 0, y: 12 },
  show: { opacity: 1, y: 0, transition: { type: 'spring' as const, stiffness: 100, damping: 15 } },
};

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type ResultItem = AttackResultItem;

// ---------------------------------------------------------------------------
// Main Component
// ---------------------------------------------------------------------------

export default function RedTeamResultsPage() {
  const t = useTranslations('redTeam');
  const tCommon = useTranslations('common');
  const params = useParams();
  const projectId = params.projectId as string;
  const runId = params.runId as string;

  const [selectedResult, setSelectedResult] = useState<ResultItem | null>(null);

  // Filters state
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'VULNERABLE' | 'SHIELDED' | 'ERROR'>('ALL');
  const [pluginFilter, setPluginFilter] = useState<string>('ALL');

  // Pagination state
  const [page, setPage] = useState(0);
  const pageSize = 10;

  // 1. Fetch red-team run details
  const { data: run, isLoading: runLoading } = useQuery<RedTeamRunResponse>({
    queryKey: ['red-team-run', runId],
    queryFn: () => getRedTeamRun(runId),
  });

  // 2. Fetch red-team results artifact
  const { data: resultsData, isLoading: resultsLoading } = useQuery<RedTeamResultResponse>({
    queryKey: ['red-team-results', runId],
    queryFn: () => getRedTeamResults(runId),
    enabled: run?.status === 'COMPLETED',
  });

  const isLoading = runLoading || resultsLoading;

  // Extract results and stats
  const stats = useMemo(() => {
    return resultsData?.summary ?? { successes: 0, failures: 0, errors: 0 };
  }, [resultsData]);

  const rawResults = useMemo(() => {
    return resultsData?.results?.results ?? [];
  }, [resultsData]);

  const totalTests = stats.successes + stats.failures + stats.errors;
  const vulnerabilityRate = totalTests > 0 ? Math.round((stats.failures / totalTests) * 100) : 0;

  // Clean filter logic — single-pass, no duplicate checks
  const filteredResults = useMemo(() => {
    return rawResults.filter((item) => {
      const isError = item.gradingResult?.componentResults?.some(
        (c) => (c as { graderError?: boolean })?.graderError
      ) ?? false;

      if (statusFilter === 'ERROR' && !isError) return false;
      if (statusFilter === 'VULNERABLE' && (item.success || isError)) return false;
      if (statusFilter === 'SHIELDED' && (!item.success || isError)) return false;
      if (pluginFilter !== 'ALL' && item.pluginId !== pluginFilter) return false;

      return true;
    });
  }, [rawResults, statusFilter, pluginFilter]);

  // Extract unique plugins for filters
  const uniquePlugins = useMemo(() => {
    const plugins = new Set<string>();
    rawResults.forEach((item) => {
      if (item.pluginId) plugins.add(item.pluginId);
    });
    return Array.from(plugins);
  }, [rawResults]);

  // Client-side pagination
  const totalItems = filteredResults.length;
  const totalPages = Math.ceil(totalItems / pageSize);
  const paginatedResults = useMemo(() => {
    const start = page * pageSize;
    const end = start + pageSize;
    return filteredResults.slice(start, end);
  }, [filteredResults, page, pageSize]);

  // Table Columns — CSS variables instead of hardcoded zinc
  const columns = useMemo<ColumnDef<ResultItem, unknown>[]>(
    () => [
      {
        accessorKey: 'pluginId',
        header: t('results.table.threat'),
        size: 150,
        cell: ({ row }) => {
          const plugin = row.original.pluginId;
          const shortLabel = plugin.split(':').pop() || plugin;
          return (
            <Badge variant="outline" className="text-xs uppercase bg-muted border font-mono text-muted-foreground">
              {shortLabel}
            </Badge>
          );
        },
      },
      {
        accessorKey: 'prompt',
        header: t('results.table.prompt'),
        cell: ({ row }) => {
          const promptObj = row.original.prompt;
          const text = typeof promptObj === 'object' ? promptObj.raw : promptObj;
          return (
            <span className="text-foreground line-clamp-1 max-w-[360px] font-mono text-xs">
              {text}
            </span>
          );
        },
      },
      {
        accessorKey: 'response',
        header: t('results.table.response'),
        cell: ({ row }) => {
          const responseObj = row.original.response;
          const text = typeof responseObj === 'object' ? responseObj.raw : responseObj;
          return (
            <span className="text-muted-foreground line-clamp-1 max-w-[320px] text-xs">
              {text}
            </span>
          );
        },
      },
      {
        accessorKey: 'success',
        header: t('results.table.status'),
        size: 120,
        cell: ({ row }) => {
          const item = row.original;
          const isError = item.gradingResult?.componentResults?.some((c) => (c as { graderError?: boolean })?.graderError) || false;
          if (isError) {
            return (
              <Badge className="bg-muted text-muted-foreground border text-[10px] py-0.5 px-2">
                {t('results.table.statusError')}
              </Badge>
            );
          }
          if (item.success) {
            return (
              <Badge className="bg-emerald-500/10 text-emerald-600 dark:text-emerald-500 border border-emerald-500/20 text-[10px] py-0.5 px-2">
                {t('results.table.statusShielded')}
              </Badge>
            );
          }
          return (
            <Badge className="bg-red-500/10 text-red-600 dark:text-red-500 border border-red-500/20 text-[10px] py-0.5 px-2">
              {t('results.table.statusVulnerable')}
            </Badge>
          );
        },
      },
    ],
    [t],
  );

  if (isLoading) {
    return (
      <PageShell title={t('title')} backHref={`/projects/${projectId}/red-team`} backLabel={tCommon('back')}>
        <div className="flex flex-col items-center justify-center py-20 gap-3 text-sm text-muted-foreground">
          <CircleNotchIcon className="size-6 animate-spin" />
          {tCommon('loading')}
        </div>
      </PageShell>
    );
  }

  return (
    <PageShell
      title={run?.name || t('reportTitle', { id: runId.slice(0, 8) })}
      description={t('description')}
      backHref={`/projects/${projectId}/red-team`}
      backLabel={tCommon('back')}
    >
      <motion.div
        variants={containerVariants}
        initial="hidden"
        animate="show"
        className="space-y-6"
      >
        {/* Extracted Stats Grid */}
        <ResultStatsGrid
          stats={stats}
          vulnerabilityRate={vulnerabilityRate}
          totalTests={totalTests}
        />

        {/* Filter Controls Toolbar */}
        <motion.div variants={itemVariants} className="flex flex-wrap items-center justify-between gap-4 p-4 rounded-xl border bg-card">
          {/* Status Segmented Controls — Phosphor icons instead of emoji */}
          <div className="flex gap-1 bg-muted p-1 rounded-lg border">
            {(['ALL', 'VULNERABLE', 'SHIELDED', 'ERROR'] as const).map((mode) => (
              <button
                key={mode}
                onClick={() => {
                  setStatusFilter(mode);
                  setPage(0);
                }}
                className={cn(
                  'px-3 py-1.5 rounded-md text-xs font-semibold transition-all cursor-pointer flex items-center gap-1.5',
                  statusFilter === mode
                    ? 'bg-background text-foreground shadow-sm'
                    : 'text-muted-foreground hover:text-foreground'
                )}
              >
                {mode === 'ALL' && t('results.filterAll')}
                {mode === 'VULNERABLE' && <><ShieldWarningIcon className="size-3.5 text-red-600 dark:text-red-500" /> {t('results.filterVulnerable')}</>}
                {mode === 'SHIELDED' && <><ShieldCheckIcon className="size-3.5 text-emerald-600 dark:text-emerald-500" /> {t('results.filterShielded')}</>}
                {mode === 'ERROR' && <><WarningCircleIcon className="size-3.5 text-yellow-600 dark:text-yellow-500" /> {t('results.filterError')}</>}
              </button>
            ))}
          </div>

          {/* Plugin Category Filter */}
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <span>{t('results.categoryLabel')}:</span>
            <select
              value={pluginFilter}
              onChange={(e) => {
                setPluginFilter(e.target.value);
                setPage(0);
              }}
              className="bg-muted border rounded-lg text-foreground px-2.5 py-1.5 outline-hidden focus:border-ring"
            >
              <option value="ALL">{t('results.categoryAll')}</option>
              {uniquePlugins.map((p) => (
                <option key={p} value={p}>
                  {p.split(':').pop() || p}
                </option>
              ))}
            </select>
          </div>
        </motion.div>

        {/* Attack Vector Data Table */}
        <motion.div variants={itemVariants} className="space-y-4">
          <DataTable
            columns={columns}
            data={paginatedResults}
            totalItems={totalItems}
            pageIndex={page}
            pageSize={pageSize}
            onPaginationChange={(nextPage) => setPage(nextPage)}
            onRowClick={(row) => setSelectedResult(row)}
            emptyMessage={t('results.emptyFiltered')}
          />

          {totalItems > 0 && (
            <DataTablePagination
              pageIndex={page}
              pageSize={pageSize}
              totalItems={totalItems}
              totalPages={totalPages}
              onPageChange={setPage}
            />
          )}
        </motion.div>
      </motion.div>

      {/* Extracted Detail Drawer */}
      <AttackDetailDrawer
        result={selectedResult}
        onClose={() => setSelectedResult(null)}
      />
    </PageShell>
  );
}
