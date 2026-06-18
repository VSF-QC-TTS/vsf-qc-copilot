'use client';

import * as React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslations } from 'next-intl';
import { useQueryClient } from '@tanstack/react-query';
import {
  SparkleIcon,
  CaretDownIcon,
  CircleNotchIcon,
} from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { apiClient } from '@/lib/api/client';
import type { ApiError } from '@/lib/api/types';
import { getErrorMessageKey } from '@/lib/utils/error-messages';
import {
  generateRubricPreviewSchema,
  type GenerateRubricPreviewFormValues,
} from '@/lib/validations/rubric';
import { useRouter } from '@/i18n/navigation';

interface CreateRubricDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

type CreateRubricResponse = {
  rubricPublicId: string;
  publicId: string;
};

const inputClassName =
  'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

const textareaClassName =
  'flex min-h-[120px] w-full resize-none rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

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
  } = useForm<GenerateRubricPreviewFormValues>({
    resolver: zodResolver(generateRubricPreviewSchema),
    defaultValues: {
      name: '',
      evaluationGoal: '',
      domainContext: '',
      language: 'vi',
      sampleQuestion: '',
      sampleExpectedAnswer: '',
      extraInstructions: '',
    },
  });

  const handleClose = React.useCallback(
    (nextOpen: boolean) => {
      if (!nextOpen && !isSubmitting) {
        reset();
        setServerError(null);
      }
      if (!isSubmitting) {
        onOpenChange(nextOpen);
      }
    },
    [onOpenChange, reset, isSubmitting],
  );

  React.useEffect(() => {
    if (!open) return;
    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape' && !isSubmitting) {
        e.stopPropagation();
        handleClose(false);
      }
    }
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [open, handleClose, isSubmitting]);

  React.useEffect(() => {
    if (!open) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prev;
    };
  }, [open]);

  async function onSubmit(values: GenerateRubricPreviewFormValues) {
    setServerError(null);
    try {
      const result = await apiClient.post<CreateRubricResponse>(
        '/api/v1/rubrics/generate',
        values,
      );
      await queryClient.invalidateQueries({ queryKey: ['rubrics'] });
      handleClose(false);
      router.push(
        `/rubrics/${result.rubricPublicId}/versions/${result.publicId}`,
      );
    } catch (error: unknown) {
      setServerError(errorMessage(error, tErrors));
    }
  }

  if (!open) return null;

  return (
    <div
      data-slot="create-rubric-dialog"
      className="fixed inset-0 z-50 flex items-center justify-center"
    >
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={() => !isSubmitting && handleClose(false)}
        aria-hidden="true"
      />

      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-rubric-title"
        className={cn(
          'relative z-10 w-full max-w-lg rounded-lg border bg-card p-6 shadow-lg',
          'animate-in fade-in-0 zoom-in-95',
        )}
      >
        <div className="flex items-center gap-2">
          <SparkleIcon weight="fill" className="size-5 text-primary" />
          <h2
            id="create-rubric-title"
            className="text-lg font-semibold text-card-foreground"
          >
            {t('createRubricTitle')}
          </h2>
        </div>

        {serverError && (
          <div className="mt-4 rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
            {serverError}
          </div>
        )}

        <form noValidate onSubmit={handleSubmit(onSubmit)} className="mt-4 space-y-4">
          {/* Rubric Name */}
          <div className="space-y-2">
            <label htmlFor="rubric-name" className="text-sm font-medium">
              {t('rubricName')} <span className="text-destructive">*</span>
            </label>
            <input
              id="rubric-name"
              autoFocus
              disabled={isSubmitting}
              placeholder={t('rubricNamePlaceholder')}
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

          {/* Evaluation Goal */}
          <div className="space-y-2">
            <label htmlFor="evaluation-goal" className="text-sm font-medium">
              {t('evaluationGoal')} <span className="text-destructive">*</span>
            </label>
            <textarea
              id="evaluation-goal"
              disabled={isSubmitting}
              className={cn(
                textareaClassName,
                errors.evaluationGoal &&
                  'border-destructive focus-visible:ring-destructive',
              )}
              placeholder={t('evaluationGoalPlaceholder')}
              {...register('evaluationGoal')}
            />
            {errors.evaluationGoal && (
              <p className="text-sm text-destructive">
                {errors.evaluationGoal.message}
              </p>
            )}
          </div>

          {/* Advanced Settings */}
          <details className="group rounded-md border border-input/60 bg-muted/10 p-3 select-none">
            <summary className="flex cursor-pointer items-center justify-between text-sm font-medium focus-visible:outline-none focus:outline-none [&::-webkit-details-marker]:hidden">
              <span>{t('advancedSettings')}</span>
              <CaretDownIcon className="size-4 transition-transform group-open:rotate-180 text-muted-foreground" weight="bold" />
            </summary>
            <div className="mt-3 space-y-4 pt-1">
              <div className="space-y-2">
                <label htmlFor="rubric-language" className="text-sm font-medium">
                  {t('language')}
                </label>
                <input
                  id="rubric-language"
                  disabled={isSubmitting}
                  className={inputClassName}
                  {...register('language')}
                />
              </div>

              <div className="space-y-2">
                <label htmlFor="domain-context" className="text-sm font-medium">
                  {t('domainContext')}
                </label>
                <textarea
                  id="domain-context"
                  disabled={isSubmitting}
                  className={textareaClassName}
                  placeholder={t('domainContextPlaceholder')}
                  {...register('domainContext')}
                />
              </div>

              <div className="grid gap-4 lg:grid-cols-2">
                <div className="space-y-2">
                  <label htmlFor="sample-question" className="text-sm font-medium">
                    {t('sampleQuestion')}
                  </label>
                  <textarea
                    id="sample-question"
                    disabled={isSubmitting}
                    className={textareaClassName}
                    {...register('sampleQuestion')}
                  />
                </div>
                <div className="space-y-2">
                  <label htmlFor="sample-answer" className="text-sm font-medium">
                    {t('sampleExpectedAnswer')}
                  </label>
                  <textarea
                    id="sample-answer"
                    disabled={isSubmitting}
                    className={textareaClassName}
                    {...register('sampleExpectedAnswer')}
                  />
                </div>
              </div>

              <div className="space-y-2">
                <label htmlFor="extra-instructions" className="text-sm font-medium">
                  {t('extraInstructions')}
                </label>
                <textarea
                  id="extra-instructions"
                  disabled={isSubmitting}
                  className={textareaClassName}
                  {...register('extraInstructions')}
                />
              </div>
            </div>
          </details>

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
              {isSubmitting ? (
                <>
                  <CircleNotchIcon className="mr-2 size-4 animate-spin" />
                  {t('generatingRubric')}
                </>
              ) : (
                <>
                  <SparkleIcon weight="fill" className="mr-2 size-4" />
                  {t('aiGenerate')}
                </>
              )}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
