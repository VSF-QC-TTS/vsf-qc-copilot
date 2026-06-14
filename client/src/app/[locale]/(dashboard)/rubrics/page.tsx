'use client';

import { useState, useMemo, useCallback } from 'react';
import { useTranslations } from 'next-intl';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { type ColumnDef } from '@tanstack/react-table';
import { PlusIcon, CopyIcon, ListChecksIcon } from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { PageShell } from '@/components/layout/page-shell';
import { DataTable } from '@/components/data-table/data-table';
import { DataTablePagination } from '@/components/data-table/data-table-pagination';
import { CreateRubricDialog } from '@/components/rubrics/create-rubric-dialog';
import { apiClient } from '@/lib/api/client';
import type { PageResponse } from '@/lib/api/types';
import { useRouter } from '@/i18n/navigation';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type RubricResponse = {
  publicId: string;
  name: string;
  description: string | null;
  projectName: string | null;
  createdAt: string;
  updatedAt: string;
};

// ---------------------------------------------------------------------------
// Tab type
// ---------------------------------------------------------------------------

type TabValue = 'my' | 'templates';
const PAGE_SIZE = 10;

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

export default function RubricsPage() {
  const t = useTranslations('rubrics');
  const tCommon = useTranslations('common');
  const router = useRouter();
  const queryClient = useQueryClient();

  // State
  const [activeTab, setActiveTab] = useState<TabValue>('my');
  const [page, setPage] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);

  // Fetch my rubrics
  const { data: myData, isLoading: myLoading } = useQuery({
    queryKey: ['rubrics', 'my', { page, size: PAGE_SIZE }],
    queryFn: () =>
      apiClient.get<PageResponse<RubricResponse>>(
        `/api/v1/rubrics?page=${page}&size=${PAGE_SIZE}`,
      ),
    enabled: activeTab === 'my',
  });

  // Fetch templates
  const { data: templateData, isLoading: templateLoading } = useQuery({
    queryKey: ['rubrics', 'templates', { page, size: PAGE_SIZE }],
    queryFn: () =>
      apiClient.get<PageResponse<RubricResponse>>(
        `/api/v1/rubrics/templates?page=${page}&size=${PAGE_SIZE}`,
      ),
    enabled: activeTab === 'templates',
  });

  const data = activeTab === 'my' ? myData : templateData;
  const isLoading = activeTab === 'my' ? myLoading : templateLoading;
  const rubrics = data?.items ?? [];
  const totalItems = data?.totalItems ?? 0;
  const totalPages = data?.totalPages ?? 0;

  // Clone mutation
  const cloneMutation = useMutation({
    mutationFn: (rubricId: string) =>
      apiClient.post<RubricResponse>(`/api/v1/rubrics/${rubricId}/clone`),
    onSuccess: (cloned) => {
      void queryClient.invalidateQueries({ queryKey: ['rubrics'] });
      router.push(`/rubrics/${cloned.publicId}`);
    },
  });

  const handleClone = useCallback(
    (e: React.MouseEvent, rubricId: string) => {
      e.stopPropagation();
      cloneMutation.mutate(rubricId);
    },
    [cloneMutation],
  );

  // Table columns — My Rubrics
  const myColumns = useMemo<ColumnDef<RubricResponse, unknown>[]>(
    () => [
      {
        accessorKey: 'name',
        header: t('columns.name'),
        cell: ({ row }) => (
          <span className="font-medium">{row.original.name}</span>
        ),
      },
      {
        accessorKey: 'projectName',
        header: t('columns.project'),
        size: 160,
        cell: ({ row }) => (
          <span className="text-muted-foreground">
            {row.original.projectName ?? tCommon('notAvailable')}
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
    [t, tCommon],
  );

  // Table columns — Templates (with clone button)
  const templateColumns = useMemo<ColumnDef<RubricResponse, unknown>[]>(
    () => [
      {
        accessorKey: 'name',
        header: t('columns.name'),
        cell: ({ row }) => (
          <span className="font-medium">{row.original.name}</span>
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
      {
        id: 'actions',
        header: '',
        size: 100,
        cell: ({ row }) => (
          <Button
            variant="outline"
            size="sm"
            disabled={cloneMutation.isPending}
            onClick={(e) => handleClone(e, row.original.publicId)}
          >
            <CopyIcon weight="bold" className="mr-1 size-4" />
            {t('clone')}
          </Button>
        ),
      },
    ],
    [t, cloneMutation.isPending, handleClone],
  );

  const columns = activeTab === 'my' ? myColumns : templateColumns;

  // Handlers
  const handleRowClick = (row: RubricResponse) => {
    if (activeTab === 'my') {
      router.push(`/rubrics/${row.publicId}`);
    }
  };

  const handlePaginationChange = (nextPage: number) => {
    setPage(nextPage);
  };

  const handleTabChange = (tab: TabValue) => {
    setActiveTab(tab);
    setPage(0);
  };

  // Tab options
  const tabs: { label: string; value: TabValue }[] = [
    { label: t('myRubrics'), value: 'my' },
    { label: t('templates'), value: 'templates' },
  ];

  return (
    <PageShell
      title={t('title')}
      actions={
        <Button onClick={() => setDialogOpen(true)}>
          <PlusIcon weight="bold" />
          {t('createRubric')}
        </Button>
      }
    >
      {/* Tab bar */}
      <div className="flex items-center gap-1">
        {tabs.map((tab) => (
          <Button
            key={tab.value}
            variant="outline"
            size="sm"
            className={cn(
              activeTab === tab.value &&
                'bg-accent text-accent-foreground',
            )}
            onClick={() => handleTabChange(tab.value)}
          >
            {tab.label}
          </Button>
        ))}
      </div>

      {/* Data table */}
      <DataTable
        columns={columns}
        data={rubrics}
        totalItems={totalItems}
        pageIndex={page}
        pageSize={PAGE_SIZE}
        onPaginationChange={handlePaginationChange}
        loading={isLoading}
        onRowClick={activeTab === 'my' ? handleRowClick : undefined}
        emptyMessage={t('noRubrics')}
        emptyAction={
          <Button onClick={() => setDialogOpen(true)}>
            <ListChecksIcon weight="bold" />
            {t('createRubric')}
          </Button>
        }
      />

      {/* Pagination */}
      {totalItems > 0 && (
        <DataTablePagination
          pageIndex={page}
          pageSize={PAGE_SIZE}
          totalItems={totalItems}
          totalPages={totalPages}
          onPageChange={setPage}
        />
      )}

      {/* Create rubric dialog */}
      <CreateRubricDialog open={dialogOpen} onOpenChange={setDialogOpen} />
    </PageShell>
  );
}
