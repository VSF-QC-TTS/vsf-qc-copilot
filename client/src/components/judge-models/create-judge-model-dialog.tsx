'use client';

import * as React from 'react';
import { useTranslations } from 'next-intl';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { apiClient } from '@/lib/api/client';
import type { JudgeModelResponse, JudgeProvider } from '@/lib/api/types';

// ---------------------------------------------------------------------------
// Props & Types
// ---------------------------------------------------------------------------

interface JudgeModelDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  projectId: string;
  initialData?: JudgeModelResponse | null;
}

type CreateJudgeModelPayload = {
  name: string;
  provider: JudgeProvider;
  modelName: string;
  baseUrl: string;
  apiKey: string;
  configJson: string;
  active: boolean;
};

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

function initialPayload(): CreateJudgeModelPayload {
  return {
    name: '',
    provider: 'GEMINI',
    modelName: DEFAULT_MODELS.GEMINI,
    baseUrl: '',
    apiKey: '',
    configJson: '',
    active: true,
  };
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

export function JudgeModelDialog({
  open,
  onOpenChange,
  projectId,
  initialData,
}: JudgeModelDialogProps) {
  const t = useTranslations('judgeModels');
  const tCommon = useTranslations('common');
  const queryClient = useQueryClient();

  const [form, setForm] = React.useState<CreateJudgeModelPayload>(() =>
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
      : initialPayload(),
  );
  
  React.useEffect(() => {
    if (open) {
      const timer = setTimeout(() => {
        setForm(
          initialData
            ? {
                name: initialData.name,
                provider: initialData.provider,
                modelName: initialData.modelName,
                baseUrl: initialData.baseUrl ?? '',
                apiKey: '',
                configJson: initialData.configJson ?? '',
                active: initialData.active,
              }
            : initialPayload(),
        );
      }, 0);
      return () => clearTimeout(timer);
    }
  }, [open, initialData]);

  const [advancedOpen, setAdvancedOpen] = React.useState(!!initialData?.baseUrl || !!initialData?.configJson);
  const [serverError, setServerError] = React.useState<string | null>(null);

  const saveMutation = useMutation({
    mutationFn: (payload: CreateJudgeModelPayload) => {
      // If editing, omit empty apiKey unless it was changed
      const body = initialData && !payload.apiKey ? { ...payload, apiKey: undefined } : payload;
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
        setForm(initialPayload());
        setServerError(null);
        setAdvancedOpen(false);
      }
      onOpenChange(nextOpen);
    },
    [onOpenChange],
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

  function updateField<K extends keyof CreateJudgeModelPayload>(
    key: K,
    value: CreateJudgeModelPayload[K],
  ) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function handleProviderChange(provider: JudgeProvider) {
    setForm((current) => ({
      ...current,
      provider,
      modelName: DEFAULT_MODELS[provider] || current.modelName,
      baseUrl: provider === 'CUSTOM' ? current.baseUrl : '',
    }));
  }

  function handleSubmit(event: React.SyntheticEvent) {
    event.preventDefault();
    setServerError(null);
    saveMutation.mutate(form);
  }

  if (!open) return null;

  return (
    <div
      data-slot="create-judge-model-dialog"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-0"
    >
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={() => !saveMutation.isPending && handleClose(false)}
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

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          {serverError && (
            <div className="rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
              {serverError}
            </div>
          )}

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <label htmlFor="judge-name" className="text-sm font-medium">
                {t('fields.name')}
              </label>
              <input
                id="judge-name"
                required
                disabled={saveMutation.isPending}
                value={form.name}
                onChange={(event) => updateField('name', event.target.value)}
                className={inputClassName}
              />
            </div>

            <div className="space-y-2">
              <label htmlFor="judge-provider" className="text-sm font-medium">
                {t('fields.provider')}
              </label>
              <select
                id="judge-provider"
                disabled={saveMutation.isPending}
                value={form.provider}
                onChange={(event) =>
                  handleProviderChange(event.target.value as JudgeProvider)
                }
                className={inputClassName}
              >
                {PROVIDERS.map((provider) => (
                  <option key={provider} value={provider}>
                    {provider}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-2">
              <label htmlFor="judge-model-name" className="text-sm font-medium">
                {t('fields.modelName')}
              </label>
              <input
                id="judge-model-name"
                required
                disabled={saveMutation.isPending}
                value={form.modelName}
                onChange={(event) => updateField('modelName', event.target.value)}
                className={inputClassName}
              />
            </div>

            <div className="space-y-2">
              <label htmlFor="judge-api-key" className="text-sm font-medium">
                {t('fields.apiKey')}
              </label>
              <input
                id="judge-api-key"
                required={!initialData}
                type="password"
                disabled={saveMutation.isPending}
                value={form.apiKey}
                onChange={(event) => updateField('apiKey', event.target.value)}
                className={inputClassName}
                placeholder={initialData ? t('leaveBlankToKeep') : undefined}
              />
            </div>

            <label className="flex items-center gap-2 self-end pb-2 text-sm font-medium">
              <input
                type="checkbox"
                disabled={saveMutation.isPending}
                checked={form.active}
                onChange={(event) => updateField('active', event.target.checked)}
                className="size-4 rounded border-input"
              />
              {t('fields.active')}
            </label>

            <div className="sm:col-span-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setAdvancedOpen((current) => !current)}
              >
                {advancedOpen ? t('hideAdvanced') : t('showAdvanced')}
              </Button>
            </div>

            {advancedOpen && (
              <>
                <div className="space-y-2">
                  <label htmlFor="judge-base-url" className="text-sm font-medium">
                    {t('fields.baseUrl')}
                  </label>
                  <input
                    id="judge-base-url"
                    disabled={saveMutation.isPending}
                    value={form.baseUrl}
                    onChange={(event) => updateField('baseUrl', event.target.value)}
                    className={inputClassName}
                    placeholder="https://api.openai.com/v1"
                  />
                </div>

                <div className="space-y-2">
                  <label htmlFor="judge-config-json" className="text-sm font-medium">
                    {t('fields.configJson')}
                  </label>
                  <textarea
                    id="judge-config-json"
                    disabled={saveMutation.isPending}
                    value={form.configJson}
                    onChange={(event) =>
                      updateField('configJson', event.target.value)
                    }
                    className={textareaClassName}
                    placeholder='{"temperature": 0}'
                  />
                </div>
              </>
            )}
          </div>

          <div className="flex justify-end gap-3 pt-4">
            <Button
              type="button"
              variant="outline"
              disabled={saveMutation.isPending}
              onClick={() => handleClose(false)}
            >
              {tCommon('cancel')}
            </Button>
            <Button type="submit" disabled={saveMutation.isPending}>
              {saveMutation.isPending ? tCommon('saving') : tCommon('save')}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
