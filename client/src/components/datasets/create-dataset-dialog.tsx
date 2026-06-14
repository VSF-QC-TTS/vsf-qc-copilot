'use client';

import * as React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslations } from 'next-intl';
import { useQueryClient } from '@tanstack/react-query';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { apiClient } from '@/lib/api/client';
import type { ApiError } from '@/lib/api/types';
import { getErrorMessageKey } from '@/lib/utils/error-messages';
import {
  createDatasetSchema,
  type CreateDatasetFormValues,
} from '@/lib/validations/dataset';
import { useRouter } from '@/i18n/navigation';

// ---------------------------------------------------------------------------
// Props
// ---------------------------------------------------------------------------

interface CreateDatasetDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  projectId: string;
}


// ---------------------------------------------------------------------------
// Shared input styles (matches login page pattern)
// ---------------------------------------------------------------------------

const inputClassName =
  'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

const textareaClassName =
  'flex min-h-[80px] w-full resize-none rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function CreateDatasetDialog({
  open,
  onOpenChange,
  projectId,
}: CreateDatasetDialogProps) {
  const t = useTranslations('datasets');
  const tCommon = useTranslations('common');
  const tErrors = useTranslations('errors');
  const router = useRouter();
  const queryClient = useQueryClient();

  const [serverError, setServerError] = React.useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateDatasetFormValues>({
    resolver: zodResolver(createDatasetSchema),
    defaultValues: {
      name: '',
      description: '',
    },
  });


  /* ---- Close helper: resets form + error ---- */
  const handleClose = React.useCallback(
    (nextOpen: boolean) => {
      if (!nextOpen) {
        reset();
        setServerError(null);
      }
      onOpenChange(nextOpen);
    },
    [onOpenChange, reset],
  );

  /* ---- Escape key ---- */
  React.useEffect(() => {
    if (!open) return;

    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        e.stopPropagation();
        handleClose(false);
      }
    }

    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [open, handleClose]);

  /* ---- Lock body scroll ---- */
  React.useEffect(() => {
    if (!open) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prev;
    };
  }, [open]);

  /* ---- Submit ---- */
  async function onSubmit(values: CreateDatasetFormValues) {
    setServerError(null);

    try {
      const result = await apiClient.post<{ publicId: string }>(
        '/api/v1/projects/' + projectId + '/datasets',
        { ...values, sourceType: 'MANUAL' },
      );
      await queryClient.invalidateQueries({ queryKey: ['datasets'] });
      handleClose(false);
      router.push(`/projects/${projectId}/datasets/${result.publicId}`);
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

  if (!open) return null;

  return (
    <div
      data-slot="create-dataset-dialog"
      className="fixed inset-0 z-50 flex items-center justify-center"
    >
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={() => !isSubmitting && handleClose(false)}
        aria-hidden="true"
      />

      {/* Card */}
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-dataset-title"
        className={cn(
          'relative z-10 w-full max-w-md rounded-lg border bg-card p-6 shadow-lg',
          'animate-in fade-in-0 zoom-in-95',
        )}
      >
        <h2
          id="create-dataset-title"
          className="text-lg font-semibold text-card-foreground"
        >
          {t('createDatasetTitle')}
        </h2>

        <form
          onSubmit={handleSubmit(onSubmit)}
          className="mt-4 space-y-4"
        >
          {serverError && (
            <div className="rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
              {serverError}
            </div>
          )}

          {/* Name field */}
          <div className="space-y-2">
            <label
              htmlFor="dataset-name"
              className="text-sm font-medium leading-none text-foreground"
            >
              {t('datasetName')}
            </label>
            <input
              id="dataset-name"
              type="text"
              autoFocus
              disabled={isSubmitting}
              placeholder={t('datasetNamePlaceholder')}
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

          {/* Description field */}
          <div className="space-y-2">
            <label
              htmlFor="dataset-description"
              className="text-sm font-medium leading-none text-foreground"
            >
              {t('datasetDescription')}
            </label>
            <textarea
              id="dataset-description"
              disabled={isSubmitting}
              placeholder={t('datasetDescriptionPlaceholder')}
              className={cn(
                textareaClassName,
                errors.description &&
                  'border-destructive focus-visible:ring-destructive',
              )}
              {...register('description')}
            />
            {errors.description && (
              <p className="text-sm text-destructive">
                {errors.description.message}
              </p>
            )}
          </div>


          {/* Actions */}
          <div className="flex justify-end gap-3 pt-2">
            <Button
              type="button"
              variant="outline"
              disabled={isSubmitting}
              onClick={() => handleClose(false)}
            >
              {tCommon('cancel')}
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? t('creating') : t('createDataset')}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
