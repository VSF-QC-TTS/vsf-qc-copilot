'use client';

import { useState, useMemo } from 'react';
import { useParams } from 'next/navigation';
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
import { apiClient } from '@/lib/api/client';
import type { PageResponse } from '@/lib/api/types';
import { useRouter } from '@/i18n/navigation';

// ---------------------------------------------------------------------------
// Connector row type (minimal for listing)
// ---------------------------------------------------------------------------

type ConnectorRow = {
  publicId: string;
  name: string;
  method: string;
  baseUrl: string;
  path: string | null;
  active: boolean;
  createdAt: string;
};
const PAGE_SIZE = 10;

// ---------------------------------------------------------------------------
// HTTP method badge colors
// ---------------------------------------------------------------------------

const METHOD_COLORS: Record<string, string> = {
  GET: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300',
  POST: 'bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300',
  PUT: 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300',
  PATCH: 'bg-orange-100 text-orange-800 dark:bg-orange-950 dark:text-orange-300',
  DELETE: 'bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300',
};

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

function truncateUrl(url: string, max = 50): string {
  return url.length > max ? url.slice(0, max) + '...' : url;
}

// ---------------------------------------------------------------------------
// Page component
// ---------------------------------------------------------------------------

export default function ConnectorsPage() {
  const t = useTranslations('connectors');
  const tCommon = useTranslations('common');
  const router = useRouter();
  const params = useParams();
  const projectId = params.projectId as string;

  // Pagination state
  const [page, setPage] = useState(0);

  // Fetch connectors
  const { data, isLoading } = useQuery({
    queryKey: ['connectors', projectId, { page, size: PAGE_SIZE }],
    queryFn: () =>
      apiClient.get<PageResponse<ConnectorRow>>(
        `/api/v1/projects/${projectId}/target-api-connectors?page=${page}&size=${PAGE_SIZE}`,
      ),
  });

  const connectors = data?.items ?? [];
  const totalItems = data?.totalItems ?? 0;
  const totalPages = data?.totalPages ?? 0;

  // Table columns
  const columns = useMemo<ColumnDef<ConnectorRow, unknown>[]>(
    () => [
      {
        accessorKey: 'name',
        header: t('columns.name'),
        cell: ({ row }) => (
          <span className="font-medium">{row.original.name}</span>
        ),
      },
      {
        accessorKey: 'method',
        header: t('columns.method'),
        size: 100,
        cell: ({ row }) => {
          const method = row.original.method.toUpperCase();
          return (
            <span
              className={cn(
                'inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium',
                METHOD_COLORS[method] ?? 'bg-zinc-100 text-zinc-600',
              )}
            >
              {method}
            </span>
          );
        },
      },
      {
        accessorKey: 'baseUrl',
        header: t('columns.url'),
        cell: ({ row }) => {
          const full =
            row.original.baseUrl + (row.original.path ?? '');
          return (
            <span
              className="text-muted-foreground font-mono text-xs"
              title={full}
            >
              {truncateUrl(full)}
            </span>
          );
        },
      },
      {
        accessorKey: 'active',
        header: t('columns.status'),
        size: 120,
        cell: ({ row }) => (
          <StatusBadge
            status={row.original.active ? 'ACTIVE' : 'INACTIVE'}
            size="sm"
          />
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
  const handleRowClick = (row: ConnectorRow) => {
    router.push(`/projects/${projectId}/connectors/${row.publicId}`);
  };

  const handleCreate = () => {
    router.push(`/projects/${projectId}/connectors/new`);
  };

  return (
    <PageShell
      title={t('title')}
      description={t('description')}
      backHref={`/projects/${projectId}`}
      backLabel={tCommon('back')}
      actions={
        <Button onClick={handleCreate}>
          <PlusIcon weight="bold" />
          {t('createConnector')}
        </Button>
      }
    >
      {/* Data table */}
      <DataTable
        columns={columns}
        data={connectors}
        totalItems={totalItems}
        pageIndex={page}
        pageSize={PAGE_SIZE}
        onPaginationChange={(nextPage) => setPage(nextPage)}
        loading={isLoading}
        onRowClick={handleRowClick}
        emptyMessage={t('noConnectors')}
        emptyAction={
          <Button onClick={handleCreate}>
            <PlusIcon weight="bold" />
            {t('createConnector')}
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
    </PageShell>
  );
}
