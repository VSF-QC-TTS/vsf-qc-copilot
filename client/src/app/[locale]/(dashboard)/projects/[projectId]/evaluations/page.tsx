'use client';

import { useState, useMemo } from 'react';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useQuery } from '@tanstack/react-query';
import { type ColumnDef } from '@tanstack/react-table';
import { PlusIcon } from '@phosphor-icons/react';

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
// Row type
// ---------------------------------------------------------------------------

type EvaluationRunRow = {
  publicId: string;
  name: string | null;
  status: string;
  datasetName: string | null;
  rubricName: string | null;
  progress: number | null;
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
        cell: ({ row }) => (
          <span className="font-medium">
            {row.original.name ?? row.original.publicId}
          </span>
        ),
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
        size: 100,
        cell: ({ row }) => {
          const progress = row.original.progress;
          return (
            <span className="text-muted-foreground">
              {progress !== null && progress !== undefined
                ? `${progress}%`
                : tCommon('notAvailable')}
            </span>
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
    [t, tCommon],
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

      {totalItems > 0 && (
        <DataTablePagination
          pageIndex={page}
          pageSize={PAGE_SIZE}
          totalItems={totalItems}
          totalPages={totalPages}
          onPageChange={setPage}
        />
      )}

      <StartEvaluationDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        projectId={projectId}
      />
    </PageShell>
  );
}
