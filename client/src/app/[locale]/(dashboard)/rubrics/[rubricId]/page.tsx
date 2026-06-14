'use client';

import * as React from 'react';
import { useState, useMemo, useCallback } from 'react';
import { useTranslations } from 'next-intl';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { type ColumnDef } from '@tanstack/react-table';
import {
  PlusIcon,
  PencilSimpleIcon,
  ChecksIcon,
  ArchiveIcon,
} from '@phosphor-icons/react';

import { Button } from '@/components/ui/button';
import { PageShell } from '@/components/layout/page-shell';
import { DataTable } from '@/components/data-table/data-table';
import { DataTablePagination } from '@/components/data-table/data-table-pagination';
import { StatusBadge } from '@/components/ui/status-badge';
import { apiClient } from '@/lib/api/client';
import type { ApiError, PageResponse, RubricVersionStatus } from '@/lib/api/types';
import { getErrorMessageKey } from '@/lib/utils/error-messages';
import { useRouter } from '@/i18n/navigation';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type RubricDetailResponse = {
  publicId: string;
  name: string;
  description: string | null;
  projectName: string | null;
  createdAt: string;
  updatedAt: string;
};

type VersionResponse = {
  publicId: string;
  versionNumber: number;
  status: RubricVersionStatus;
  criteriaCount: number;
  createdAt: string;
  updatedAt: string;
};
const PAGE_SIZE = 10;

// ---------------------------------------------------------------------------
// Input styles
// ---------------------------------------------------------------------------

const inputClassName =
  'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

const textareaClassName =
  'flex min-h-[80px] w-full resize-none rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

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

export default function RubricDetailPage({
  params,
}: {
  params: Promise<{ rubricId: string }>;
}) {
  const { rubricId } = React.use(params);
  const t = useTranslations('rubrics');
  const tCommon = useTranslations('common');
  const tErrors = useTranslations('errors');
  const router = useRouter();
  const queryClient = useQueryClient();

  // Editing metadata state
  const [isEditing, setIsEditing] = useState(false);
  const [editName, setEditName] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [metaError, setMetaError] = useState<string | null>(null);

  // Versions pagination
  const [vPage, setVPage] = useState(0);

  // Fetch rubric detail
  const { data: rubric } = useQuery({
    queryKey: ['rubric', rubricId],
    queryFn: () =>
      apiClient.get<RubricDetailResponse>(`/api/v1/rubrics/${rubricId}`),
  });

  // Fetch versions
  const { data: versionsData, isLoading: versionsLoading } = useQuery({
    queryKey: ['rubric-versions', rubricId, { page: vPage, size: PAGE_SIZE }],
    queryFn: () =>
      apiClient.get<PageResponse<VersionResponse>>(
        `/api/v1/rubrics/${rubricId}/versions?page=${vPage}&size=${PAGE_SIZE}`,
      ),
  });

  const versions = versionsData?.items ?? [];
  const totalVersions = versionsData?.totalItems ?? 0;
  const totalVersionPages = versionsData?.totalPages ?? 0;

  // Start editing
  const startEdit = useCallback(() => {
    if (rubric) {
      setEditName(rubric.name);
      setEditDescription(rubric.description ?? '');
      setIsEditing(true);
      setMetaError(null);
    }
  }, [rubric]);

  // Save metadata
  const metaMutation = useMutation({
    mutationFn: (body: { name: string; description?: string }) =>
      apiClient.patch<RubricDetailResponse>(
        `/api/v1/rubrics/${rubricId}`,
        body,
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['rubric', rubricId] });
      setIsEditing(false);
    },
    onError: (error: unknown) => {
      if (
        typeof error === 'object' &&
        error !== null &&
        'code' in error &&
        'status' in error &&
        'message' in error
      ) {
        const apiError = error as ApiError;
        const messageKey = getErrorMessageKey(apiError);
        const key = messageKey.replace(/^errors\./, '');
        setMetaError(tErrors(key));
      } else {
        setMetaError(tErrors('network'));
      }
    },
  });

  const handleSaveMeta = () => {
    if (!editName.trim()) return;
    metaMutation.mutate({
      name: editName.trim(),
      description: editDescription.trim() || undefined,
    });
  };

  // Create version
  const createVersionMutation = useMutation({
    mutationFn: () =>
      apiClient.post<VersionResponse>(
        `/api/v1/rubrics/${rubricId}/versions`,
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['rubric-versions', rubricId],
      });
    },
  });

  // Publish version
  const publishMutation = useMutation({
    mutationFn: (versionId: string) =>
      apiClient.patch<VersionResponse>(
        `/api/v1/rubric-versions/${versionId}`,
        { status: 'PUBLISHED' },
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['rubric-versions', rubricId],
      });
    },
  });

  // ArchiveIcon version
  const archiveMutation = useMutation({
    mutationFn: (versionId: string) =>
      apiClient.patch<VersionResponse>(
        `/api/v1/rubric-versions/${versionId}`,
        { status: 'ARCHIVED' },
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['rubric-versions', rubricId],
      });
    },
  });

  const [actionError, setActionError] = useState<string | null>(null);

  const handlePublish = useCallback(
    (e: React.MouseEvent, versionId: string, criteriaCount: number) => {
      e.stopPropagation();
      setActionError(null);
      if (criteriaCount === 0) {
        setActionError(t('publishDisabled'));
        return;
      }
      publishMutation.mutate(versionId);
    },
    [publishMutation, t],
  );

  const handleArchive = useCallback(
    (e: React.MouseEvent, versionId: string) => {
      e.stopPropagation();
      archiveMutation.mutate(versionId);
    },
    [archiveMutation],
  );

  // Version columns
  const versionColumns = useMemo<ColumnDef<VersionResponse, unknown>[]>(
    () => [
      {
        accessorKey: 'versionNumber',
        header: '#',
        size: 60,
        cell: ({ row }) => (
          <span className="font-medium">v{row.original.versionNumber}</span>
        ),
      },
      {
        accessorKey: 'status',
        header: tCommon('filter'),
        size: 120,
        cell: ({ row }) => <StatusBadge status={row.original.status} size="sm" />,
      },
      {
        accessorKey: 'criteriaCount',
        header: t('criteria'),
        size: 100,
        cell: ({ row }) => row.original.criteriaCount,
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
        size: 200,
        cell: ({ row }) => {
          const v = row.original;
          return (
            <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
              {v.status === 'DRAFT' && (
                <Button
                  variant="outline"
                  size="sm"
                  disabled={publishMutation.isPending}
                  onClick={(e) => handlePublish(e, v.publicId, v.criteriaCount)}
                >
                  <ChecksIcon weight="bold" className="mr-1 size-4" />
                  {t('publish')}
                </Button>
              )}
              {(v.status === 'DRAFT' || v.status === 'PUBLISHED') && (
                <Button
                  variant="outline"
                  size="sm"
                  disabled={archiveMutation.isPending}
                  onClick={(e) => handleArchive(e, v.publicId)}
                >
                  <ArchiveIcon weight="bold" className="mr-1 size-4" />
                  {t('archive')}
                </Button>
              )}
            </div>
          );
        },
      },
    ],
    [
      t,
      tCommon,
      publishMutation.isPending,
      archiveMutation.isPending,
      handlePublish,
      handleArchive,
    ],
  );

  const handleVersionRowClick = (row: VersionResponse) => {
    router.push(`/rubrics/${rubricId}/versions/${row.publicId}`);
  };

  return (
    <PageShell
      title={rubric?.name ?? t('title')}
      description={rubric?.description ?? undefined}
      backHref="/rubrics"
      backLabel={tCommon('back')}
    >
      {/* Metadata edit section */}
      <div className="rounded-md border bg-card p-4">
        {isEditing ? (
          <div className="space-y-3">
            {metaError && (
              <div className="rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
                {metaError}
              </div>
            )}
            <div className="space-y-1">
              <label className="text-sm font-medium text-foreground">
                {t('rubricName')}
              </label>
              <input
                type="text"
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                className={inputClassName}
              />
            </div>
            <div className="space-y-1">
              <label className="text-sm font-medium text-foreground">
                {t('rubricDescription')}
              </label>
              <textarea
                value={editDescription}
                onChange={(e) => setEditDescription(e.target.value)}
                className={textareaClassName}
              />
            </div>
            <div className="flex gap-2">
              <Button
                size="sm"
                disabled={metaMutation.isPending}
                onClick={handleSaveMeta}
              >
                {metaMutation.isPending ? tCommon('loading') : tCommon('save')}
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setIsEditing(false)}
              >
                {tCommon('cancel')}
              </Button>
            </div>
          </div>
        ) : (
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm text-muted-foreground">
                {rubric?.projectName ?? tCommon('notAvailable')}
              </p>
            </div>
            <Button variant="outline" size="sm" onClick={startEdit}>
              <PencilSimpleIcon weight="bold" className="mr-1 size-4" />
              {tCommon('edit')}
            </Button>
          </div>
        )}
      </div>

      {/* Versions section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">{t('versions')}</h2>
          <Button
            size="sm"
            disabled={createVersionMutation.isPending}
            onClick={() => createVersionMutation.mutate()}
          >
            <PlusIcon weight="bold" />
            {t('createVersion')}
          </Button>
        </div>

        {actionError && (
          <div className="rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
            {actionError}
          </div>
        )}

        <DataTable
          columns={versionColumns}
          data={versions}
          totalItems={totalVersions}
          pageIndex={vPage}
          pageSize={PAGE_SIZE}
          onPaginationChange={(nextPage) => {
            setVPage(nextPage);
          }}
          loading={versionsLoading}
          onRowClick={handleVersionRowClick}
          emptyMessage={t('noRubrics')}
        />

        {totalVersions > 0 && (
          <DataTablePagination
            pageIndex={vPage}
            pageSize={PAGE_SIZE}
            totalItems={totalVersions}
            totalPages={totalVersionPages}
            onPageChange={setVPage}
          />
        )}
      </div>
    </PageShell>
  );
}
