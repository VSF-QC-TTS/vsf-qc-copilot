'use client';

import * as React from 'react';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { CheckCircleIcon, ArchiveIcon } from '@phosphor-icons/react';
import { motion } from 'motion/react';

import { apiClient } from '@/lib/api/client';
import type { DatasetDetailResponse } from '@/lib/api/types';
import { Button } from '@/components/ui/button';
import { StatusBadge } from '@/components/ui/status-badge';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { DatasetDialog } from '@/components/datasets/create-dataset-dialog';
import { PageShell } from '@/components/layout/page-shell';
import { useRouter } from '@/i18n/navigation';
import { useBreadcrumbStore } from '@/lib/store/breadcrumb-store';
import { TestCaseTable } from '@/components/test-cases/test-case-table';
import { Skeleton, SkeletonText } from '@/components/feedback/loading-skeleton';



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
// Loading skeleton
// ---------------------------------------------------------------------------

function DatasetDetailSkeleton() {
  return (
    <div className="space-y-6">
      {/* Header skeleton */}
      <div className="flex items-center justify-between gap-4">
        <div className="space-y-2">
          <Skeleton className="h-7 w-48" />
          <Skeleton className="h-4 w-32" />
        </div>
        <Skeleton className="h-9 w-24" />
      </div>

      {/* Info card skeleton */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="md:col-span-3 rounded-xl border bg-card p-6 space-y-4">
          <SkeletonText width="w-3/4" />
          <SkeletonText width="w-1/2" />
        </div>
        <div className="rounded-xl border bg-card p-6 space-y-4">
          <SkeletonText width="w-1/2" />
          <SkeletonText width="w-1/3" />
        </div>
      </div>

      {/* Table placeholder skeleton */}
      <Skeleton className="h-48 rounded-lg" />
    </div>
  );
}

// ---------------------------------------------------------------------------
// Page component
// ---------------------------------------------------------------------------

export default function DatasetDetailPage() {
  const t = useTranslations('datasets');
  const tCommon = useTranslations('common');
  const params = useParams();
  const queryClient = useQueryClient();
  const projectId = params.projectId as string;
  const datasetId = params.datasetId as string;
  const router = useRouter();

  const [approveOpen, setApproveOpen] = React.useState(false);
  const [archiveOpen, setArchiveOpen] = React.useState(false);
  const [editOpen, setEditOpen] = React.useState(false);
  const [deleteOpen, setDeleteOpen] = React.useState(false);

  // --- Fetch dataset ---
  const {
    data: dataset,
    isLoading,
  } = useQuery({
    queryKey: ['dataset', datasetId],
    queryFn: () =>
      apiClient.get<DatasetDetailResponse>('/api/v1/datasets/' + datasetId),
  });

  // --- Approve mutation ---
  const approveMutation = useMutation({
    mutationFn: () =>
      apiClient.patch('/api/v1/datasets/' + datasetId, { status: 'APPROVED' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dataset', datasetId] });
      queryClient.invalidateQueries({ queryKey: ['datasets'] });
      queryClient.invalidateQueries({ queryKey: ['project-readiness', projectId] });
      setApproveOpen(false);
    },
  });

  React.useEffect(() => {
    if (dataset) {
      useBreadcrumbStore.getState().setMapping(datasetId, dataset.name);
    }
  }, [dataset, datasetId]);

  // --- ArchiveIcon mutation ---
  const archiveMutation = useMutation({
    mutationFn: () =>
      apiClient.patch('/api/v1/datasets/' + datasetId, { status: 'ARCHIVED' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dataset', datasetId] });
      queryClient.invalidateQueries({ queryKey: ['datasets'] });
      queryClient.invalidateQueries({ queryKey: ['project-readiness', projectId] });
      setArchiveOpen(false);
    },
  });

  // --- Delete mutation ---
  const deleteMutation = useMutation({
    mutationFn: () => apiClient.delete('/api/v1/datasets/' + datasetId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['datasets'] });
      queryClient.invalidateQueries({ queryKey: ['project-readiness', projectId] });
      router.push(`/projects/${projectId}/datasets`);
    },
  });

  // --- Loading state ---
  if (isLoading) {
    return (
      <PageShell
        title={t('detail')}
        backHref={`/projects/${projectId}/datasets`}
        backLabel={tCommon('back')}
      >
        <DatasetDetailSkeleton />
      </PageShell>
    );
  }

  if (!dataset) {
    return null;
  }

  const canApprove =
    dataset.status === 'DRAFT' && dataset.activeTestCaseCount > 0;
  const isDraft = dataset.status === 'DRAFT';
  const isApproved = dataset.status === 'APPROVED';
  const isArchived = dataset.status === 'ARCHIVED';

  return (
    <PageShell
      title={dataset.name}
      description={t('detail')}
      backHref={`/projects/${projectId}/datasets`}
      backLabel={tCommon('back')}
      actions={
        <div className="flex items-center gap-2">
          {isDraft && (
            <Button
              disabled={!canApprove}
              onClick={() => setApproveOpen(true)}
            >
              <CheckCircleIcon className="mr-2 size-4" weight="bold" />
              {t('approve')}
            </Button>
          )}
          <Button
            variant="outline"
            onClick={() => setEditOpen(true)}
          >
            {tCommon('edit', { fallback: 'Edit' })}
          </Button>
          {!isArchived && (
            <Button
              variant="outline"
              onClick={() => setArchiveOpen(true)}
            >
              <ArchiveIcon className="mr-2 size-4" weight="bold" />
              {t('archive')}
            </Button>
          )}
          <Button
            variant="destructive"
            onClick={() => setDeleteOpen(true)}
          >
            {tCommon('delete', { fallback: 'Delete' })}
          </Button>
        </div>
      }
    >
      <motion.div
        variants={containerVariants}
        initial="hidden"
        animate="show"
        className="space-y-6"
      >
        {/* ---- Dataset info card ---- */}
        <motion.div variants={itemVariants} className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="md:col-span-3 rounded-xl border bg-card p-6 flex flex-col gap-4 relative overflow-hidden">
            <div className="flex items-center gap-3">
              <StatusBadge status={dataset.status} />
              {isDraft && canApprove && (
                <div className="px-2 py-1 rounded-md bg-amber-500/10 text-amber-600 dark:text-amber-400 text-xs font-medium border border-amber-500/20 flex items-center gap-1.5">
                  <CheckCircleIcon weight="fill" className="size-3.5" />
                  {t('readyToApprove')}
                </div>
              )}
            </div>

            {dataset.description && (
              <p className="text-sm text-muted-foreground max-w-[65ch]">
                {dataset.description}
              </p>
            )}

            <div className="grid gap-x-6 gap-y-2 sm:grid-cols-2 text-sm text-muted-foreground mt-2">
              <div>
                <span className="font-medium text-foreground">
                  {t('columns.createdAt')}:
                </span>{' '}
                {formatDate(dataset.createdAt)}
              </div>
              <div>
                <span className="font-medium text-foreground">
                  {t('updatedAt')}:
                </span>{' '}
                {formatDate(dataset.updatedAt)}
              </div>
            </div>
          </div>

          <div className="rounded-xl border bg-card p-6 flex flex-col justify-center gap-2">
            <div className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
              {t('columns.testCaseCount')}
            </div>
            <div className="text-4xl font-semibold tracking-tight">
              {dataset.testCaseCount}
            </div>
          </div>
        </motion.div>

        {/* Helper text / Alerts */}
        {isDraft && (
          <motion.div variants={itemVariants}>
            {!canApprove ? (
              <div className="rounded-xl border border-blue-500/20 bg-blue-500/5 p-4 flex items-start gap-3 text-blue-700 dark:text-blue-400">
                <div className="text-sm">
                  <p className="opacity-90">{t('approveDisabledHint')}</p>
                </div>
              </div>
            ) : (
              <div className="rounded-xl border border-amber-500/30 bg-amber-500/5 p-4 flex items-center justify-between gap-4 text-amber-700 dark:text-amber-500 shadow-sm">
                <div className="text-sm">
                  <p className="font-medium">{t('readyToApproveHint', { count: dataset.activeTestCaseCount })}</p>
                </div>
                <Button size="sm" onClick={() => setApproveOpen(true)} className="shrink-0 bg-amber-600 hover:bg-amber-700 text-white">
                  <CheckCircleIcon className="mr-1.5 size-4" weight="bold" />
                  {t('approve')}
                </Button>
              </div>
            )}
          </motion.div>
        )}

        {/* ---- Test case table (placeholder — Epic 7) ---- */}
        <motion.section variants={itemVariants} className="space-y-3">
          <h2 className="text-lg font-semibold tracking-tight">
            {t('testCases')}
          </h2>
          <TestCaseTable
            datasetId={datasetId as string}
            datasetStatus={dataset.status}
          />
        </motion.section>
      </motion.div>

      {/* ---- Approve confirm dialog ---- */}
      <ConfirmDialog
        open={approveOpen}
        onOpenChange={setApproveOpen}
        title={t('approveDatasetTitle')}
        description={t('approveDatasetConfirm')}
        confirmLabel={t('approve')}
        loading={approveMutation.isPending}
        onConfirm={() => approveMutation.mutate()}
      />

      {/* ---- Archive confirm dialog ---- */}
      <ConfirmDialog
        open={archiveOpen}
        onOpenChange={setArchiveOpen}
        title={t('archiveDatasetTitle')}
        description={t('archiveDatasetConfirm')}
        confirmLabel={t('archive')}
        variant="destructive"
        loading={archiveMutation.isPending}
        onConfirm={() => archiveMutation.mutate()}
      />

      {/* ---- Delete confirm dialog ---- */}
      <ConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title={t('deleteDatasetTitle', { fallback: 'Delete Dataset' })}
        description={t('deleteDatasetConfirm', { name: dataset.name })}
        confirmLabel={tCommon('delete', { fallback: 'Delete' })}
        variant="destructive"
        loading={deleteMutation.isPending}
        onConfirm={() => deleteMutation.mutate()}
      />

      {/* ---- Edit Dataset Dialog ---- */}
      <DatasetDialog
        open={editOpen}
        onOpenChange={setEditOpen}
        projectId={projectId}
        initialData={dataset}
      />
    </PageShell>
  );
}
