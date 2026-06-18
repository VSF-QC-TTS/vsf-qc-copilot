'use client';

import { useState, useMemo } from 'react';
import { useParams } from 'next/navigation';
import { useTranslations, useLocale } from 'next-intl';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { type ColumnDef } from '@tanstack/react-table';
import { PlusIcon, PencilSimpleIcon, TrashIcon } from '@phosphor-icons/react';
import { motion } from 'motion/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { PageShell } from '@/components/layout/page-shell';
import { DataTable } from '@/components/data-table/data-table';
import { DataTablePagination } from '@/components/data-table/data-table-pagination';
import { StatusBadge } from '@/components/ui/status-badge';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { DatasetDialog } from '@/components/datasets/create-dataset-dialog';
import { apiClient } from '@/lib/api/client';
import type { PageResponse, DatasetStatus, DatasetDetailResponse } from '@/lib/api/types';
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
  createdAt: string;
  updatedAt: string;
};

// ---------------------------------------------------------------------------
// Status filter options
// ---------------------------------------------------------------------------

type StatusFilter = 'ALL' | DatasetStatus;
const PAGE_SIZE = 10;

// ---------------------------------------------------------------------------
// Date formatter
// ---------------------------------------------------------------------------

function formatDate(iso: string, locale: string): string {
  if (!iso) return '-';
  return new Date(iso).toLocaleDateString(locale, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

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
// Page component
// ---------------------------------------------------------------------------

export default function DatasetsPage() {
  const t = useTranslations('datasets');
  const tCommon = useTranslations('common');
  const locale = useLocale();
  const router = useRouter();
  const params = useParams();
  const projectId = params.projectId as string;
  const queryClient = useQueryClient();

  // State
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editDataset, setEditDataset] = useState<DatasetResponse | null>(null);
  const [deleteDataset, setDeleteDataset] = useState<DatasetResponse | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Delete Mutation
  const deleteMutation = useMutation({
    mutationFn: (id: string) => apiClient.delete(`/api/v1/datasets/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['datasets', projectId] });
      queryClient.invalidateQueries({ queryKey: ['project-readiness', projectId] });
      setDeleteDataset(null);
    },
    onSettled: () => setIsDeleting(false),
  });

  const handleDelete = () => {
    if (!deleteDataset) return;
    setIsDeleting(true);
    deleteMutation.mutate(deleteDataset.publicId);
  };

  // Build query URL
  const status = statusFilter === 'ALL' ? undefined : statusFilter;

  const { data, isLoading } = useQuery({
    queryKey: ['datasets', projectId, { page, size: PAGE_SIZE, status }],
    queryFn: () =>
      apiClient.get<PageResponse<DatasetResponse>>(
        `/api/v1/projects/${projectId}/datasets?page=${page}&size=${PAGE_SIZE}${status ? '&status=' + status : ''}`,
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
        accessorKey: 'createdAt',
        header: t('columns.createdAt'),
        size: 140,
        cell: ({ row }) => (
          <span className="text-muted-foreground">
            {formatDate(row.original.createdAt, locale)}
          </span>
        ),
      },
      {
        id: 'actions',
        size: 80,
        cell: ({ row }) => {
          const item = row.original;
          return (
            <div className="flex items-center justify-end gap-2" onClick={(e) => e.stopPropagation()}>
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setEditDataset(item)}
                title={tCommon('edit', { fallback: 'Edit' })}
              >
                <PencilSimpleIcon weight="bold" />
              </Button>
              <Button
                variant="ghost"
                size="icon"
                className="text-destructive hover:bg-destructive/10 hover:text-destructive"
                onClick={() => setDeleteDataset(item)}
                title={tCommon('delete', { fallback: 'Delete' })}
              >
                <TrashIcon weight="bold" />
              </Button>
            </div>
          );
        },
      },
    ],
    [t, tCommon],
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
      backHref={`/projects/${projectId}`}
      backLabel={t('back', { fallback: 'Back' })}
      actions={
        <Button onClick={() => setDialogOpen(true)}>
          <PlusIcon weight="bold" />
          {t('createDataset')}
        </Button>
      }
    >
      <motion.div
        variants={containerVariants}
        initial="hidden"
        animate="show"
        className="space-y-4"
      >
        {/* Status filter tabs */}
        <motion.div variants={itemVariants} className="flex items-center gap-1 p-1 bg-muted/40 border border-border/50 rounded-lg w-fit">
          {filterOptions.map((opt) => {
            const isActive = statusFilter === opt.value;
            return (
              <button
                key={opt.value}
                onClick={() => {
                  setStatusFilter(opt.value);
                  setPage(0);
                }}
                className={cn(
                  "relative px-4 py-1.5 text-sm font-medium rounded-md transition-colors",
                  isActive ? "text-foreground" : "text-muted-foreground hover:text-foreground hover:bg-muted/50"
                )}
              >
                {isActive && (
                  <motion.div
                    layoutId="datasetsFilterTab"
                    className="absolute inset-0 bg-background shadow-xs border border-border/50 rounded-md"
                    transition={{ type: "spring", stiffness: 400, damping: 30 }}
                  />
                )}
                <span className="relative z-10">{opt.label}</span>
              </button>
            );
          })}
        </motion.div>

        {/* Data table */}
        <motion.div variants={itemVariants}>
          <DataTable
            columns={columns}
            data={datasets}
            totalItems={totalItems}
            pageIndex={page}
            pageSize={PAGE_SIZE}
            onPaginationChange={handlePaginationChange}
            loading={isLoading}
            onRowClick={handleRowClick}
            emptyMessage={t('noDatasets')}
            emptyAction={
              <Button onClick={() => setDialogOpen(true)}>
                <PlusIcon weight="bold" />
                {t('createDataset')}
              </Button>
            }
          />
        </motion.div>

        {/* Pagination (below table) */}
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

      {/* Create / Edit dataset dialog */}
      <DatasetDialog
        open={dialogOpen || !!editDataset}
        onOpenChange={(open) => {
          if (!open) {
            setDialogOpen(false);
            setEditDataset(null);
          } else {
            setDialogOpen(true);
          }
        }}
        projectId={projectId}
        initialData={editDataset as unknown as DatasetDetailResponse}
      />

      {/* Delete dataset dialog */}
      <ConfirmDialog
        open={!!deleteDataset}
        onOpenChange={(open) => !open && setDeleteDataset(null)}
        title={t('deleteDatasetTitle', { fallback: 'Delete Dataset' })}
        description={t('deleteDatasetConfirm', { name: deleteDataset?.name ?? '' })}
        confirmLabel={tCommon('delete', { fallback: 'Delete' })}
        variant="destructive"
        loading={isDeleting}
        onConfirm={handleDelete}
      />
    </PageShell>
  );
}
