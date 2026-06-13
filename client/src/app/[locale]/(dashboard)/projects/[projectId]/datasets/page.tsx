'use client';

import { useState, useMemo } from 'react';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useQuery } from '@tanstack/react-query';
import { type ColumnDef } from '@tanstack/react-table';
import { Plus, Database } from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { PageShell } from '@/components/layout/page-shell';
import { DataTable } from '@/components/data-table/data-table';
import { DataTablePagination } from '@/components/data-table/data-table-pagination';
import { StatusBadge } from '@/components/ui/status-badge';
import { CreateDatasetDialog } from '@/components/datasets/create-dataset-dialog';
import { apiClient } from '@/lib/api/client';
import type { PageResponse, DatasetStatus } from '@/lib/api/types';
import { useRouter } from '@/i18n/navigation';

// ---------------------------------------------------------------------------
// Dataset response type (local — matches backend shape)
// ---------------------------------------------------------------------------

type DatasetResponse = {
  publicId: string;
  name: string;
  description: string | null;
  status: DatasetStatus;
  testCaseCount: number;
  requirementTitle: string | null;
  createdAt: string;
  updatedAt: string;
};

// ---------------------------------------------------------------------------
// Status filter options
// ---------------------------------------------------------------------------

type StatusFilter = 'ALL' | DatasetStatus;

// ---------------------------------------------------------------------------
// Date formatter
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

export default function DatasetsPage() {
  const t = useTranslations('datasets');
  const router = useRouter();
  const params = useParams();
  const projectId = params.projectId as string;

  // State
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [dialogOpen, setDialogOpen] = useState(false);

  // Build query URL
  const status = statusFilter === 'ALL' ? undefined : statusFilter;

  const { data, isLoading } = useQuery({
    queryKey: ['datasets', projectId, { page, status }],
    queryFn: () =>
      apiClient.get<PageResponse<DatasetResponse>>(
        `/api/v1/projects/${projectId}/datasets?page=${page}&size=20${status ? '&status=' + status : ''}`,
      ),
  });

  const datasets = data?.items ?? [];
  const totalItems = data?.totalItems ?? 0;
  const totalPages = data?.totalPages ?? 0;

  // Table columns
  const columns = useMemo<ColumnDef<DatasetResponse, unknown>[]>(
    () => [
      {
        accessorKey: 'name',
        header: t('columns.name'),
        cell: ({ row }) => (
          <span className="font-medium">{row.original.name}</span>
        ),
      },
      {
        accessorKey: 'status',
        header: t('columns.status'),
        size: 120,
        cell: ({ row }) => <StatusBadge status={row.original.status} size="sm" />,
      },
      {
        accessorKey: 'testCaseCount',
        header: t('columns.testCaseCount'),
        size: 120,
        cell: ({ row }) => (
          <span className="text-muted-foreground">
            {row.original.testCaseCount}
          </span>
        ),
      },
      {
        accessorKey: 'requirementTitle',
        header: t('columns.requirement'),
        size: 200,
        cell: ({ row }) => (
          <span className="text-muted-foreground truncate max-w-[180px] inline-block">
            {row.original.requirementTitle ?? '—'}
          </span>
        ),
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
    [t],
  );

  // Handlers
  const handleRowClick = (row: DatasetResponse) => {
    router.push(`/projects/${projectId}/datasets/${row.publicId}`);
  };

  const handlePaginationChange = (nextPage: number) => {
    setPage(nextPage);
  };

  // Filter tabs
  const filterOptions: { label: string; value: StatusFilter }[] = [
    { label: t('filterAll'), value: 'ALL' },
    { label: t('filterDraft'), value: 'DRAFT' },
    { label: t('filterApproved'), value: 'APPROVED' },
    { label: t('filterArchived'), value: 'ARCHIVED' },
  ];

  return (
    <PageShell
      title={t('title')}
      description={t('description')}
      actions={
        <Button onClick={() => setDialogOpen(true)}>
          <Plus weight="bold" />
          {t('createDataset')}
        </Button>
      }
    >
      {/* Status filter tabs */}
      <div className="flex items-center gap-1">
        {filterOptions.map((opt) => (
          <Button
            key={opt.value}
            variant="outline"
            size="sm"
            className={cn(
              statusFilter === opt.value &&
                'bg-accent text-accent-foreground',
            )}
            onClick={() => {
              setStatusFilter(opt.value);
              setPage(0);
            }}
          >
            {opt.label}
          </Button>
        ))}
      </div>

      {/* Data table */}
      <DataTable
        columns={columns}
        data={datasets}
        totalItems={totalItems}
        pageIndex={page}
        pageSize={20}
        onPaginationChange={handlePaginationChange}
        loading={isLoading}
        onRowClick={handleRowClick}
        emptyMessage={t('noDatasets')}
        emptyAction={
          <Button onClick={() => setDialogOpen(true)}>
            <Plus weight="bold" />
            {t('createDataset')}
          </Button>
        }
      />

      {/* Pagination (below table) */}
      {totalItems > 0 && (
        <DataTablePagination
          pageIndex={page}
          pageSize={20}
          totalItems={totalItems}
          totalPages={totalPages}
          onPageChange={setPage}
          onPageSizeChange={() => {
            // Fixed page size of 20 for datasets
          }}
        />
      )}

      {/* Create dataset dialog */}
      <CreateDatasetDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        projectId={projectId}
      />
    </PageShell>
  );
}
