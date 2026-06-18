'use client';

import { useState, useMemo } from 'react';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useQuery } from '@tanstack/react-query';
import { type ColumnDef } from '@tanstack/react-table';
import { PlusIcon } from '@phosphor-icons/react';
import { motion } from 'motion/react';

import { Button } from '@/components/ui/button';
import { PageShell } from '@/components/layout/page-shell';
import { DataTable } from '@/components/data-table/data-table';
import { DataTablePagination } from '@/components/data-table/data-table-pagination';
import { StatusBadge } from '@/components/ui/status-badge';
import { Badge } from '@/components/ui/badge';
import { listRedTeamRuns } from '@/lib/api/redteam';
import type { RedTeamRunResponse } from '@/lib/api/types';
import { useRouter } from '@/i18n/navigation';
import { CreateRedTeamRunDialog } from '@/components/red-team/create-run-dialog';

// ---------------------------------------------------------------------------
// Motion Variants
// ---------------------------------------------------------------------------

const containerVariants = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.1 } },
};

const itemVariants = {
  hidden: { opacity: 0, y: 15 },
  show: { opacity: 1, y: 0, transition: { type: 'spring' as const, stiffness: 100, damping: 15 } },
};

const PAGE_SIZE = 10;

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

// ---------------------------------------------------------------------------
// Page component
// ---------------------------------------------------------------------------

export default function RedTeamRunsPage() {
  const t = useTranslations('redTeam');
  const tCommon = useTranslations('common');
  const router = useRouter();
  const params = useParams();
  const projectId = params.projectId as string;

  const [page, setPage] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);

  // Fetch red-team runs
  const { data, isLoading } = useQuery({
    queryKey: ['red-team-runs', projectId, { page, size: PAGE_SIZE }],
    queryFn: () => listRedTeamRuns(projectId, page, PAGE_SIZE),
  });

  const runs = data?.items ?? [];
  const totalItems = data?.totalItems ?? 0;
  const totalPages = data?.totalPages ?? 0;

  // Columns
  const columns = useMemo<ColumnDef<RedTeamRunResponse, unknown>[]>(
    () => [
      {
        accessorKey: 'name',
        header: t('scanName'),
        cell: ({ row }) => {
          const runNumber = totalItems - (page * PAGE_SIZE) - row.index;
          const displayName = row.original.name || t('defaultScanName', { number: runNumber });
          return (
            <div className="flex flex-col gap-0.5">
              <span className="font-medium text-foreground tracking-tight">
                {displayName}
              </span>
              <span className="text-[11px] text-muted-foreground font-mono">
                {row.original.publicId.slice(0, 8)}
              </span>
            </div>
          );
        },
      },
      {
        accessorKey: 'status',
        header: t('results.table.status'),
        size: 130,
        cell: ({ row }) => (
          <StatusBadge status={row.original.status} size="sm" />
        ),
      },
      {
        accessorKey: 'connectorName',
        header: t('fields.connector'),
        cell: ({ row }) => (
          <span className="text-muted-foreground font-medium text-[13px] tracking-tight">
            {row.original.connectorName ?? tCommon('notAvailable')}
          </span>
        ),
      },
      {
        accessorKey: 'plugins',
        header: t('fields.plugins'),
        cell: ({ row }) => {
          const plugins = row.original.plugins ?? [];
          return (
            <div className="flex flex-wrap gap-1.5 max-w-[240px]">
              {plugins.slice(0, 2).map((p) => {
                const shortLabel = p.split(':').pop() || p;
                return (
                  <Badge key={p} variant="outline" className="text-[10px] px-1.5 py-0 font-medium tracking-tight bg-muted/40 uppercase">
                    {shortLabel}
                  </Badge>
                );
              })}
              {plugins.length > 2 && (
                <Badge variant="outline" className="text-[10px] px-1.5 py-0 font-medium bg-muted/20">
                  +{plugins.length - 2}
                </Badge>
              )}
            </div>
          );
        },
      },
      {
        accessorKey: 'vulnerabilities',
        header: t('results.exploited'),
        size: 140,
        cell: ({ row }) => {
          const run = row.original;
          if (run.status !== 'COMPLETED') {
            return <span className="text-muted-foreground text-xs">—</span>;
          }
          const total = run.totalCases ?? 0;
          const failed = run.failedCases ?? 0;
          const hasVulns = failed > 0;

          return (
            <div className="flex items-center gap-2">
              <span className={hasVulns ? 'text-rose-500/90 font-medium font-mono text-xs' : 'text-emerald-500/80 font-medium font-mono text-xs'}>
                {failed} / {total}
              </span>
              {hasVulns ? (
                <span className="text-[10px] px-1.5 py-0.5 rounded-sm bg-rose-500/10 border border-rose-500/20 text-rose-600/90 dark:text-rose-400/90 font-medium tracking-tight">
                  Vuln
                </span>
              ) : (
                <span className="text-[10px] px-1.5 py-0.5 rounded-sm bg-emerald-500/10 border border-emerald-500/20 text-emerald-600/80 dark:text-emerald-500/80 font-medium tracking-tight">
                  Safe
                </span>
              )}
            </div>
          );
        },
      },
      {
        accessorKey: 'createdAt',
        header: tCommon('createdAt'),
        cell: ({ row }) => (
          <span className="text-muted-foreground text-[11px] font-mono tracking-tight">
            {formatDate(row.original.createdAt)}
          </span>
        ),
      },
    ],
    [t, tCommon, totalItems, page],
  );

  const handleRowClick = (row: RedTeamRunResponse) => {
    if (row.status === 'COMPLETED') {
      router.push(`/projects/${projectId}/red-team/${row.publicId}/results`);
    } else {
      router.push(`/projects/${projectId}/red-team/${row.publicId}`);
    }
  };

  return (
    <PageShell
      title={t('title')}
      description={t('description')}
      backHref={`/projects/${projectId}`}
      backLabel={tCommon('back')}
      actions={
        <Button onClick={() => setDialogOpen(true)} className="shadow-xs">
          <PlusIcon weight="bold" className="mr-1.5 size-4" />
          {t('startScan')}
        </Button>
      }
    >
      <motion.div
        variants={containerVariants}
        initial="hidden"
        animate="show"
        className="space-y-4"
      >
        <motion.div variants={itemVariants}>
          <DataTable
            columns={columns}
            data={runs}
            totalItems={totalItems}
            pageIndex={page}
            pageSize={PAGE_SIZE}
            onPaginationChange={(nextPage) => setPage(nextPage)}
            loading={isLoading}
            onRowClick={handleRowClick}
            emptyMessage={t('description')}
            emptyAction={
              <Button onClick={() => setDialogOpen(true)} variant="outline">
                <PlusIcon weight="bold" className="mr-1.5 size-4" />
                {t('startScan')}
              </Button>
            }
          />
        </motion.div>

        {totalItems > 0 && (
          <motion.div variants={itemVariants}>
            <DataTablePagination
              pageIndex={page}
              pageSize={PAGE_SIZE}
              totalItems={totalItems}
              totalPages={totalPages}
              onPageChange={setPage}
            />
          </motion.div>
        )}
      </motion.div>

      <CreateRedTeamRunDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        projectId={projectId}
      />
    </PageShell>
  );
}
