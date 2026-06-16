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
  XIcon,
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
import { cn } from '@/lib/utils';
import { motion, AnimatePresence } from 'motion/react';

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

function errorMessage(error: unknown, tErrors: (key: string) => string): string {
  if (
    typeof error === 'object' &&
    error !== null &&
    'code' in error &&
    'status' in error &&
    'message' in error
  ) {
    const apiError = error as ApiError;
    const messageKey = getErrorMessageKey(apiError);
    return tErrors(messageKey.replace(/^errors\./, ''));
  }
  return tErrors('network');
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
  const [createVersionOpen, setCreateVersionOpen] = useState(false);
  const [selectedSourceVersionId, setSelectedSourceVersionId] = useState<string | null>(null);
  const [createVersionError, setCreateVersionError] = useState<string | null>(null);

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

  const { data: allVersionsData, isLoading: allVersionsLoading } = useQuery({
    queryKey: ['rubric-versions', rubricId, 'all-for-clone'],
    queryFn: () =>
      apiClient.get<PageResponse<VersionResponse>>(
        `/api/v1/rubrics/${rubricId}/versions?page=0&size=100`,
      ),
    enabled: createVersionOpen,
  });
  const sourceVersions = allVersionsData?.items ?? versions;
  const effectiveSelectedSourceVersionId =
    selectedSourceVersionId ?? sourceVersions[0]?.publicId ?? null;

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
    mutationFn: (sourceVersionPublicId: string | null) =>
      apiClient.post<VersionResponse>(
        `/api/v1/rubrics/${rubricId}/versions`,
        sourceVersionPublicId ? { sourceVersionPublicId } : undefined,
      ),
    onSuccess: (created) => {
      void queryClient.invalidateQueries({
        queryKey: ['rubric-versions', rubricId],
      });
      setCreateVersionOpen(false);
      setCreateVersionError(null);
      setSelectedSourceVersionId(null);
      router.push(`/rubrics/${rubricId}/versions/${created.publicId}`);
    },
    onError: (error: unknown) => {
      setCreateVersionError(errorMessage(error, tErrors));
    },
  });

  const openCreateVersion = () => {
    setCreateVersionError(null);
    setSelectedSourceVersionId(sourceVersions[0]?.publicId ?? null);
    setCreateVersionOpen(true);
  };

  const closeCreateVersion = () => {
    if (createVersionMutation.isPending) return;
    setCreateVersionOpen(false);
    setCreateVersionError(null);
  };

  const submitCreateVersion = () => {
    setCreateVersionError(null);
    createVersionMutation.mutate(effectiveSelectedSourceVersionId);
  };

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
      void queryClient.invalidateQueries({
        queryKey: ['project-readiness'],
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
      void queryClient.invalidateQueries({
        queryKey: ['project-readiness'],
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
      <motion.div
        variants={containerVariants}
        initial="hidden"
        animate="show"
        className="space-y-6"
      >
        {/* Metadata edit section */}
        <motion.div
          variants={itemVariants}
          className="rounded-xl border bg-card/50 shadow-sm backdrop-blur-sm p-5"
        >
          <AnimatePresence mode="wait">
            {isEditing ? (
              <motion.div
                key="editing"
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                transition={{ duration: 0.2 }}
                className="space-y-4 overflow-hidden"
              >
                {metaError && (
                  <div className="rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
                    {metaError}
                  </div>
                )}
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-1.5 sm:col-span-2">
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
                  <div className="space-y-1.5 sm:col-span-2">
                    <label className="text-sm font-medium text-foreground">
                      {t('rubricDescription')}
                    </label>
                    <textarea
                      value={editDescription}
                      onChange={(e) => setEditDescription(e.target.value)}
                      className={textareaClassName}
                    />
                  </div>
                </div>
                <div className="flex gap-2 pt-2">
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
              </motion.div>
            ) : (
              <motion.div
                key="viewing"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                transition={{ duration: 0.2 }}
                className="flex flex-col sm:flex-row sm:items-start justify-between gap-4"
              >
                <div className="space-y-1">
                  <p className="text-sm text-muted-foreground flex items-center gap-2">
                    <span className="font-semibold">{t('columns.project')}:</span>
                    {rubric?.projectName ?? tCommon('notAvailable')}
                  </p>
                  {rubric?.description && (
                    <p className="text-sm text-foreground mt-2 max-w-2xl leading-relaxed">
                      {rubric.description}
                    </p>
                  )}
                </div>
                <Button variant="outline" size="sm" onClick={startEdit} className="shrink-0">
                  <PencilSimpleIcon weight="bold" className="mr-2 size-4" />
                  {tCommon('edit')}
                </Button>
              </motion.div>
            )}
          </AnimatePresence>
        </motion.div>

        {/* Versions section */}
        <motion.div variants={itemVariants} className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">{t('versions')}</h2>
            <Button
              size="sm"
              disabled={createVersionMutation.isPending}
              onClick={openCreateVersion}
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
        </motion.div>

        <CreateVersionDialog
          open={createVersionOpen}
          versions={sourceVersions}
          selectedVersionId={effectiveSelectedSourceVersionId}
          loadingVersions={allVersionsLoading}
          submitting={createVersionMutation.isPending}
          error={createVersionError}
          onSelect={setSelectedSourceVersionId}
          onSubmit={submitCreateVersion}
          onClose={closeCreateVersion}
        />
      </motion.div>
    </PageShell>
  );
}

function CreateVersionDialog({
  open,
  versions,
  selectedVersionId,
  loadingVersions,
  submitting,
  error,
  onSelect,
  onSubmit,
  onClose,
}: {
  open: boolean;
  versions: VersionResponse[];
  selectedVersionId: string | null;
  loadingVersions: boolean;
  submitting: boolean;
  error: string | null;
  onSelect: (versionId: string | null) => void;
  onSubmit: () => void;
  onClose: () => void;
}) {
  const t = useTranslations('rubrics');
  const tCommon = useTranslations('common');

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={onClose}
        aria-hidden="true"
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-version-title"
        className="relative z-10 w-full max-w-lg rounded-lg border bg-card p-6 shadow-lg"
      >
        <div className="flex items-center justify-between gap-3">
          <h2 id="create-version-title" className="text-lg font-semibold">
            {t('createVersion')}
          </h2>
          <Button
            variant="outline"
            size="icon"
            className="size-8"
            disabled={submitting}
            onClick={onClose}
          >
            <XIcon weight="bold" className="size-4" />
          </Button>
        </div>

        {error && (
          <div className="mt-4 rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
            {error}
          </div>
        )}

        <div className="mt-4 space-y-3">
          <p className="text-sm text-muted-foreground">
            {t('createVersionSourceHelp')}
          </p>

          {loadingVersions ? (
            <div className="rounded-md border px-4 py-6 text-center text-sm text-muted-foreground">
              {tCommon('loading')}
            </div>
          ) : versions.length > 0 ? (
            <div className="space-y-2 max-h-[40vh] overflow-y-auto pr-1">
              {versions.map((version) => (
                <label
                  key={version.publicId}
                  className={cn(
                    "flex cursor-pointer items-center justify-between gap-3 rounded-md border px-4 py-3 transition-colors",
                    selectedVersionId === version.publicId ? "bg-accent border-accent-foreground/20" : "bg-background hover:bg-muted/50"
                  )}
                >
                  <span className="flex items-center gap-3">
                    <input
                      type="radio"
                      name="sourceVersionPublicId"
                      checked={selectedVersionId === version.publicId}
                      disabled={submitting}
                      onChange={() => onSelect(version.publicId)}
                      className="size-4"
                    />
                    <span className="text-sm font-medium">
                      v{version.versionNumber}
                    </span>
                  </span>
                  <span className="flex items-center gap-3 text-xs text-muted-foreground">
                    <span>{t('criteria')}: {version.criteriaCount}</span>
                    <StatusBadge status={version.status} size="sm" />
                  </span>
                </label>
              ))}
            </div>
          ) : (
            <label className="flex cursor-pointer items-start gap-3 rounded-md border bg-background px-4 py-3">
              <input
                type="radio"
                name="sourceVersionPublicId"
                checked={selectedVersionId === null}
                disabled={submitting}
                onChange={() => onSelect(null)}
                className="mt-1 size-4"
              />
              <span>
                <span className="block text-sm font-medium">
                  {t('createBlankVersion')}
                </span>
                <span className="text-xs text-muted-foreground">
                  {t('createBlankVersionHelp')}
                </span>
              </span>
            </label>
          )}
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <Button variant="outline" disabled={submitting} onClick={onClose}>
            {tCommon('cancel')}
          </Button>
          <Button disabled={submitting || loadingVersions} onClick={onSubmit}>
            {submitting ? tCommon('loading') : tCommon('create')}
          </Button>
        </div>
      </div>
    </div>
  );
}
