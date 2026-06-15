'use client';

import * as React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslations } from 'next-intl';
import { useQueryClient } from '@tanstack/react-query';
import {
  RobotIcon,
  CheckCircleIcon,
  XCircleIcon,
  CircleNotchIcon,
  HourglassIcon,
  InfoIcon,
} from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { apiClient } from '@/lib/api/client';
import type { ApiError } from '@/lib/api/types';
import { getErrorMessageKey } from '@/lib/utils/error-messages';
import {
  generateTestCasesSchema,
  type GenerateTestCasesFormValues,
} from '@/lib/validations/test-case';
import { useJobProgress } from '@/hooks/use-job-progress';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------


interface AIGenerateDialogProps {
  datasetId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

// ---------------------------------------------------------------------------
// Shared input styles
// ---------------------------------------------------------------------------

const inputClassName =
  'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';


const textareaClassName =
  'flex min-h-[80px] w-full resize-none rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function AIGenerateDialog({
  datasetId,
  open,
  onOpenChange,
}: AIGenerateDialogProps) {
  const t = useTranslations('testCases');
  const tCommon = useTranslations('common');
  const tErrors = useTranslations('errors');
  const queryClient = useQueryClient();

  const [serverError, setServerError] = React.useState<string | null>(null);
  const [jobPublicId, setJobPublicId] = React.useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<GenerateTestCasesFormValues>({
    resolver: zodResolver(generateTestCasesSchema),
    defaultValues: {
      prompt: '',
      count: 10,
      additionalPrompt: '',
    },
  });


  // ---------- Job progress ----------
  const {
    job,
    isPolling,
    isCompleted,
    isFailed,
    isTerminal,
  } = useJobProgress(jobPublicId, {
    onCompleted: () => {
      queryClient.invalidateQueries({
        queryKey: ['test-cases', datasetId],
      });
    },
  });

  const isJobActive = !!jobPublicId && !isTerminal;

  /* ---- Close helper ---- */
  const handleClose = React.useCallback(
    (nextOpen: boolean) => {
      if (!nextOpen && !isJobActive) {
        reset();
        setServerError(null);
        setJobPublicId(null);
      }
      if (!isJobActive) {
        onOpenChange(nextOpen);
      }
    },
    [onOpenChange, reset, isJobActive],
  );

  /* ---- Escape key ---- */
  React.useEffect(() => {
    if (!open) return;

    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape' && !isJobActive) {
        e.stopPropagation();
        handleClose(false);
      }
    }

    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [open, handleClose, isJobActive]);

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
  async function onSubmit(values: GenerateTestCasesFormValues) {
    setServerError(null);

    try {
      const result = await apiClient.post<{ jobPublicId: string }>(
        `/api/v1/datasets/${datasetId}/generate`,
        values,
      );
      setJobPublicId(result.jobPublicId);
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

  const showForm = !jobPublicId;
  const progressPercent =
    job && job.progressTotal > 0
      ? Math.round((job.progressCurrent / job.progressTotal) * 100)
      : 0;

  return (
    <div
      data-slot="ai-generate-dialog"
      className="fixed inset-0 z-50 flex items-center justify-center"
    >
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={() => !isSubmitting && !isJobActive && handleClose(false)}
        aria-hidden="true"
      />

      {/* Card */}
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="ai-generate-title"
        className={cn(
          'relative z-10 w-full max-w-md rounded-lg border bg-card p-6 shadow-lg',
          'animate-in fade-in-0 zoom-in-95',
        )}
      >
        <div className="flex items-center gap-2">
          <RobotIcon className="size-5 text-primary" />
          <h2
            id="ai-generate-title"
            className="text-lg font-semibold text-card-foreground"
          >
            {t('aiGenerateTitle')}
          </h2>
        </div>

        <div className="mt-4 space-y-4">
          {/* Helper hint */}
          <div className="flex gap-2 rounded-md border bg-blue-50/50 p-3 text-sm text-muted-foreground dark:border-blue-900/30 dark:bg-blue-950/20">
            <InfoIcon className="mt-0.5 size-4 shrink-0 text-blue-500" weight="fill" />
            <p>{t('aiGenerateHint')}</p>
          </div>

          {/* Server error */}
          {serverError && (
            <div className="rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
              {serverError}
            </div>
          )}

          {/* ---- Form ---- */}
          {showForm && (
            <form
              onSubmit={handleSubmit(onSubmit)}
              className="space-y-4"
            >
              {/* Prompt text area */}
              <div className="space-y-2">
                <label
                  htmlFor="gen-prompt"
                  className="text-sm font-medium leading-none text-foreground"
                >
                  {t('aiFieldPrompt')}{' '}
                  <span className="text-destructive">*</span>
                </label>
                <textarea
                  id="gen-prompt"
                  disabled={isSubmitting}
                  placeholder={t('aiFieldPromptPlaceholder')}
                  className={cn(
                    textareaClassName,
                    errors.prompt &&
                      'border-destructive focus-visible:ring-destructive',
                  )}
                  {...register('prompt')}
                />
                {errors.prompt && (
                  <p className="text-sm text-destructive">
                    {errors.prompt.message}
                  </p>
                )}
              </div>

              {/* Count */}
              <div className="space-y-2">
                <label
                  htmlFor="gen-count"
                  className="text-sm font-medium leading-none text-foreground"
                >
                  {t('aiFieldCount')}{' '}
                  <span className="text-destructive">*</span>
                </label>
                <input
                  id="gen-count"
                  type="number"
                  min={5}
                  max={100}
                  disabled={isSubmitting}
                  className={cn(
                    inputClassName,
                    errors.count &&
                      'border-destructive focus-visible:ring-destructive',
                  )}
                  {...register('count')}
                />
                <p className="text-xs text-muted-foreground">
                  {t('aiFieldCountHint')}
                </p>
                {errors.count && (
                  <p className="text-sm text-destructive">
                    {errors.count.message}
                  </p>
                )}
              </div>

              {/* Additional prompt */}
              <div className="space-y-2">
                <label
                  htmlFor="gen-additional-prompt"
                  className="text-sm font-medium leading-none text-foreground"
                >
                  {t('aiFieldAdditionalPrompt')}
                </label>
                <textarea
                  id="gen-additional-prompt"
                  disabled={isSubmitting}
                  placeholder={t('aiFieldAdditionalPromptPlaceholder')}
                  className={cn(
                    textareaClassName,
                    errors.additionalPrompt &&
                      'border-destructive focus-visible:ring-destructive',
                  )}
                  {...register('additionalPrompt')}
                />
                {errors.additionalPrompt && (
                  <p className="text-sm text-destructive">
                    {errors.additionalPrompt.message}
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
                  {isSubmitting ? t('aiGenerating') : t('aiGenerate')}
                </Button>
              </div>
            </form>
          )}

          {/* ---- Job progress ---- */}
          {jobPublicId && (
            <div className="space-y-4">
              {/* PENDING */}
              {job?.status === 'PENDING' && (
                <div className="flex items-center gap-3 rounded-md border bg-muted/50 px-4 py-4">
                  <HourglassIcon className="size-5 animate-pulse text-amber-500" />
                  <div>
                    <p className="text-sm font-medium">{t('jobPending')}</p>
                    <p className="text-xs text-muted-foreground">
                      {t('jobPendingHint')}
                    </p>
                  </div>
                </div>
              )}

              {/* RUNNING */}
              {job?.status === 'RUNNING' && (
                <div className="space-y-2 rounded-md border bg-muted/50 px-4 py-4">
                  <div className="flex items-center gap-3">
                    <CircleNotchIcon className="size-5 animate-spin text-primary" />
                    <div>
                      <p className="text-sm font-medium">{t('jobRunning')}</p>
                      <p className="text-xs text-muted-foreground">
                        {t('jobRunningHint')}
                      </p>
                    </div>
                  </div>
                  {/* Progress bar */}
                  <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
                    <div
                      className="h-full rounded-full bg-primary transition-all duration-300"
                      style={{ width: `${progressPercent}%` }}
                    />
                  </div>
                  <p className="text-right text-xs text-muted-foreground">
                    {progressPercent}%
                  </p>
                </div>
              )}

              {/* COMPLETED */}
              {isCompleted && (
                <div className="flex items-center gap-3 rounded-md border border-emerald-200 bg-emerald-50 px-4 py-4 dark:border-emerald-800 dark:bg-emerald-950">
                  <CheckCircleIcon className="size-5 text-emerald-600 dark:text-emerald-400" />
                  <div>
                    <p className="text-sm font-medium text-emerald-800 dark:text-emerald-300">
                      {t('jobCompleted')}
                    </p>
                    <p className="text-xs text-emerald-700 dark:text-emerald-400">
                      {t('jobCompletedHint')}
                    </p>
                  </div>
                </div>
              )}

              {/* FAILED */}
              {isFailed && (
                <div className="flex items-center gap-3 rounded-md border border-destructive/50 bg-destructive/10 px-4 py-4">
                  <XCircleIcon className="size-5 text-destructive" />
                  <div>
                    <p className="text-sm font-medium text-destructive">
                      {t('jobFailed')}
                    </p>
                    {job?.errorMessage && (
                      <p className="text-xs text-destructive/80">
                        {job.errorMessage}
                      </p>
                    )}
                  </div>
                </div>
              )}

              {/* Loading / initial polling */}
              {!job && isPolling && (
                <div className="flex items-center justify-center gap-2 py-4">
                  <CircleNotchIcon className="size-5 animate-spin text-muted-foreground" />
                  <p className="text-sm text-muted-foreground">
                    {tCommon('loading')}
                  </p>
                </div>
              )}

              {/* Close button (only when terminal) */}
              {isTerminal && (
                <div className="flex justify-end pt-2">
                  <Button
                    type="button"
                    onClick={() => handleClose(false)}
                  >
                    {tCommon('close')}
                  </Button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
