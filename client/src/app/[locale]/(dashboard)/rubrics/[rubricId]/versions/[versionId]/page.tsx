'use client';

import * as React from 'react';
import { useState, useMemo, useCallback } from 'react';
import { useForm, Controller, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslations } from 'next-intl';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Plus,
  PencilSimple,
  Trash,
  ArrowLeft,
  X,
  Warning,
} from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { PageShell } from '@/components/layout/page-shell';
import { StatusBadge } from '@/components/ui/status-badge';
import { apiClient } from '@/lib/api/client';
import type { ApiError, RubricVersionStatus } from '@/lib/api/types';
import { getErrorMessageKey } from '@/lib/utils/error-messages';
import {
  createCriterionSchema,
  type CreateCriterionFormValues,
} from '@/lib/validations/rubric';
import { useRouter } from '@/i18n/navigation';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type VersionDetailResponse = {
  publicId: string;
  versionNumber: number;
  status: RubricVersionStatus;
  rubricPublicId: string;
  rubricName: string;
  criteriaCount: number;
  createdAt: string;
  updatedAt: string;
};

type CriterionResponse = {
  publicId: string;
  name: string;
  description: string | null;
  weight: number;
  judgeInstruction: string;
  passCondition: string | null;
  failCondition: string | null;
  metricKey: string;
  isCritical: boolean;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
};

// ---------------------------------------------------------------------------
// Input styles
// ---------------------------------------------------------------------------

const inputClassName =
  'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

const textareaClassName =
  'flex min-h-[80px] w-full resize-none rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

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
  const router = useRouter();
  const queryClient = useQueryClient();

  // Panel state
  const [panelOpen, setPanelOpen] = useState(false);
  const [editingCriterion, setEditingCriterion] = useState<CriterionResponse | null>(null);
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);

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
      apiClient.get<CriterionResponse[]>(
        `/api/v1/rubric-versions/${versionId}/criteria`,
      ),
  });

  const criteria = useMemo(() => {
    const list = criteriaData ?? [];
    return [...list].sort((a, b) => a.sortOrder - b.sortOrder);
  }, [criteriaData]);

  const totalWeight = useMemo(
    () => criteria.reduce((sum, c) => sum + c.weight, 0),
    [criteria],
  );

  const isDraft = version?.status === 'DRAFT';

  // Open panel for create
  const openCreate = useCallback(() => {
    setEditingCriterion(null);
    setPanelOpen(true);
  }, []);

  // Open panel for edit
  const openEdit = useCallback((criterion: CriterionResponse) => {
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
  });

  const handleDelete = useCallback(
    (criterionId: string) => {
      deleteMutation.mutate(criterionId);
    },
    [deleteMutation],
  );

  return (
    <PageShell
      title={
        version
          ? `v${version.versionNumber} - ${version.rubricName}`
          : t('title')
      }
      actions={
        <div className="flex items-center gap-2">
          {version && <StatusBadge status={version.status} />}
          <Button
            variant="outline"
            onClick={() => router.push(`/rubrics/${rubricId}`)}
          >
            <ArrowLeft weight="bold" />
            {tCommon('back')}
          </Button>
        </div>
      }
    >
      {/* Criteria header */}
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">{t('criteria')}</h2>
        {isDraft && (
          <Button size="sm" onClick={openCreate}>
            <Plus weight="bold" />
            {t('addCriterion')}
          </Button>
        )}
      </div>

      {/* Criteria list */}
      {criteria.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-md border py-12 text-center">
          <p className="text-muted-foreground">{t('noRubrics')}</p>
          {isDraft && (
            <Button className="mt-4" onClick={openCreate}>
              <Plus weight="bold" />
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
                          <Warning weight="bold" className="size-3" />
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
                        <PencilSimple weight="bold" className="size-4" />
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
                          <Trash weight="bold" className="size-4" />
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
// Criteria Editor Panel (slide-in)
// ---------------------------------------------------------------------------

function CriteriaEditorPanel({
  versionId,
  criterion,
  onClose,
  nextSortOrder,
}: {
  versionId: string;
  criterion: CriterionResponse | null;
  onClose: () => void;
  nextSortOrder: number;
}) {
  const t = useTranslations('rubrics');
  const tCommon = useTranslations('common');
  const tErrors = useTranslations('errors');
  const queryClient = useQueryClient();

  const isEditMode = criterion !== null;
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    control,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<CreateCriterionFormValues>({
    resolver: zodResolver(createCriterionSchema),
    defaultValues: {
      name: criterion?.name ?? '',
      description: criterion?.description ?? '',
      weight: criterion?.weight ?? 50,
      judgeInstruction: criterion?.judgeInstruction ?? '',
      passCondition: criterion?.passCondition ?? '',
      failCondition: criterion?.failCondition ?? '',
      metricKey: criterion?.metricKey ?? '',
      isCritical: criterion?.isCritical ?? false,
      sortOrder: criterion?.sortOrder ?? nextSortOrder,
    },
  });

  const weightValue = useWatch({ control, name: 'weight' });

  async function onSubmit(values: CreateCriterionFormValues) {
    setServerError(null);

    try {
      if (isEditMode && criterion) {
        await apiClient.patch(
          `/api/v1/rubric-criteria/${criterion.publicId}`,
          values,
        );
      } else {
        await apiClient.post(
          `/api/v1/rubric-versions/${versionId}/criteria`,
          values,
        );
      }
      await queryClient.invalidateQueries({
        queryKey: ['rubric-criteria', versionId],
      });
      await queryClient.invalidateQueries({
        queryKey: ['rubric-version', versionId],
      });
      onClose();
    } catch (error: unknown) {
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
        setServerError(tErrors(key));
      } else {
        setServerError(tErrors('network'));
      }
    }
  }

  /* ---- Escape key ---- */
  React.useEffect(() => {
    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        e.stopPropagation();
        onClose();
      }
    }
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [onClose]);

  /* ---- Lock body scroll ---- */
  React.useEffect(() => {
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prev;
    };
  }, []);

  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={() => !isSubmitting && onClose()}
        aria-hidden="true"
      />

      {/* Slide-in panel */}
      <div
        role="dialog"
        aria-modal="true"
        className={cn(
          'relative z-10 flex h-full w-full max-w-lg flex-col border-l bg-card shadow-lg',
          'animate-in slide-in-from-right',
        )}
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b px-6 py-4">
          <h2 className="text-lg font-semibold">
            {isEditMode ? t('editCriterion') : t('addCriterion')}
          </h2>
          <Button
            variant="outline"
            size="icon"
            className="size-8"
            onClick={onClose}
          >
            <X weight="bold" className="size-4" />
          </Button>
        </div>

        {/* Form body */}
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="flex-1 space-y-4 overflow-y-auto px-6 py-4"
        >
          {serverError && (
            <div className="rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
              {serverError}
            </div>
          )}

          {/* Name */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-foreground">
              {t('criterionName')}
            </label>
            <input
              type="text"
              autoFocus
              disabled={isSubmitting}
              className={cn(
                inputClassName,
                errors.name && 'border-destructive focus-visible:ring-destructive',
              )}
              {...register('name')}
            />
            {errors.name && (
              <p className="text-sm text-destructive">{errors.name.message}</p>
            )}
          </div>

          {/* Description */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-foreground">
              {t('rubricDescription')}
            </label>
            <textarea
              disabled={isSubmitting}
              className={cn(
                textareaClassName,
                errors.description &&
                  'border-destructive focus-visible:ring-destructive',
              )}
              {...register('description')}
            />
          </div>

          {/* Weight — number input + range slider synced */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-foreground">
              {t('weight')}
            </label>
            <div className="flex items-center gap-3">
              <input
                type="number"
                min={1}
                max={100}
                disabled={isSubmitting}
                className={cn(inputClassName, 'w-20')}
                {...register('weight')}
              />
              <input
                type="range"
                min={1}
                max={100}
                value={typeof weightValue === 'number' ? weightValue : 50}
                onChange={(e) =>
                  setValue('weight', Number(e.target.value), {
                    shouldValidate: true,
                  })
                }
                disabled={isSubmitting}
                className="flex-1"
              />
              <span className="w-10 text-right text-sm text-muted-foreground">
                {typeof weightValue === 'number' ? weightValue : 50}
              </span>
            </div>
            {errors.weight && (
              <p className="text-sm text-destructive">{errors.weight.message}</p>
            )}
          </div>

          {/* Judge Instruction */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-foreground">
              {t('judgeInstruction')}
            </label>
            <textarea
              disabled={isSubmitting}
              rows={4}
              className={cn(
                textareaClassName,
                errors.judgeInstruction &&
                  'border-destructive focus-visible:ring-destructive',
              )}
              {...register('judgeInstruction')}
            />
            {errors.judgeInstruction && (
              <p className="text-sm text-destructive">
                {errors.judgeInstruction.message}
              </p>
            )}
          </div>

          {/* Pass Condition */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-foreground">
              {t('passCondition')}
            </label>
            <textarea
              disabled={isSubmitting}
              className={textareaClassName}
              {...register('passCondition')}
            />
          </div>

          {/* Fail Condition */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-foreground">
              {t('failCondition')}
            </label>
            <textarea
              disabled={isSubmitting}
              className={textareaClassName}
              {...register('failCondition')}
            />
          </div>

          {/* Metric Key */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-foreground">
              {t('metricKey')}
            </label>
            <input
              type="text"
              disabled={isSubmitting}
              className={cn(
                inputClassName,
                errors.metricKey &&
                  'border-destructive focus-visible:ring-destructive',
              )}
              {...register('metricKey')}
            />
            {errors.metricKey && (
              <p className="text-sm text-destructive">
                {errors.metricKey.message}
              </p>
            )}
          </div>

          {/* Sort Order */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-foreground">
              {t('sortOrder')}
            </label>
            <input
              type="number"
              min={0}
              disabled={isSubmitting}
              className={cn(
                inputClassName,
                'w-24',
                errors.sortOrder &&
                  'border-destructive focus-visible:ring-destructive',
              )}
              {...register('sortOrder')}
            />
            {errors.sortOrder && (
              <p className="text-sm text-destructive">
                {errors.sortOrder.message}
              </p>
            )}
          </div>

          {/* Is Critical toggle */}
          <div className="flex items-center gap-3">
            <Controller
              name="isCritical"
              control={control}
              render={({ field }) => (
                <button
                  type="button"
                  role="switch"
                  aria-checked={field.value}
                  disabled={isSubmitting}
                  onClick={() => field.onChange(!field.value)}
                  className={cn(
                    'relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors',
                    field.value ? 'bg-primary' : 'bg-input',
                    isSubmitting && 'cursor-not-allowed opacity-50',
                  )}
                >
                  <span
                    className={cn(
                      'pointer-events-none inline-block size-5 rounded-full bg-background shadow-lg ring-0 transition-transform',
                      field.value ? 'translate-x-5' : 'translate-x-0',
                    )}
                  />
                </button>
              )}
            />
            <label className="text-sm font-medium text-foreground">
              {t('isCritical')}
            </label>
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-3 border-t pt-4">
            <Button
              type="button"
              variant="outline"
              disabled={isSubmitting}
              onClick={onClose}
            >
              {tCommon('cancel')}
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? tCommon('loading') : tCommon('save')}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
