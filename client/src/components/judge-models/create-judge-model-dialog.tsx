'use client';

import * as React from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslations } from 'next-intl';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { apiClient } from '@/lib/api/client';
import type { JudgeModelResponse, JudgeProvider } from '@/lib/api/types';
import {
  createJudgeModelSchema,
  type CreateJudgeModelFormValues,
} from '@/lib/validations/judge-model';

// ---------------------------------------------------------------------------
// Props & Types
// ---------------------------------------------------------------------------

interface JudgeModelDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  projectId: string;
  initialData?: JudgeModelResponse | null;
}

const PROVIDERS: JudgeProvider[] = [
  'GEMINI',
  'OPENAI',
  'ANTHROPIC',
  'DEEPSEEK',
  'CUSTOM',
];
const DEFAULT_MODELS: Record<JudgeProvider, string> = {
  GEMINI: 'gemini-2.5-flash',
  OPENAI: 'gpt-4.1-mini',
  ANTHROPIC: 'claude-3-5-haiku-latest',
  DEEPSEEK: 'deepseek-chat',
  CUSTOM: '',
};

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

export function JudgeModelDialog({
  open,
  onOpenChange,
  projectId,
  initialData,
}: JudgeModelDialogProps) {
  const t = useTranslations('judgeModels');
  const tCommon = useTranslations('common');
  const queryClient = useQueryClient();

  const nameInputRef = React.useRef<HTMLInputElement | null>(null);
  const [advancedToggled, setAdvancedToggled] = React.useState(false);
  const [serverError, setServerError] = React.useState<string | null>(null);

  const isEdit = !!initialData;

  const {
    register,
    handleSubmit,
    reset,
    control,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<CreateJudgeModelFormValues>({
    resolver: zodResolver(createJudgeModelSchema),
    defaultValues: {
      name: '',
      provider: 'GEMINI',
      modelName: DEFAULT_MODELS.GEMINI,
      apiKey: '',
      active: true,
      baseUrl: '',
      configJson: '',
    },
  });

  // ---- Derive advanced section default from initialData ----
  const shouldShowAdvanced = !!initialData?.baseUrl || !!initialData?.configJson;

  // ---- Reset + autofocus on dialog open ----
  React.useEffect(() => {
    if (open) {
      reset(
        initialData
          ? {
              name: initialData.name,
              provider: initialData.provider,
              modelName: initialData.modelName,
              baseUrl: initialData.baseUrl ?? '',
              apiKey: '', // don't prefill masked api key
              configJson: initialData.configJson ?? '',
              active: initialData.active,
            }
          : {
              name: '',
              provider: 'GEMINI',
              modelName: DEFAULT_MODELS.GEMINI,
              apiKey: '',
              active: true,
              baseUrl: '',
              configJson: '',
            },
      );
      // Auto-focus name input after DOM settles
      requestAnimationFrame(() => {
        nameInputRef.current?.focus();
      });
    }
  }, [open, initialData, reset]);

  // ---- Auto-set default model name when provider changes ----
  const provider = useWatch({ control, name: 'provider' });
  React.useEffect(() => {
    const selectedProvider = provider as JudgeProvider;
    if (selectedProvider && DEFAULT_MODELS[selectedProvider]) {
      setValue('modelName', DEFAULT_MODELS[selectedProvider]);
    }
    if (selectedProvider !== 'CUSTOM') {
      setValue('baseUrl', '');
    }
  }, [provider, setValue]);

  // ---- Save mutation ----
  const saveMutation = useMutation({
    mutationFn: async (payload: CreateJudgeModelFormValues) => {
      // If editing, omit empty apiKey unless it was changed
      const body =
        initialData && !payload.apiKey
          ? { ...payload, apiKey: undefined }
          : payload;
      if (initialData) {
        return apiClient.patch<JudgeModelResponse>(
          `/api/v1/judge-models/${initialData.publicId}`,
          body,
        );
      }
      return apiClient.post<JudgeModelResponse>(
        `/api/v1/projects/${projectId}/judge-models`,
        body,
      );
    },
    onSuccess: async () => {
      setServerError(null);
      await queryClient.invalidateQueries({
        queryKey: ['judge-models', projectId],
      });
      await queryClient.invalidateQueries({
        queryKey: ['judge-models-active', projectId],
      });
      await queryClient.invalidateQueries({
        queryKey: ['project-readiness', projectId],
      });
      handleClose(false);
    },
    onError: (error: unknown) => {
      const message =
        typeof error === 'object' && error !== null && 'message' in error
          ? String((error as { message: unknown }).message)
          : t('saveFailed');
      setServerError(message);
    },
  });

  /* ---- Close helper ---- */
  const handleClose = React.useCallback(
    (nextOpen: boolean) => {
      if (!nextOpen) {
        reset();
        setServerError(null);
        setAdvancedToggled(false);
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

  /* ---- Form submit ---- */
  function onSubmit(values: CreateJudgeModelFormValues) {
    // Client-side: require apiKey on create
    if (!isEdit && (!values.apiKey || values.apiKey.trim() === '')) {
      // Manually set this since the schema allows optional for edit mode
      setServerError(t('fields.apiKey') + ' is required');
      return;
    }
    setServerError(null);
    saveMutation.mutate(values);
  }

  const isPending = isSubmitting || saveMutation.isPending;

  // ---- Connect register to ref for auto-focus ----
  const { ref: nameRegisterRef, ...nameRegisterRest } = register('name');

  if (!open) return null;

  return (
    <div
      data-slot="create-judge-model-dialog"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-0"
    >
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={() => !isPending && handleClose(false)}
        aria-hidden="true"
      />

      {/* Card */}
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-judge-model-title"
        className={cn(
          'relative z-10 w-full max-w-2xl rounded-lg border bg-card p-6 shadow-lg overflow-y-auto max-h-[90vh]',
          'animate-in fade-in-0 zoom-in-95',
        )}
      >
        <h2
          id="create-judge-model-title"
          className="text-lg font-semibold text-card-foreground"
        >
          {initialData ? t('editJudgeModel') : t('createJudgeModel')}
        </h2>

        <form noValidate onSubmit={handleSubmit(onSubmit)} className="mt-4 space-y-4">
          {serverError && (
            <div className="rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
              {serverError}
            </div>
          )}

          <div className="grid gap-4 sm:grid-cols-2">
            {/* Name */}
            <div className="space-y-2">
              <label htmlFor="judge-name" className="text-sm font-medium">
                {t('fields.name')}
              </label>
              <input
                id="judge-name"
                disabled={isPending}
                className={cn(
                  inputClassName,
                  errors.name &&
                    'border-destructive focus-visible:ring-destructive',
                )}
                ref={(el) => {
                  nameRegisterRef(el);
                  nameInputRef.current = el;
                }}
                {...nameRegisterRest}
              />
              {errors.name && (
                <p className="text-sm text-destructive">
                  {errors.name.message}
                </p>
              )}
            </div>

            {/* Provider */}
            <div className="space-y-2">
              <label htmlFor="judge-provider" className="text-sm font-medium">
                {t('fields.provider')}
              </label>
              <select
                id="judge-provider"
                disabled={isPending}
                className={cn(
                  inputClassName,
                  errors.provider &&
                    'border-destructive focus-visible:ring-destructive',
                )}
                {...register('provider')}
              >
                {PROVIDERS.map((p) => (
                  <option key={p} value={p}>
                    {p}
                  </option>
                ))}
              </select>
              {errors.provider && (
                <p className="text-sm text-destructive">
                  {errors.provider.message}
                </p>
              )}
            </div>

            {/* Model Name */}
            <div className="space-y-2">
              <label
                htmlFor="judge-model-name"
                className="text-sm font-medium"
              >
                {t('fields.modelName')}
              </label>
              <input
                id="judge-model-name"
                disabled={isPending}
                className={cn(
                  inputClassName,
                  errors.modelName &&
                    'border-destructive focus-visible:ring-destructive',
                )}
                {...register('modelName')}
              />
              {errors.modelName && (
                <p className="text-sm text-destructive">
                  {errors.modelName.message}
                </p>
              )}
            </div>

            {/* API Key */}
            <div className="space-y-2">
              <label htmlFor="judge-api-key" className="text-sm font-medium">
                {t('fields.apiKey')}
              </label>
              <input
                id="judge-api-key"
                type="password"
                disabled={isPending}
                placeholder={
                  initialData ? t('leaveBlankToKeep') : undefined
                }
                className={cn(
                  inputClassName,
                  errors.apiKey &&
                    'border-destructive focus-visible:ring-destructive',
                )}
                {...register('apiKey')}
              />
              {errors.apiKey && (
                <p className="text-sm text-destructive">
                  {errors.apiKey.message}
                </p>
              )}
            </div>

            {/* Active checkbox */}
            <label className="flex items-center gap-2 self-end pb-2 text-sm font-medium">
              <input
                type="checkbox"
                disabled={isPending}
                className="size-4 rounded border-input"
                {...register('active')}
              />
              {t('fields.active')}
            </label>

            {/* Advanced toggle */}
            <div className="sm:col-span-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setAdvancedToggled((current) => !current)}
              >
                {(shouldShowAdvanced || advancedToggled) ? t('hideAdvanced') : t('showAdvanced')}
              </Button>
            </div>

            {(shouldShowAdvanced || advancedToggled) && (
              <>
                {/* Base URL */}
                <div className="space-y-2">
                  <label
                    htmlFor="judge-base-url"
                    className="text-sm font-medium"
                  >
                    {t('fields.baseUrl')}
                  </label>
                  <input
                    id="judge-base-url"
                    disabled={isPending}
                    placeholder="https://api.openai.com/v1"
                    className={cn(
                      inputClassName,
                      errors.baseUrl &&
                        'border-destructive focus-visible:ring-destructive',
                    )}
                    {...register('baseUrl')}
                  />
                  {errors.baseUrl && (
                    <p className="text-sm text-destructive">
                      {errors.baseUrl.message}
                    </p>
                  )}
                </div>

                {/* Config JSON */}
                <div className="space-y-2">
                  <label
                    htmlFor="judge-config-json"
                    className="text-sm font-medium"
                  >
                    {t('fields.configJson')}
                  </label>
                  <textarea
                    id="judge-config-json"
                    disabled={isPending}
                    placeholder={'{"temperature": 0}'}
                    className={cn(
                      textareaClassName,
                      errors.configJson &&
                        'border-destructive focus-visible:ring-destructive',
                    )}
                    {...register('configJson')}
                  />
                  {errors.configJson && (
                    <p className="text-sm text-destructive">
                      {errors.configJson.message}
                    </p>
                  )}
                </div>
              </>
            )}
          </div>

          <div className="flex justify-end gap-3 pt-4">
            <Button
              type="button"
              variant="outline"
              disabled={isPending}
              onClick={() => handleClose(false)}
            >
              {tCommon('cancel')}
            </Button>
            <Button type="submit" disabled={isPending}>
              {isPending ? tCommon('saving') : tCommon('save')}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
