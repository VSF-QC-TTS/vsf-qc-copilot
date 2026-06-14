'use client';

import { useState, useMemo } from 'react';
import { useTranslations } from 'next-intl';
import { useQuery } from '@tanstack/react-query';
import { type ColumnDef } from '@tanstack/react-table';
import { PlusIcon } from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { PageShell } from '@/components/layout/page-shell';
import { DataTable } from '@/components/data-table/data-table';
import { DataTablePagination } from '@/components/data-table/data-table-pagination';
import { StatusBadge } from '@/components/ui/status-badge';
import { CreateProjectDialog } from '@/components/projects/create-project-dialog';
import { apiClient } from '@/lib/api/client';
import type { ProjectResponse, PageResponse, ProjectStatus } from '@/lib/api/types';
import { useRouter } from '@/i18n/navigation';

// ---------------------------------------------------------------------------
// Status filter options
// ---------------------------------------------------------------------------

type StatusFilter = 'ALL' | ProjectStatus;
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

export default function ProjectsPage() {
  const t = useTranslations('projects');
  const router = useRouter();

  // State
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [dialogOpen, setDialogOpen] = useState(false);

  // Build query URL
  const status = statusFilter === 'ALL' ? undefined : statusFilter;

  const { data, isLoading } = useQuery({
    queryKey: ['projects', { page, size: PAGE_SIZE, status }],
    queryFn: () =>
      apiClient.get<PageResponse<ProjectResponse>>(
        `/api/v1/projects?page=${page}&size=${PAGE_SIZE}&sort=createdAt,desc${status ? '&status=' + status : ''}`,
      ),
  });

  const projects = data?.items ?? [];
  const totalItems = data?.totalItems ?? 0;
  const totalPages = data?.totalPages ?? 0;

  // Table columns
  const columns = useMemo<ColumnDef<ProjectResponse, unknown>[]>(
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
        accessorKey: 'updatedAt',
        header: t('columns.updatedAt'),
        size: 140,
        cell: ({ row }) => (
          <span className="text-muted-foreground">
            {formatDate(row.original.updatedAt)}
          </span>
        ),
      },
    ],
    [t],
  );

  // Handlers
  const handleRowClick = (row: ProjectResponse) => {
    router.push(`/projects/${row.publicId}`);
  };

  const handlePaginationChange = (nextPage: number) => {
    setPage(nextPage);
  };

  // Filter tabs
  const filterOptions: { label: string; value: StatusFilter }[] = [
    { label: t('filterAll'), value: 'ALL' },
    { label: t('filterActive'), value: 'ACTIVE' },
    { label: t('filterArchived'), value: 'ARCHIVED' },
  ];

  return (
    <PageShell
      title={t('title')}
      description={t('description')}
      actions={
        <Button onClick={() => setDialogOpen(true)}>
          <PlusIcon weight="bold" />
          {t('createProject')}
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
        data={projects}
        totalItems={totalItems}
        pageIndex={page}
        pageSize={PAGE_SIZE}
        onPaginationChange={handlePaginationChange}
        loading={isLoading}
        onRowClick={handleRowClick}
        emptyMessage={t('noProjects')}
        emptyAction={
          <Button onClick={() => setDialogOpen(true)}>
            <PlusIcon weight="bold" />
            {t('createProject')}
          </Button>
        }
      />

      {/* Pagination (below table) */}
      {totalItems > 0 && (
        <DataTablePagination
          pageIndex={page}
          pageSize={PAGE_SIZE}
          totalItems={totalItems}
          totalPages={totalPages}
          onPageChange={setPage}
        />
      )}

      {/* Create project dialog */}
      <CreateProjectDialog open={dialogOpen} onOpenChange={setDialogOpen} />
    </PageShell>
  );
}
