'use client';

import * as React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslations } from 'next-intl';
import { useQueryClient } from '@tanstack/react-query';
import { ArrowLeftIcon, RobotIcon } from '@phosphor-icons/react';

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

type PreviewCriterion = {
  name: string;
  description: string | null;
  weight: number;
  passCondition: string | null;
  failCondition: string | null;
  judgeInstruction: string;
  metricKey: string;
  isCritical: boolean;
  sortOrder: number;
};

type RubricPreview = {
  name: string;
  description: string | null;
  content: string;
  outputSchemaJson: string | null;
  criteria: PreviewCriterion[];
};

type CreateRubricResponse = {
  rubricPublicId: string;
  publicId: string;
};

const inputClassName =
  'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

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
  const [preview, setPreview] = React.useState<RubricPreview | null>(null);
  const [isGenerating, setIsGenerating] = React.useState(false);
  const [isCreating, setIsCreating] = React.useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
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

  const isBusy = isGenerating || isCreating;

  const handleClose = React.useCallback(
    (nextOpen: boolean) => {
      if (!nextOpen) {
        reset();
        setPreview(null);
        setServerError(null);
        setIsGenerating(false);
        setIsCreating(false);
      }
      onOpenChange(nextOpen);
    },
    [onOpenChange, reset],
  );

  React.useEffect(() => {
    if (!open) return;
    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape' && !isBusy) {
        e.stopPropagation();
        handleClose(false);
      }
    }
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [open, handleClose, isBusy]);

  React.useEffect(() => {
    if (!open) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prev;
    };
  }, [open]);

  async function onGenerate(values: GenerateRubricPreviewFormValues) {
    setServerError(null);
    setIsGenerating(true);
    try {
      const result = await apiClient.post<RubricPreview>(
        '/api/v1/rubrics/generate-preview',
        values,
      );
      setPreview(result);
    } catch (error: unknown) {
      setServerError(errorMessage(error, tErrors));
    } finally {
      setIsGenerating(false);
    }
  }

  async function handleCreate() {
    if (!preview) return;
    setServerError(null);
    setIsCreating(true);
    try {
      const result = await apiClient.post<CreateRubricResponse>(
        '/api/v1/rubrics',
        preview,
      );
      await queryClient.invalidateQueries({ queryKey: ['rubrics'] });
      await queryClient.invalidateQueries({ queryKey: ['rubric-versions-published'] });
      handleClose(false);
      router.push(`/rubrics/${result.rubricPublicId}/versions/${result.publicId}`);
    } catch (error: unknown) {
      setServerError(errorMessage(error, tErrors));
      setIsCreating(false);
    }
  }

  function updatePreview<K extends keyof RubricPreview>(
    key: K,
    value: RubricPreview[K],
  ) {
    setPreview((current) => (current ? { ...current, [key]: value } : current));
  }

  function updateCriterion<K extends keyof PreviewCriterion>(
    index: number,
    key: K,
    value: PreviewCriterion[K],
  ) {
    setPreview((current) => {
      if (!current) return current;
      return {
        ...current,
        criteria: current.criteria.map((criterion, idx) =>
          idx === index ? { ...criterion, [key]: value } : criterion,
        ),
      };
    });
  }

  if (!open) return null;

  return (
    <div
      data-slot="create-rubric-dialog"
      className="fixed inset-0 z-50 flex items-center justify-center"
    >
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={() => !isBusy && handleClose(false)}
        aria-hidden="true"
      />

      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-rubric-title"
        className={cn(
          'relative z-10 max-h-[90vh] w-full max-w-4xl overflow-y-auto rounded-lg border bg-card p-6 shadow-lg',
          'animate-in fade-in-0 zoom-in-95',
        )}
      >
        <div className="flex items-center gap-2">
          <RobotIcon className="size-5 text-primary" weight="duotone" />
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

        {preview ? (
          <div className="mt-4 space-y-5">
            <div className="grid gap-4 lg:grid-cols-2">
              <div className="space-y-2">
                <label htmlFor="preview-name" className="text-sm font-medium">
                  {t('rubricName')}
                </label>
                <input
                  id="preview-name"
                  value={preview.name}
                  disabled={isBusy}
                  onChange={(event) => updatePreview('name', event.target.value)}
                  className={inputClassName}
                />
              </div>
              <div className="space-y-2">
                <label htmlFor="preview-description" className="text-sm font-medium">
                  {t('rubricDescription')}
                </label>
                <input
                  id="preview-description"
                  value={preview.description ?? ''}
                  disabled={isBusy}
                  onChange={(event) =>
                    updatePreview('description', event.target.value)
                  }
                  className={inputClassName}
                />
              </div>
            </div>

            <div className="space-y-2">
              <label htmlFor="preview-content" className="text-sm font-medium">
                {t('rubricContent')}
              </label>
              <textarea
                id="preview-content"
                value={preview.content}
                disabled={isBusy}
                onChange={(event) => updatePreview('content', event.target.value)}
                className={cn(textareaClassName, 'min-h-[140px]')}
              />
            </div>

            <div className="space-y-3">
              <h3 className="text-sm font-semibold">{t('criteria')}</h3>
              {preview.criteria.map((criterion, index) => (
                <div
                  key={`${criterion.metricKey}-${index}`}
                  className="grid gap-3 rounded-md border bg-background p-3 lg:grid-cols-[1fr_96px]"
                >
                  <div className="grid gap-3 lg:grid-cols-2">
                    <input
                      value={criterion.name}
                      disabled={isBusy}
                      onChange={(event) =>
                        updateCriterion(index, 'name', event.target.value)
                      }
                      className={inputClassName}
                      aria-label={t('criterionName')}
                    />
                    <input
                      value={criterion.metricKey}
                      disabled={isBusy}
                      onChange={(event) =>
                        updateCriterion(index, 'metricKey', event.target.value)
                      }
                      className={inputClassName}
                      aria-label={t('metricKey')}
                    />
                    <textarea
                      value={criterion.judgeInstruction}
                      disabled={isBusy}
                      onChange={(event) =>
                        updateCriterion(index, 'judgeInstruction', event.target.value)
                      }
                      className={textareaClassName}
                      aria-label={t('judgeInstruction')}
                    />
                    <textarea
                      value={criterion.passCondition ?? ''}
                      disabled={isBusy}
                      onChange={(event) =>
                        updateCriterion(index, 'passCondition', event.target.value)
                      }
                      className={textareaClassName}
                      aria-label={t('passCondition')}
                    />
                  </div>
                  <div className="space-y-3">
                    <label className="block text-xs font-medium text-muted-foreground">
                      {t('weight')}
                      <input
                        type="number"
                        min={1}
                        max={100}
                        value={criterion.weight}
                        disabled={isBusy}
                        onChange={(event) =>
                          updateCriterion(
                            index,
                            'weight',
                            Number(event.target.value),
                          )
                        }
                        className={cn(inputClassName, 'mt-1')}
                      />
                    </label>
                    <label className="flex items-center gap-2 text-sm">
                      <input
                        type="checkbox"
                        checked={criterion.isCritical}
                        disabled={isBusy}
                        onChange={(event) =>
                          updateCriterion(index, 'isCritical', event.target.checked)
                        }
                        className="size-4 rounded border-input"
                      />
                      {t('isCritical')}
                    </label>
                  </div>
                </div>
              ))}
            </div>

            <details className="rounded-md border bg-background p-3">
              <summary className="cursor-pointer text-sm font-medium">
                {t('outputSchema')}
              </summary>
              <textarea
                value={preview.outputSchemaJson ?? ''}
                disabled={isBusy}
                onChange={(event) =>
                  updatePreview('outputSchemaJson', event.target.value)
                }
                className={cn(textareaClassName, 'mt-3 font-mono')}
              />
            </details>

            <div className="flex justify-between gap-3 pt-2">
              <Button
                type="button"
                variant="outline"
                disabled={isBusy}
                onClick={() => setPreview(null)}
              >
                <ArrowLeftIcon weight="bold" />
                {tCommon('back')}
              </Button>
              <div className="flex gap-3">
                <Button
                  type="button"
                  variant="outline"
                  disabled={isBusy}
                  onClick={() => handleClose(false)}
                >
                  {tCommon('cancel')}
                </Button>
                <Button type="button" disabled={isBusy} onClick={handleCreate}>
                  {isCreating ? tCommon('loading') : tCommon('create')}
                </Button>
              </div>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit(onGenerate)} className="mt-4 space-y-4">
            <div className="grid gap-4 lg:grid-cols-2">
              <div className="space-y-2">
                <label htmlFor="rubric-name" className="text-sm font-medium">
                  {t('rubricName')}
                </label>
                <input
                  id="rubric-name"
                  autoFocus
                  disabled={isGenerating}
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

              <div className="space-y-2">
                <label htmlFor="rubric-language" className="text-sm font-medium">
                  {t('language')}
                </label>
                <input
                  id="rubric-language"
                  disabled={isGenerating}
                  className={inputClassName}
                  {...register('language')}
                />
              </div>
            </div>

            <div className="space-y-2">
              <label htmlFor="evaluation-goal" className="text-sm font-medium">
                {t('evaluationGoal')}
              </label>
              <textarea
                id="evaluation-goal"
                disabled={isGenerating}
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

            <div className="space-y-2">
              <label htmlFor="domain-context" className="text-sm font-medium">
                {t('domainContext')}
              </label>
              <textarea
                id="domain-context"
                disabled={isGenerating}
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
                  disabled={isGenerating}
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
                  disabled={isGenerating}
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
                disabled={isGenerating}
                className={textareaClassName}
                {...register('extraInstructions')}
              />
            </div>

            <div className="flex justify-end gap-3 pt-2">
              <Button
                type="button"
                variant="outline"
                disabled={isGenerating}
                onClick={() => handleClose(false)}
              >
                {tCommon('cancel')}
              </Button>
              <Button type="submit" disabled={isGenerating}>
                <RobotIcon weight="bold" />
                {isGenerating ? tCommon('loading') : t('generatePreview')}
              </Button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
