'use client';

import * as React from 'react';
import { useState, useMemo, useCallback } from 'react';
import { useTranslations } from 'next-intl';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  PlusIcon,
  PencilSimpleIcon,
  TrashIcon,
  WarningIcon,
  ChecksIcon,
  ArchiveIcon,
} from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { PageShell } from '@/components/layout/page-shell';
import { StatusBadge } from '@/components/ui/status-badge';
import { apiClient } from '@/lib/api/client';
import type { ApiError, PageResponse, RubricVersionStatus } from '@/lib/api/types';
import { getErrorMessageKey } from '@/lib/utils/error-messages';
import { CriteriaEditorPanel, type CriterionResponse } from './criteria-editor-panel';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type VersionDetailResponse = {
  publicId: string;
  versionNumber: number;
  status: RubricVersionStatus;
  rubricPublicId: string;
  rubricName: string;
  content: string | null;
  outputSchemaJson: string | null;
  criteriaCount: number;
  createdAt: string;
};

// ---------------------------------------------------------------------------
// Input styles
// ---------------------------------------------------------------------------



const textareaClassName =
  'flex min-h-[80px] w-full resize-none rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

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
// Page component
// ---------------------------------------------------------------------------

export default function VersionDetailPage({
  params,
}: {
  params: Promise<{ rubricId: string; versionId: string }>;
}) {
  const { rubricId, versionId } = React.use(params);
  const t = useTranslations('rubrics');
  const tCommon = useTranslations('common');
  const tErrors = useTranslations('errors');
  const queryClient = useQueryClient();

  // Panel state
  const [panelOpen, setPanelOpen] = useState(false);
  const [editingCriterion, setEditingCriterion] = useState<CriterionResponse | null>(null);
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // Fetch version detail
  const { data: version } = useQuery({
    queryKey: ['rubric-version', versionId],
    queryFn: () =>
      apiClient.get<VersionDetailResponse>(
        `/api/v1/rubric-versions/${versionId}`,
      ),
  });

  // Fetch criteria
  const { data: criteriaData } = useQuery({
    queryKey: ['rubric-criteria', versionId],
    queryFn: () =>
      apiClient.get<PageResponse<CriterionResponse>>(
        `/api/v1/rubric-versions/${versionId}/criteria`,
      ),
  });

  const criteria = useMemo(() => {
    const list = criteriaData?.items ?? [];
    return [...list].sort((a, b) => a.sortOrder - b.sortOrder);
  }, [criteriaData]);

  const totalWeight = useMemo(
    () => criteria.reduce((sum, c) => sum + c.weight, 0),
    [criteria],
  );

  const isDraft = version?.status === 'DRAFT';

  // Open panel for create
  const openCreate = useCallback(() => {
    setActionError(null);
    setEditingCriterion(null);
    setPanelOpen(true);
  }, []);

  // Open panel for edit
  const openEdit = useCallback((criterion: CriterionResponse) => {
    setActionError(null);
    setEditingCriterion(criterion);
    setPanelOpen(true);
  }, []);

  const closePanel = useCallback(() => {
    setPanelOpen(false);
    setEditingCriterion(null);
  }, []);

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: (criterionId: string) =>
      apiClient.del(`/api/v1/rubric-criteria/${criterionId}`),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['rubric-criteria', versionId],
      });
      void queryClient.invalidateQueries({
        queryKey: ['rubric-version', versionId],
      });
      setDeleteConfirmId(null);
    },
    onError: (error: unknown) => {
      setActionError(errorMessage(error, tErrors));
    },
  });

  const handleDelete = useCallback(
    (criterionId: string) => {
      deleteMutation.mutate(criterionId);
    },
    [deleteMutation],
  );

  // Publish version
  const publishMutation = useMutation({
    mutationFn: () =>
      apiClient.patch<VersionDetailResponse>(
        `/api/v1/rubric-versions/${versionId}`,
        { status: 'PUBLISHED' },
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['rubric-version', versionId],
      });
      void queryClient.invalidateQueries({
        queryKey: ['rubric-versions', rubricId],
      });
    },
    onError: (error: unknown) => {
      setActionError(errorMessage(error, tErrors));
    },
  });

  // Archive version
  const archiveMutation = useMutation({
    mutationFn: () =>
      apiClient.patch<VersionDetailResponse>(
        `/api/v1/rubric-versions/${versionId}`,
        { status: 'ARCHIVED' },
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['rubric-version', versionId],
      });
      void queryClient.invalidateQueries({
        queryKey: ['rubric-versions', rubricId],
      });
    },
    onError: (error: unknown) => {
      setActionError(errorMessage(error, tErrors));
    },
  });

  const handlePublish = useCallback(() => {
    setActionError(null);
    if (criteria.length === 0) {
      setActionError(t('publishDisabled'));
      return;
    }
    publishMutation.mutate();
  }, [criteria.length, publishMutation, t]);

  const handleArchive = useCallback(() => {
    setActionError(null);
    archiveMutation.mutate();
  }, [archiveMutation]);

  return (
    <PageShell
      title={
        version
          ? `${t('versionLabel', { number: version.versionNumber })} - ${version.rubricName}`
          : t('title')
      }
      backHref={`/rubrics/${rubricId}`}
      backLabel={tCommon('back')}
      actions={
        version && (
          <div className="flex items-center gap-3">
            <StatusBadge status={version.status} />
            {version.status === 'DRAFT' && (
              <Button
                size="sm"
                variant="default"
                disabled={publishMutation.isPending || criteria.length === 0}
                onClick={handlePublish}
              >
                <ChecksIcon weight="bold" className="mr-1.5 size-4" />
                {t('publish')}
              </Button>
            )}
            {(version.status === 'DRAFT' || version.status === 'PUBLISHED') && (
              <Button
                size="sm"
                variant="outline"
                disabled={archiveMutation.isPending}
                onClick={handleArchive}
              >
                <ArchiveIcon weight="bold" className="mr-1.5 size-4" />
                {t('archive')}
              </Button>
            )}
          </div>
        )
      }
    >
      {/* Locked Warning Banners */}
      {version && version.status !== 'DRAFT' && (
        <div className={cn(
          "rounded-xl border p-4 flex items-start gap-3 text-sm leading-relaxed",
          version.status === 'PUBLISHED' 
            ? "border-amber-200/50 bg-amber-500/10 text-amber-800 dark:text-amber-300"
            : "border-red-200/50 bg-red-500/10 text-red-800 dark:text-red-300"
        )}>
          <WarningIcon weight="bold" className="size-5 shrink-0 mt-0.5" />
          <div className="flex-1">
            <p className="font-semibold mb-0.5">
              {version.status === 'PUBLISHED' ? tCommon('status.PUBLISHED') : tCommon('status.ARCHIVED')}
            </p>
            <p className="text-muted-foreground">
              {version.status === 'PUBLISHED' ? t('lockedPublishedHint') : t('lockedArchivedHint')}
            </p>
          </div>
        </div>
      )}

      {/* Rubric content */}
      {version && (
        <RubricContentEditor
          key={version.publicId}
          versionId={versionId}
          isDraft={isDraft}
          content={version.content ?? ''}
          outputSchemaJson={version.outputSchemaJson ?? ''}
        />
      )}

      {/* Criteria header */}
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">{t('criteria')}</h2>
        {isDraft && (
          <Button size="sm" onClick={openCreate}>
            <PlusIcon weight="bold" />
            {t('addCriterion')}
          </Button>
        )}
      </div>

      {actionError && (
        <div className="rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {actionError}
        </div>
      )}

      {/* Weights Visualization */}
      {criteria.length > 0 && (
        <div className="rounded-xl border bg-card/40 p-5 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-foreground">{t('weightsDistribution')}</h3>
            <span className="text-xs text-muted-foreground">{t('totalWeight')}: {totalWeight}</span>
          </div>
          
          <div className="flex h-3 w-full rounded-full overflow-hidden bg-muted">
            {criteria.map((c, i) => {
              const pct = totalWeight > 0 ? (c.weight / totalWeight) * 100 : 0;
              if (pct === 0) return null;
              const COLORS = [
                'bg-blue-500 dark:bg-blue-600',
                'bg-emerald-500 dark:bg-emerald-600',
                'bg-amber-500 dark:bg-amber-600',
                'bg-violet-500 dark:bg-violet-600',
                'bg-pink-500 dark:bg-pink-600',
                'bg-cyan-500 dark:bg-cyan-600',
                'bg-orange-500 dark:bg-orange-600',
              ];
              const color = COLORS[i % COLORS.length];
              return (
                <div
                  key={c.publicId}
                  style={{ width: `${pct}%` }}
                  className={cn(color, "h-full transition-all duration-300")}
                  title={`${c.name}: ${c.weight} (${Math.round(pct)}%)`}
                />
              );
            })}
          </div>

          {/* Legend */}
          <div className="flex flex-wrap gap-x-4 gap-y-2 pt-1 text-xs">
            {criteria.map((c, i) => {
              const pct = totalWeight > 0 ? Math.round((c.weight / totalWeight) * 100) : 0;
              const COLORS = [
                'bg-blue-500',
                'bg-emerald-500',
                'bg-amber-500',
                'bg-violet-500',
                'bg-pink-500',
                'bg-cyan-500',
                'bg-orange-500',
              ];
              const color = COLORS[i % COLORS.length];
              return (
                <div key={c.publicId} className="flex items-center gap-1.5 text-muted-foreground">
                  <span className={cn("size-2 rounded-full", color)} />
                  <span className="font-medium text-foreground truncate max-w-[120px]">{c.name}</span>
                  <span>{pct}%</span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Criteria list */}
      {criteria.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-md border py-12 text-center">
          <p className="text-muted-foreground">{t('noRubrics')}</p>
          {isDraft && (
            <Button className="mt-4" onClick={openCreate}>
              <PlusIcon weight="bold" />
              {t('addCriterion')}
            </Button>
          )}
        </div>
      ) : (
        <div className="space-y-3">
          {criteria.map((criterion) => {
            const pct =
              totalWeight > 0
                ? Math.round((criterion.weight / totalWeight) * 100)
                : 0;

            return (
              <div
                key={criterion.publicId}
                className="rounded-md border bg-card p-4"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-medium">{criterion.name}</span>
                      {criterion.isCritical && (
                        <span className="inline-flex items-center gap-1 rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-800 dark:bg-red-950 dark:text-red-300">
                          <WarningIcon weight="bold" className="size-3" />
                          {t('isCritical')}
                        </span>
                      )}
                    </div>
                    {criterion.description && (
                      <p className="text-sm text-muted-foreground">
                        {criterion.description}
                      </p>
                    )}
                    <div className="flex flex-wrap gap-4 text-xs text-muted-foreground">
                      <span>
                        {t('weight')}: {criterion.weight} ({pct}%)
                      </span>
                      <span>
                        {t('metricKey')}: {criterion.metricKey}
                      </span>
                      <span>
                        {t('sortOrder')}: {criterion.sortOrder}
                      </span>
                    </div>
                  </div>
                  {isDraft && (
                    <div className="flex items-center gap-1">
                      <Button
                        variant="outline"
                        size="icon"
                        className="size-8"
                        onClick={() => openEdit(criterion)}
                      >
                        <PencilSimpleIcon weight="bold" className="size-4" />
                      </Button>
                      {deleteConfirmId === criterion.publicId ? (
                        <div className="flex items-center gap-1">
                          <Button
                            variant="destructive"
                            size="sm"
                            disabled={deleteMutation.isPending}
                            onClick={() => handleDelete(criterion.publicId)}
                          >
                            {tCommon('confirm')}
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => setDeleteConfirmId(null)}
                          >
                            {tCommon('cancel')}
                          </Button>
                        </div>
                      ) : (
                        <Button
                          variant="outline"
                          size="icon"
                          className="size-8"
                          onClick={() =>
                            setDeleteConfirmId(criterion.publicId)
                          }
                        >
                          <TrashIcon weight="bold" className="size-4" />
                        </Button>
                      )}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Criteria editor slide-in panel */}
      {panelOpen && isDraft && (
        <CriteriaEditorPanel
          versionId={versionId}
          criterion={editingCriterion}
          onClose={closePanel}
          nextSortOrder={
            criteria.length > 0
              ? Math.max(...criteria.map((c) => c.sortOrder)) + 1
              : 0
          }
        />
      )}
    </PageShell>
  );
}

// ---------------------------------------------------------------------------
// Rubric Content Editor
// ---------------------------------------------------------------------------

function RubricContentEditor({
  versionId,
  isDraft,
  content,
  outputSchemaJson,
}: {
  versionId: string;
  isDraft: boolean;
  content: string;
  outputSchemaJson: string;
}) {
  const t = useTranslations('rubrics');
  const tCommon = useTranslations('common');
  const tErrors = useTranslations('errors');
  const queryClient = useQueryClient();
  const [contentDraft, setContentDraft] = useState(content);
  const [schemaDraft, setSchemaDraft] = useState(outputSchemaJson);
  const [serverError, setServerError] = useState<string | null>(null);

  const isDirty = contentDraft !== content || schemaDraft !== outputSchemaJson;

  const isSchemaInvalid = useMemo(() => {
    if (!schemaDraft.trim()) return false;
    try {
      JSON.parse(schemaDraft);
      return false;
    } catch {
      return true;
    }
  }, [schemaDraft]);

  React.useEffect(() => {
    if (!isDirty) return;
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      e.preventDefault();
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [isDirty]);

  const updateVersionMutation = useMutation({
    mutationFn: () =>
      apiClient.patch(`/api/v1/rubric-versions/${versionId}`, {
        content: contentDraft,
        outputSchemaJson: schemaDraft,
      }),
    onMutate: () => {
      setServerError(null);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['rubric-version', versionId],
      });
    },
    onError: (error: unknown) => {
      setServerError(errorMessage(error, tErrors));
    },
  });

  return (
    <section className="space-y-3 rounded-md border bg-card p-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">{t('rubricContent')}</h2>
        <div className="flex items-center gap-3">
          {isDirty && !isSchemaInvalid && (
            <span className="inline-flex items-center gap-1.5 text-xs text-amber-500 font-medium">
              <span className="size-2 rounded-full bg-amber-500 animate-pulse" />
              {t('unsavedChanges')}
            </span>
          )}
          {isSchemaInvalid && (
            <span className="text-xs text-red-500 font-medium">
              {t('invalidJson')}
            </span>
          )}
          {isDraft && (
            <Button
              size="sm"
              disabled={updateVersionMutation.isPending || !isDirty || isSchemaInvalid}
              onClick={() => updateVersionMutation.mutate()}
            >
              {updateVersionMutation.isPending ? tCommon('loading') : tCommon('save')}
            </Button>
          )}
        </div>
      </div>
      {serverError && (
        <div className="rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {serverError}
        </div>
      )}
      <textarea
        value={contentDraft}
        disabled={!isDraft || updateVersionMutation.isPending}
        onChange={(event) => setContentDraft(event.target.value)}
        className={cn(textareaClassName, 'min-h-[140px]')}
      />
      <details className="rounded-md border bg-background p-3">
        <summary className="cursor-pointer text-sm font-medium">
          {t('outputSchema')}
        </summary>
        <textarea
          value={schemaDraft}
          disabled={!isDraft || updateVersionMutation.isPending}
          onChange={(event) => setSchemaDraft(event.target.value)}
          className={cn(textareaClassName, 'mt-3 font-mono')}
        />
      </details>
    </section>
  );
}


