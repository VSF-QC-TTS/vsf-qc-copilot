'use client';

import * as React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslations } from 'next-intl';
import { useQuery, useQueryClient } from '@tanstack/react-query';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { apiClient } from '@/lib/api/client';
import type { ApiError, PageResponse, ProjectResponse } from '@/lib/api/types';
import { getErrorMessageKey } from '@/lib/utils/error-messages';
import {
  createRubricSchema,
  type CreateRubricFormValues,
} from '@/lib/validations/rubric';
import { useRouter } from '@/i18n/navigation';

// ---------------------------------------------------------------------------
// Props
// ---------------------------------------------------------------------------

interface CreateRubricDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

// ---------------------------------------------------------------------------
// Shared input styles (matches create-dataset-dialog pattern)
// ---------------------------------------------------------------------------

const inputClassName =
  'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

const textareaClassName =
  'flex min-h-[80px] w-full resize-none rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

const selectClassName =
  'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function CreateRubricDialog({
  open,
  onOpenChange,
}: CreateRubricDialogProps) {
  const t = useTranslations('rubrics');
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
  } = useForm<CreateRubricFormValues>({
    resolver: zodResolver(createRubricSchema),
    defaultValues: {
      name: '',
      description: '',
      projectPublicId: '',
    },
  });

  // --- Fetch active projects for select ---
  const { data: projectsData } = useQuery({
    queryKey: ['projects', 'active-for-rubric'],
    queryFn: () =>
      apiClient.get<PageResponse<ProjectResponse>>(
        '/api/v1/projects?page=0&size=100&status=ACTIVE&sort=name,asc',
      ),
    enabled: open,
  });

  const projects = projectsData?.items ?? [];

  /* ---- Close helper ---- */
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
  async function onSubmit(values: CreateRubricFormValues) {
    setServerError(null);

    try {
      const { projectPublicId, ...body } = values;
      const result = await apiClient.post<{ publicId: string }>(
        `/api/v1/projects/${projectPublicId}/rubrics`,
        body,
      );
      await queryClient.invalidateQueries({ queryKey: ['rubrics'] });
      handleClose(false);
      router.push(`/rubrics/${result.publicId}`);
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
      data-slot="create-rubric-dialog"
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
        aria-labelledby="create-rubric-title"
        className={cn(
          'relative z-10 w-full max-w-md rounded-lg border bg-card p-6 shadow-lg',
          'animate-in fade-in-0 zoom-in-95',
        )}
      >
        <h2
          id="create-rubric-title"
          className="text-lg font-semibold text-card-foreground"
        >
          {t('createRubricTitle')}
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

          {/* Project select */}
          <div className="space-y-2">
            <label
              htmlFor="rubric-project"
              className="text-sm font-medium leading-none text-foreground"
            >
              {t('project')}
            </label>
            <select
              id="rubric-project"
              disabled={isSubmitting}
              className={cn(
                selectClassName,
                errors.projectPublicId &&
                  'border-destructive focus-visible:ring-destructive',
              )}
              {...register('projectPublicId')}
            >
              <option value="">{t('selectProject')}</option>
              {projects.map((p) => (
                <option key={p.publicId} value={p.publicId}>
                  {p.name}
                </option>
              ))}
            </select>
            {errors.projectPublicId && (
              <p className="text-sm text-destructive">
                {errors.projectPublicId.message}
              </p>
            )}
          </div>

          {/* Name field */}
          <div className="space-y-2">
            <label
              htmlFor="rubric-name"
              className="text-sm font-medium leading-none text-foreground"
            >
              {t('rubricName')}
            </label>
            <input
              id="rubric-name"
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

          {/* Description field */}
          <div className="space-y-2">
            <label
              htmlFor="rubric-description"
              className="text-sm font-medium leading-none text-foreground"
            >
              {t('rubricDescription')}
            </label>
            <textarea
              id="rubric-description"
              disabled={isSubmitting}
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
              {isSubmitting ? tCommon('loading') : tCommon('create')}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
