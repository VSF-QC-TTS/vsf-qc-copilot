'use client';

import * as React from 'react';
import { useState } from 'react';
import { useForm, Controller, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslations } from 'next-intl';
import { useQueryClient } from '@tanstack/react-query';
import { XIcon } from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { apiClient } from '@/lib/api/client';
import type { ApiError } from '@/lib/api/types';
import { getErrorMessageKey } from '@/lib/utils/error-messages';
import {
  createCriterionSchema,
  type CreateCriterionFormValues,
} from '@/lib/validations/rubric';

// ---------------------------------------------------------------------------
// Shared Types
// ---------------------------------------------------------------------------

export type CriterionResponse = {
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
// Local Styles
// ---------------------------------------------------------------------------

const inputClassName =
  'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

const textareaClassName =
  'flex min-h-[80px] w-full resize-none rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function CriteriaEditorPanel({
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
            <XIcon weight="bold" className="size-4" />
          </Button>
        </div>

        {/* Form body */}
        <form noValidate
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
