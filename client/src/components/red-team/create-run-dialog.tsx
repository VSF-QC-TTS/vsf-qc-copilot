'use client';

import * as React from 'react';
import { useTranslations } from 'next-intl';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { ShieldCheckIcon } from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { apiClient } from '@/lib/api/client';
import {
  createRedTeamRunSchema,
  type CreateRedTeamRunFormValues,
} from '@/lib/validations/red-team';
import type { PageResponse } from '@/lib/api/types';
import { Link, useRouter } from '@/i18n/navigation';
import { createRedTeamRun } from '@/lib/api/redteam';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type ConnectorOption = {
  publicId: string;
  name: string;
  active: boolean;
};

type JudgeModelOption = {
  publicId: string;
  name: string;
  provider: string;
  modelName: string;
  active: boolean;
};

// ---------------------------------------------------------------------------
// Props
// ---------------------------------------------------------------------------

interface CreateRedTeamRunDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  projectId: string;
}

interface RequirementFieldProps {
  empty: boolean;
  label: string;
  actionHref: string;
  actionLabel: string;
  children: React.ReactNode;
}

function RequirementField({
  empty,
  label,
  actionHref,
  actionLabel,
  children,
}: RequirementFieldProps) {
  if (!empty) {
    return <div className="space-y-1.5">{children}</div>;
  }
  return (
    <div className="rounded-md border border-dashed bg-muted/30 p-3">
      <div className="flex items-center justify-between gap-3">
        <span className="text-sm font-medium">{label}</span>
        <Button asChild size="sm" variant="outline">
          <Link href={actionHref}>{actionLabel}</Link>
        </Button>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function CreateRedTeamRunDialog({
  open,
  onOpenChange,
  projectId,
}: CreateRedTeamRunDialogProps) {
  const t = useTranslations('redTeam');
  const tCommon = useTranslations('common');
  const router = useRouter();
  const queryClient = useQueryClient();

  const [submitError, setSubmitError] = React.useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = React.useState(false);
  const cancelRef = React.useRef<HTMLButtonElement>(null);

  const {
    register,
    handleSubmit,
    setValue,
    control,
    formState: { errors },
  } = useForm<CreateRedTeamRunFormValues>({
    resolver: zodResolver(createRedTeamRunSchema),
    defaultValues: {
      name: '',
      targetConnectorPublicId: '',
      judgeModelPublicId: '',
      purpose: '',
      plugins: ['harmful:privacy', 'prompt-extraction', 'pii:direct'],
      numTests: 5,
    },
  });

  // Fetch active connectors
  const { data: connectorsData, isLoading: connectorsLoading } = useQuery({
    queryKey: ['connectors-active', projectId],
    queryFn: () =>
      apiClient.get<PageResponse<ConnectorOption>>(
        `/api/v1/projects/${projectId}/target-api-connectors?active=true&size=100`,
      ),
    enabled: open,
  });

  // Fetch active judge models
  const { data: judgeModelsData, isLoading: judgeModelsLoading } = useQuery({
    queryKey: ['judge-models-active', projectId],
    queryFn: () =>
      apiClient.get<PageResponse<JudgeModelOption>>(
        `/api/v1/projects/${projectId}/judge-models?active=true&size=100`,
      ),
    enabled: open,
  });

  const connectors = React.useMemo(() => connectorsData?.items ?? [], [connectorsData]);
  const judgeModels = React.useMemo(() => judgeModelsData?.items ?? [], [judgeModelsData]);
  const isLoading = connectorsLoading || judgeModelsLoading;

  // Auto-fill sole active connector if available
  React.useEffect(() => {
    if (connectors.length === 1 && open) {
      setValue('targetConnectorPublicId', connectors[0].publicId);
    }
  }, [connectors, open, setValue]);

  const onSubmit = async (values: CreateRedTeamRunFormValues) => {
    setIsSubmitting(true);
    setSubmitError(null);

    try {
      const res = await createRedTeamRun(projectId, {
        name: values.name || undefined,
        targetConnectorPublicId: values.targetConnectorPublicId,
        judgeModelPublicId: values.judgeModelPublicId || null,
        purpose: values.purpose,
        plugins: values.plugins,
        strategies: ['basic'], // Backend defaults to basic strategy
        numTests: values.numTests,
      });

      // Invalidate queries and navigate to the newly created Red Team run progress page
      queryClient.invalidateQueries({ queryKey: ['red-team-runs', projectId] });
      onOpenChange(false);
      router.push(`/projects/${projectId}/red-team/${res.runPublicId}`);
    } catch (err: unknown) {
      const errMsg = err instanceof Error ? err.message : (err as { message?: string })?.message || tCommon('error');
      setSubmitError(errMsg);
      setIsSubmitting(false);
    }
  };

  if (!open) return null;

  const canStart = connectors.length > 0;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/60 backdrop-blur-xs"
        onClick={() => !isSubmitting && onOpenChange(false)}
        aria-hidden="true"
      />

      {/* Card */}
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-redteam-title"
        className={cn(
          'relative z-10 w-full max-w-xl max-h-[90vh] flex flex-col rounded-xl border border-border/50 bg-card/90 backdrop-blur-md p-6 shadow-2xl text-foreground overflow-y-auto',
          'animate-in fade-in-0 zoom-in-95 duration-200',
        )}
      >
        <div className="flex items-center gap-3 mb-2">
          <div className="p-2 rounded-lg bg-muted text-foreground/80 border border-border/50">
            <ShieldCheckIcon size={20} weight="duotone" />
          </div>
          <h2
            id="create-redteam-title"
            className="text-lg font-semibold tracking-tight"
          >
            {t('createScanTitle')}
          </h2>
        </div>

        {submitError && (
          <div className="mt-3 rounded-lg border border-destructive/30 bg-destructive/10 p-3 text-sm text-red-600 dark:text-red-400">
            {submitError}
          </div>
        )}

        {isLoading ? (
          <div className="py-12 flex flex-col items-center justify-center gap-3 text-muted-foreground text-sm">
            <svg
              className="size-6 animate-spin text-muted-foreground"
              viewBox="0 0 24 24"
              fill="none"
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
            {tCommon('loading')}
          </div>
        ) : (
          <form noValidate
            onSubmit={handleSubmit(onSubmit)}
            className="mt-4 space-y-4"
          >
            {/* Run name input */}
            <div className="space-y-1.5">
              <label htmlFor="name" className="text-[13px] font-medium text-foreground">
                {t('scanName')}
              </label>
              <input
                id="name"
                type="text"
                placeholder={t('scanNamePlaceholder')}
                {...register('name')}
                disabled={isSubmitting}
                className="w-full rounded-md border border-border/50 bg-background px-3 py-2 text-sm text-foreground outline-hidden placeholder:text-muted-foreground focus:border-ring focus:ring-1 shadow-xs disabled:opacity-50 transition-all"
              />
              {errors.name && (
                <p className="text-[11px] text-destructive mt-1 font-medium">{errors.name.message}</p>
              )}
            </div>

            {/* Target Connector select */}
            <RequirementField
              empty={connectors.length === 0}
              label={t('fields.connector')}
              actionHref={`/projects/${projectId}/connectors`}
              actionLabel={tCommon('create')}
            >
              <label
                htmlFor="targetConnectorPublicId"
                className="text-[13px] font-medium text-foreground"
              >
                {t('fields.connector')}
              </label>
              <select
                id="targetConnectorPublicId"
                {...register('targetConnectorPublicId')}
                disabled={isSubmitting}
                className="w-full rounded-md border border-border/50 bg-background px-3 py-2 text-sm text-foreground outline-hidden focus:border-ring focus:ring-1 shadow-xs disabled:opacity-50 transition-all"
              >
                <option value="">
                  {t('fields.connectorPlaceholder')}
                </option>
                {connectors.map((c) => (
                  <option key={c.publicId} value={c.publicId}>
                    {c.name}
                  </option>
                ))}
              </select>
              {errors.targetConnectorPublicId && (
                <p className="text-[11px] text-destructive mt-1 font-medium">
                  {errors.targetConnectorPublicId.message}
                </p>
              )}
            </RequirementField>

            {/* Judge model select */}
            <RequirementField
              empty={false} // Never hide because it's optional
              label={t('fields.judgeModel')}
              actionHref={`/projects/${projectId}/judge-models`}
              actionLabel={tCommon('create')}
            >
              <label
                htmlFor="judgeModelPublicId"
                className="text-[13px] font-medium text-foreground flex items-center gap-1.5"
              >
                {t('fields.judgeModel')} <span className="text-[10px] text-muted-foreground font-mono uppercase tracking-widest font-medium border border-border/50 rounded-sm px-1 py-0.5 ml-1 bg-muted/30">Optional</span>
              </label>
              <select
                id="judgeModelPublicId"
                {...register('judgeModelPublicId')}
                disabled={isSubmitting}
                className="w-full rounded-md border border-border/50 bg-background px-3 py-2 text-sm text-foreground outline-hidden focus:border-ring focus:ring-1 shadow-xs disabled:opacity-50 transition-all"
              >
                <option value="">
                  {t('fields.judgeModelPlaceholder')}
                </option>
                {judgeModels.map((jm) => (
                  <option key={jm.publicId} value={jm.publicId}>
                    {jm.name} ({jm.provider}: {jm.modelName})
                  </option>
                ))}
              </select>
              {errors.judgeModelPublicId && (
                <p className="text-[11px] text-destructive mt-1 font-medium">
                  {errors.judgeModelPublicId.message}
                </p>
              )}
            </RequirementField>

            {/* Chatbot Purpose textarea */}
            <div className="space-y-1.5">
              <label htmlFor="purpose" className="text-[13px] font-medium text-foreground">
                {t('fields.purpose')}
              </label>
              <textarea
                id="purpose"
                rows={3}
                placeholder={t('fields.purposePlaceholder')}
                {...register('purpose')}
                disabled={isSubmitting}
                className="w-full rounded-md border border-border/50 bg-background px-3 py-2 text-sm text-foreground outline-hidden placeholder:text-muted-foreground focus:border-ring focus:ring-1 shadow-xs disabled:opacity-50 resize-none transition-all"
              />
              {errors.purpose ? (
                <p className="text-[11px] text-destructive mt-1 font-medium">{errors.purpose.message}</p>
              ) : (
                <p className="text-[11px] text-muted-foreground font-medium">
                  {t('fields.purposeHint')}
                </p>
              )}
            </div>

            {/* Plugins Checklist & NumTests */}
            <div className="grid gap-4 sm:grid-cols-2">
              {/* Plugins multi-select */}
              <div className="space-y-2.5">
                <span className="text-[13px] font-medium text-foreground block">
                  {t('fields.plugins')}
                </span>
                <div className="space-y-2 rounded-md border border-border/50 bg-background p-3 shadow-xs">
                  {['harmful:privacy', 'prompt-extraction', 'pii:direct'].map((plugin) => (
                    <label key={plugin} className="flex items-center gap-2.5 text-xs font-medium cursor-pointer text-foreground/80 hover:text-foreground">
                      <input
                        type="checkbox"
                        value={plugin}
                        {...register('plugins')}
                        disabled={isSubmitting}
                        className="rounded-sm border-border bg-background text-primary focus:ring-primary/50 focus:ring-offset-0 size-3.5 transition-colors"
                      />
                      <span className="font-mono tracking-tight text-[11px] uppercase">
                        {plugin === 'harmful:privacy' && t('plugins.privacy')}
                        {plugin === 'prompt-extraction' && t('plugins.extraction')}
                        {plugin === 'pii:direct' && t('plugins.pii')}
                      </span>
                    </label>
                  ))}
                </div>
                {errors.plugins && (
                  <p className="text-[11px] text-destructive mt-1 font-medium">{errors.plugins.message}</p>
                )}
              </div>

              {/* Num tests input */}
              <div className="space-y-2 flex flex-col justify-between">
                <div className="space-y-2.5">
                  <label htmlFor="numTests" className="text-[13px] font-medium text-foreground block">
                    {t('fields.numTests')}
                  </label>
                  <Controller
                    control={control}
                    name="numTests"
                    render={({ field }) => (
                      <div className="space-y-3">
                        <div className="flex items-center justify-between text-xs text-muted-foreground font-mono">
                          <span>1</span>
                          <span className="font-semibold text-primary text-[13px]">{t('fields.numTestsPerPlugin', { count: field.value })}</span>
                          <span>10</span>
                        </div>
                        <input
                          id="numTests"
                          type="range"
                          min={1}
                          max={10}
                          step={1}
                          disabled={isSubmitting}
                          value={field.value}
                          onChange={(e) => field.onChange(parseInt(e.target.value))}
                          className="w-full accent-primary h-1.5 bg-muted rounded-full appearance-none cursor-pointer border border-border/50"
                        />
                      </div>
                    )}
                  />
                  {errors.numTests && (
                    <p className="text-[11px] text-destructive mt-1 font-medium">{errors.numTests.message}</p>
                  )}
                </div>

                <p className="text-[11px] text-muted-foreground font-medium">
                  {t('fields.numTestsHint')}
                </p>
              </div>
            </div>

            {/* Actions */}
            <div className="flex items-center justify-end gap-3 pt-5 border-t border-border/50 mt-2">
              <Button
                ref={cancelRef}
                type="button"
                variant="ghost"
                disabled={isSubmitting}
                onClick={() => onOpenChange(false)}
                className="text-muted-foreground hover:text-foreground font-medium"
              >
                {tCommon('cancel')}
              </Button>
              <Button
                type="submit"
                variant="default"
                disabled={!canStart || isSubmitting}
                className="font-medium shadow-xs min-w-[120px]"
              >
                {isSubmitting && (
                  <svg
                    className="mr-1.5 size-4 animate-spin"
                    viewBox="0 0 24 24"
                    fill="none"
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
                {t('startScan')}
              </Button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
