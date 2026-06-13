'use client';

import * as React from 'react';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { CheckCircle, Archive } from '@phosphor-icons/react';

import { apiClient } from '@/lib/api/client';
import type { DatasetStatus } from '@/lib/api/types';
import { Button } from '@/components/ui/button';
import { StatusBadge } from '@/components/ui/status-badge';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { PageShell } from '@/components/layout/page-shell';
import { TestCaseTable } from '@/components/test-cases/test-case-table';
import { Skeleton, SkeletonText } from '@/components/feedback/loading-skeleton';

// ---------------------------------------------------------------------------
// Dataset detail response type
// ---------------------------------------------------------------------------

type DatasetDetailResponse = {
  publicId: string;
  name: string;
  description: string | null;
  status: DatasetStatus;
  testCaseCount: number;
  activeTestCaseCount: number;
  requirementTitle: string | null;
  requirementPublicId: string | null;
  createdAt: string;
  updatedAt: string;
};

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
      <div className="rounded-lg border bg-card p-6 space-y-4">
        <SkeletonText width="w-3/4" />
        <div className="grid gap-4 sm:grid-cols-2">
          <SkeletonText width="w-1/2" />
          <SkeletonText width="w-1/2" />
          <SkeletonText width="w-1/3" />
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

  const [approveOpen, setApproveOpen] = React.useState(false);
  const [archiveOpen, setArchiveOpen] = React.useState(false);

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
      setApproveOpen(false);
    },
  });

  // --- Archive mutation ---
  const archiveMutation = useMutation({
    mutationFn: () =>
      apiClient.patch('/api/v1/datasets/' + datasetId, { status: 'ARCHIVED' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dataset', datasetId] });
      queryClient.invalidateQueries({ queryKey: ['datasets'] });
      setArchiveOpen(false);
    },
  });

  // --- Loading state ---
  if (isLoading) {
    return (
      <PageShell title={t('detail')}>
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
      actions={
        !isArchived ? (
          <div className="flex items-center gap-2">
            {isDraft && (
              <Button
                disabled={!canApprove}
                onClick={() => setApproveOpen(true)}
              >
                <CheckCircle className="mr-2 size-4" weight="bold" />
                {t('approve')}
              </Button>
            )}
            {(isDraft || isApproved) && (
              <Button
                variant="outline"
                onClick={() => setArchiveOpen(true)}
              >
                <Archive className="mr-2 size-4" weight="bold" />
                {t('archive')}
              </Button>
            )}
          </div>
        ) : undefined
      }
    >
      {/* ---- Dataset info card ---- */}
      <div className="rounded-lg border bg-card p-6 space-y-4">
        <div className="flex items-center gap-3">
          <StatusBadge status={dataset.status} />
        </div>

        {dataset.description && (
          <p className="text-sm text-muted-foreground">
            {dataset.description}
          </p>
        )}

        <div className="grid gap-4 sm:grid-cols-2 text-sm text-muted-foreground">
          <div>
            <span className="font-medium text-foreground">
              {t('columns.requirement')}:
            </span>{' '}
            {dataset.requirementTitle ?? tCommon('notAvailable')}
          </div>
          <div>
            <span className="font-medium text-foreground">
              {t('columns.testCaseCount')}:
            </span>{' '}
            {dataset.testCaseCount}
          </div>
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

        {/* Helper text: cannot approve without active test cases */}
        {isDraft && !canApprove && (
          <p className="text-sm text-amber-600 dark:text-amber-400">
            {t('approveDisabledHint')}
          </p>
        )}
      </div>

      {/* ---- Test case table (placeholder — Epic 7) ---- */}
      <section className="space-y-3">
        <h2 className="text-lg font-semibold tracking-tight">
          {t('testCases')}
        </h2>
        <TestCaseTable
          datasetId={datasetId as string}
          datasetStatus={dataset.status}
          projectId={projectId as string}
        />
      </section>

      {/* ---- Approve confirm dialog ---- */}
      <ConfirmDialog
        open={approveOpen}
        onOpenChange={setApproveOpen}
        title={t('approve')}
        description={t('approveConfirm', { name: dataset.name })}
        confirmLabel={t('approve')}
        loading={approveMutation.isPending}
        onConfirm={() => approveMutation.mutate()}
      />

      {/* ---- Archive confirm dialog ---- */}
      <ConfirmDialog
        open={archiveOpen}
        onOpenChange={setArchiveOpen}
        title={t('archive')}
        description={t('archiveConfirm', { name: dataset.name })}
        confirmLabel={t('archive')}
        variant="destructive"
        loading={archiveMutation.isPending}
        onConfirm={() => archiveMutation.mutate()}
      />
    </PageShell>
  );
}
