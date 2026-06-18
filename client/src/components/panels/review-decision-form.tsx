'use client';

import * as React from 'react';
import { useTranslations } from 'next-intl';
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { PencilSimpleIcon } from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { StatusBadge } from '@/components/ui/status-badge';
import { apiClient } from '@/lib/api/client';
import {
  reviewDecisionSchema,
  QC_STATUSES,
  type ReviewDecisionFormValues,
} from '@/lib/validations/evaluation';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type ReviewDecisionResponse = {
  publicId: string;
  qcStatus: string;
  qcNote: string | null;
  picBug: string | null;
  reviewedBy: {
    publicId: string;
    displayName: string;
  } | null;
  reviewedAt: string | null;
};

interface ReviewDecisionFormProps {
  resultPublicId: string;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function ReviewDecisionForm({ resultPublicId }: ReviewDecisionFormProps) {
  const t = useTranslations('qcReview');
  const tCommon = useTranslations('common');
  const queryClient = useQueryClient();

  const [isEditing, setIsEditing] = React.useState(false);
  const [submitError, setSubmitError] = React.useState<string | null>(null);

  // Fetch existing review decision
  const {
    data: existingReview,
    isLoading,
    error: fetchError,
  } = useQuery<ReviewDecisionResponse | null>({
    queryKey: ['review-decision', resultPublicId],
    queryFn: async () => {
      try {
        return await apiClient.get<ReviewDecisionResponse>(
          `/api/v1/evaluation-results/${resultPublicId}/review-decision`,
        );
      } catch (err: unknown) {
        // 404 means no review exists yet — that is expected
        if (
          err instanceof Object &&
          'status' in err &&
          (err as { status: number }).status === 404
        ) {
          return null;
        }
        throw err;
      }
    },
  });

  const hasExistingReview = existingReview !== null && existingReview !== undefined;
  const isCreateMode = !hasExistingReview;

  // Form
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ReviewDecisionFormValues>({
    resolver: zodResolver(reviewDecisionSchema),
    defaultValues: {
      qcStatus: undefined,
      qcNote: '',
      picBugUserPublicId: '',
    },
  });

  // Sync form values when existing review loads or enters edit mode
  React.useEffect(() => {
    if (hasExistingReview && isEditing) {
      const status = QC_STATUSES.find((s) => s === existingReview.qcStatus);
      reset({
        qcStatus: status,
        qcNote: existingReview.qcNote ?? '',
        picBugUserPublicId: '',
      });
    } else if (isCreateMode) {
      reset({
        qcStatus: undefined,
        qcNote: '',
        picBugUserPublicId: '',
      });
    }
  }, [hasExistingReview, isEditing, existingReview, isCreateMode, reset]);

  // Create mutation (PUT)
  const createMutation = useMutation({
    mutationFn: (values: ReviewDecisionFormValues) =>
      apiClient.put<ReviewDecisionResponse>(
        `/api/v1/evaluation-results/${resultPublicId}/review-decision`,
        {
          qcStatus: values.qcStatus,
          qcNote: values.qcNote || undefined,
          picBugUserPublicId: values.picBugUserPublicId || undefined,
        },
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['review-decision', resultPublicId],
      });
      void queryClient.invalidateQueries({ queryKey: ['evaluation-results'] });
      setSubmitError(null);
    },
    onError: (err: unknown) => {
      const msg =
        err instanceof Object && 'message' in err
          ? (err as { message: string }).message
          : 'Failed to save review';
      setSubmitError(msg);
    },
  });

  // Update mutation (PATCH)
  const updateMutation = useMutation({
    mutationFn: (values: ReviewDecisionFormValues) =>
      apiClient.patch<ReviewDecisionResponse>(
        `/api/v1/review-decisions/${existingReview?.publicId ?? ''}`,
        {
          qcStatus: values.qcStatus,
          qcNote: values.qcNote || undefined,
          picBugUserPublicId: values.picBugUserPublicId || undefined,
        },
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['review-decision', resultPublicId],
      });
      void queryClient.invalidateQueries({ queryKey: ['evaluation-results'] });
      setIsEditing(false);
      setSubmitError(null);
    },
    onError: (err: unknown) => {
      const msg =
        err instanceof Object && 'message' in err
          ? (err as { message: string }).message
          : 'Failed to update review';
      setSubmitError(msg);
    },
  });

  const isSaving = createMutation.isPending || updateMutation.isPending;

  const onSubmit = (values: ReviewDecisionFormValues) => {
    setSubmitError(null);
    if (isCreateMode) {
      createMutation.mutate(values);
    } else {
      updateMutation.mutate(values);
    }
  };

  // Loading state
  if (isLoading) {
    return (
      <div className="rounded-md border border-dashed p-4 text-center text-sm text-muted-foreground animate-pulse">
        {t('title')}...
      </div>
    );
  }

  // Fetch error
  if (fetchError) {
    return (
      <div className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
        {(fetchError as { message?: string }).message ?? 'Error loading review'}
      </div>
    );
  }

  // Read-only display (existing review, not editing)
  if (hasExistingReview && !isEditing) {
    return (
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <StatusBadge status={existingReview.qcStatus} size="sm" />
          <Button
            variant="ghost"
            size="sm"
            className="h-7 gap-1 text-xs"
            onClick={() => setIsEditing(true)}
          >
            <PencilSimpleIcon className="size-3.5" />
            {t('editReview')}
          </Button>
        </div>

        {existingReview.qcNote && (
          <div className="space-y-1">
            <span className="text-xs font-medium text-muted-foreground">
              {t('qcNote')}
            </span>
            <p className="text-sm whitespace-pre-wrap">{existingReview.qcNote}</p>
          </div>
        )}

        {existingReview.picBug && (
          <div className="space-y-1">
            <span className="text-xs font-medium text-muted-foreground">
              {t('picBug')}
            </span>
            <p className="text-sm">{existingReview.picBug}</p>
          </div>
        )}

        {/* Reviewer attribution */}
        {existingReview.reviewedBy && (
          <div className="flex items-center gap-4 text-xs text-muted-foreground pt-1 border-t">
            <span>
              {t('reviewedBy')}: {existingReview.reviewedBy.displayName}
            </span>
            {existingReview.reviewedAt && (
              <span>
                {t('reviewedAt')}: {formatDateTime(existingReview.reviewedAt)}
              </span>
            )}
          </div>
        )}
      </div>
    );
  }

  // Form (create or edit mode)
  return (
    <form noValidate onSubmit={handleSubmit(onSubmit)} className="space-y-3">
      {submitError && (
        <div className="rounded-md bg-red-50 p-2 text-xs text-red-800 dark:bg-red-950 dark:text-red-300">
          {submitError}
        </div>
      )}

      {/* QC Status select */}
      <div className="space-y-1">
        <label htmlFor="qcStatus" className="text-xs font-medium">
          {t('qcStatus')}
        </label>
        <select
          id="qcStatus"
          {...register('qcStatus')}
          disabled={isSaving}
          className={cn(
            'w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none',
            'focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50',
          )}
        >
          <option value="">{t('statusNotReviewed')}</option>
          {QC_STATUSES.map((status) => (
            <option key={status} value={status}>
              {t(`status${status.charAt(0)}${status.slice(1).toLowerCase().replace(/_./g, (m) => m[1].toUpperCase())}` as 'statusPass' | 'statusFail' | 'statusNeedFix' | 'statusIgnored')}
            </option>
          ))}
        </select>
        {errors.qcStatus && (
          <p className="text-xs text-red-600">{errors.qcStatus.message}</p>
        )}
      </div>

      {/* QC Note textarea */}
      <div className="space-y-1">
        <label htmlFor="qcNote" className="text-xs font-medium">
          {t('qcNote')}
        </label>
        <textarea
          id="qcNote"
          {...register('qcNote')}
          disabled={isSaving}
          rows={3}
          className={cn(
            'w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none resize-none',
            'focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50',
          )}
        />
        {errors.qcNote && (
          <p className="text-xs text-red-600">{errors.qcNote.message}</p>
        )}
      </div>

      {/* PIC Bug User Public ID */}
      <div className="space-y-1">
        <label htmlFor="picBugUserPublicId" className="text-xs font-medium">
          {t('picBug')}
        </label>
        <input
          id="picBugUserPublicId"
          type="text"
          {...register('picBugUserPublicId')}
          disabled={isSaving}
          className={cn(
            'w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none',
            'focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50',
          )}
        />
        {errors.picBugUserPublicId && (
          <p className="text-xs text-red-600">
            {errors.picBugUserPublicId.message}
          </p>
        )}
      </div>

      {/* Actions */}
      <div className="flex items-center justify-end gap-2 pt-1">
        {hasExistingReview && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            disabled={isSaving}
            onClick={() => setIsEditing(false)}
          >
            {tCommon('cancel')}
          </Button>
        )}
        <Button type="submit" size="sm" disabled={isSaving}>
          {isSaving && (
            <svg
              className="mr-1.5 size-3.5 animate-spin"
              viewBox="0 0 24 24"
              fill="none"
              aria-hidden="true"
            >
              <circle
                className="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeWidth="4"
              />
              <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
              />
            </svg>
          )}
          {isCreateMode ? t('saveReview') : t('updateReview')}
        </Button>
      </div>
    </form>
  );
}
