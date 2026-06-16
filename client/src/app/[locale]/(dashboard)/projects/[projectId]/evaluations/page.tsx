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
import { StartEvaluationDialog } from '@/components/evaluations/start-evaluation-dialog';
import { apiClient } from '@/lib/api/client';
import type { PageResponse } from '@/lib/api/types';
import { useRouter } from '@/i18n/navigation';

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

// ---------------------------------------------------------------------------
// Row type
// ---------------------------------------------------------------------------

type EvaluationRunRow = {
  publicId: string;
  datasetPublicId: string;
  datasetName: string | null;
  rubricVersionPublicId: string;
  rubricName: string | null;
  targetApiConnectorPublicId: string;
  judgeModelPublicId: string | null;
  status: string;
  totalCases: number;
  completedCases: number;
  passedCases: number;
  failedCases: number;
  createdAt: string;
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
  });
}

// ---------------------------------------------------------------------------
// Page component
// ---------------------------------------------------------------------------

export default function EvaluationsPage() {
  const t = useTranslations('evaluations');
  const tCommon = useTranslations('common');
  const router = useRouter();
  const params = useParams();
  const projectId = params.projectId as string;

  const [page, setPage] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);

  // Fetch evaluation runs
  const { data, isLoading } = useQuery({
    queryKey: ['evaluation-runs', projectId, { page, size: PAGE_SIZE }],
    queryFn: () =>
      apiClient.get<PageResponse<EvaluationRunRow>>(
        `/api/v1/projects/${projectId}/evaluation-runs?page=${page}&size=${PAGE_SIZE}&sort=createdAt,desc`,
      ),
  });

  const runs = data?.items ?? [];
  const totalItems = data?.totalItems ?? 0;
  const totalPages = data?.totalPages ?? 0;

  // Columns
  const columns = useMemo<ColumnDef<EvaluationRunRow, unknown>[]>(
    () => [
      {
        accessorKey: 'name',
        header: t('columns.name'),
        cell: ({ row }) => {
          const runNumber = totalItems - (page * PAGE_SIZE) - row.index;
          return (
            <div className="flex flex-col">
              <span className="font-medium text-foreground">
                {t('runNumber', { number: runNumber })}
              </span>
              <span className="text-[10px] text-muted-foreground font-mono">
                {row.original.publicId.slice(0, 8)}
              </span>
            </div>
          );
        },
      },
      {
        accessorKey: 'status',
        header: t('columns.status'),
        size: 130,
        cell: ({ row }) => (
          <StatusBadge status={row.original.status} size="sm" />
        ),
      },
      {
        accessorKey: 'datasetName',
        header: t('columns.dataset'),
        cell: ({ row }) => (
          <span className="text-muted-foreground">
            {row.original.datasetName ?? tCommon('notAvailable')}
          </span>
        ),
      },
      {
        accessorKey: 'rubricName',
        header: t('columns.rubric'),
        cell: ({ row }) => (
          <span className="text-muted-foreground">
            {row.original.rubricName ?? tCommon('notAvailable')}
          </span>
        ),
      },
      {
        accessorKey: 'progress',
        header: t('columns.progress'),
        size: 140,
        cell: ({ row }) => {
          const total = row.original.totalCases;
          const completed = row.original.completedCases;
          const pct = total > 0 ? Math.round((completed / total) * 100) : 0;
          return (
            <div className="flex flex-col gap-1.5 w-[100px]">
              <div className="flex items-center justify-between text-xs">
                <span className="text-muted-foreground">{completed}/{total}</span>
                <span className="font-medium text-foreground">{pct}%</span>
              </div>
              <div className="h-1.5 w-full bg-secondary overflow-hidden rounded-full">
                <motion.div 
                  className="h-full bg-primary" 
                  initial={{ width: 0 }}
                  animate={{ width: `${pct}%` }}
                  transition={{ duration: 1, ease: 'easeOut' }}
                />
              </div>
            </div>
          );
        },
      },
      {
        accessorKey: 'createdAt',
        header: t('columns.createdAt'),
        size: 140,
        cell: ({ row }) => (
          <span className="text-muted-foreground">
            {formatDate(row.original.createdAt)}
          </span>
        ),
      },
    ],
    [t, tCommon, totalItems, page],
  );

  const handleRowClick = (row: EvaluationRunRow) => {
    router.push(`/projects/${projectId}/evaluations/${row.publicId}`);
  };

  return (
    <PageShell
      title={t('title')}
      backHref={`/projects/${projectId}`}
      backLabel={tCommon('back')}
      actions={
        <Button onClick={() => setDialogOpen(true)}>
          <PlusIcon weight="bold" />
          {t('startEvaluation')}
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
            emptyMessage={t('noEvaluations')}
            emptyAction={
              <Button onClick={() => setDialogOpen(true)}>
                <PlusIcon weight="bold" />
                {t('startEvaluation')}
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

      <StartEvaluationDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        projectId={projectId}
      />
    </PageShell>
  );
}
