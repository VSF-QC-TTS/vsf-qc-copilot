'use client';

import * as React from 'react';
import { useTranslations } from 'next-intl';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { LightningIcon } from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { apiClient } from '@/lib/api/client';
import { useJobProgress } from '@/hooks/use-job-progress';
import {
  startEvaluationSchema,
  type StartEvaluationFormValues,
} from '@/lib/validations/evaluation';
import type { PageResponse } from '@/lib/api/types';
import { Link, useRouter } from '@/i18n/navigation';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type DatasetOption = {
  publicId: string;
  name: string;
  status: string;
};

type RubricVersionOption = {
  publicId: string;
  versionNumber: number;
  rubricName: string;
  status: string;
};

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

type StartEvaluationResponse = {
  runPublicId: string;
  jobPublicId: string;
  status: string;
  message: string;
};

// ---------------------------------------------------------------------------
// Props
// ---------------------------------------------------------------------------

interface StartEvaluationDialogProps {
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

export function StartEvaluationDialog({
  open,
  onOpenChange,
  projectId,
}: StartEvaluationDialogProps) {
  const t = useTranslations('evaluations');
  const tCommon = useTranslations('common');
  const router = useRouter();
  const queryClient = useQueryClient();

  const [jobPublicId, setJobPublicId] = React.useState<string | null>(null);
  const [runPublicId, setRunPublicId] = React.useState<string | null>(null);
  const [submitError, setSubmitError] = React.useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = React.useState(false);
  const cancelRef = React.useRef<HTMLButtonElement>(null);

  const {
    register,
    handleSubmit,
    setValue,
    control,
    reset,
    formState: { errors },
  } = useForm<StartEvaluationFormValues>({
    resolver: zodResolver(startEvaluationSchema),
    defaultValues: {
      datasetPublicId: '',
      rubricVersionPublicId: '',
      targetConnectorPublicId: '',
      judgeModelPublicId: '',
    },
  });

  // Fetch datasets (APPROVED)
  const { data: datasetsData, isLoading: datasetsLoading } = useQuery({
    queryKey: ['datasets-approved', projectId],
    queryFn: () =>
      apiClient.get<PageResponse<DatasetOption>>(
        `/api/v1/projects/${projectId}/datasets?status=APPROVED&size=100`,
      ),
    enabled: open,
  });

  // Fetch rubric versions (PUBLISHED)
  const { data: rubricVersionsData, isLoading: rubricVersionsLoading } =
    useQuery({
      queryKey: ['rubric-versions-published'],
      queryFn: () =>
        apiClient.get<PageResponse<RubricVersionOption>>(
          '/api/v1/rubric-versions?status=PUBLISHED&size=100',
        ),
      enabled: open,
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

  const datasets = React.useMemo(() => datasetsData?.items ?? [], [datasetsData]);
  const rubricVersions = React.useMemo(
    () => rubricVersionsData?.items ?? [],
    [rubricVersionsData],
  );
  const connectors = React.useMemo(() => connectorsData?.items ?? [], [connectorsData]);
  const judgeModels = React.useMemo(
    () => judgeModelsData?.items ?? [],
    [judgeModelsData],
  );
  const selectedValues = useWatch({ control });

  // Job progress polling
  const { job } = useJobProgress(jobPublicId, {
    onCompleted: () => {
      void queryClient.invalidateQueries({
        queryKey: ['evaluation-runs', projectId],
      });
      onOpenChange(false);
      if (runPublicId) {
        router.push(`/projects/${projectId}/evaluations/${runPublicId}`);
      }
    },
    onFailed: () => {
      setSubmitError('Evaluation job failed.');
      setIsSubmitting(false);
    },
  });

  // Reset on close
  React.useEffect(() => {
    if (open) return;

    let cancelled = false;
    queueMicrotask(() => {
      if (cancelled) return;

      reset();
      setJobPublicId(null);
      setRunPublicId(null);
      setSubmitError(null);
      setIsSubmitting(false);
    });

    return () => {
      cancelled = true;
    };
  }, [open, reset]);

  // Focus cancel on open
  React.useEffect(() => {
    if (open) {
      const raf = requestAnimationFrame(() => cancelRef.current?.focus());
      return () => cancelAnimationFrame(raf);
    }
  }, [open]);

  // Escape key
  React.useEffect(() => {
    if (!open) return;
    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        e.stopPropagation();
        onOpenChange(false);
      }
    }
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [open, onOpenChange]);

  // Lock body scroll
  React.useEffect(() => {
    if (!open) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prev;
    };
  }, [open]);

  const onSubmit = async (values: StartEvaluationFormValues) => {
    setSubmitError(null);
    setIsSubmitting(true);
    try {
      const res = await apiClient.post<StartEvaluationResponse>(
        `/api/v1/projects/${projectId}/evaluation-runs`,
        values,
      );
      setRunPublicId(res.runPublicId);
      setJobPublicId(res.jobPublicId);
    } catch (err: unknown) {
      const msg =
        err instanceof Object && 'message' in err
          ? (err as { message: string }).message
          : 'Failed to start evaluation';
      setSubmitError(msg);
      setIsSubmitting(false);
    }
  };

  const handleQuickEvaluate = async () => {
    setSubmitError(null);
    setIsSubmitting(true);
    try {
      const res = await apiClient.post<StartEvaluationResponse>(
        `/api/v1/projects/${projectId}/quick-evaluate`,
        {},
      );
      setRunPublicId(res.runPublicId);
      setJobPublicId(res.jobPublicId);
    } catch (err: unknown) {
      const msg =
        err instanceof Object && 'message' in err
          ? (err as { message: string }).message
          : 'Quick evaluation failed';
      setSubmitError(msg);
      setIsSubmitting(false);
    }
  };

  const isLoading =
    datasetsLoading ||
    rubricVersionsLoading ||
    connectorsLoading ||
    judgeModelsLoading;
  const canQuickEvaluate =
    datasets.length === 1 &&
    rubricVersions.length === 1 &&
    connectors.length === 1 &&
    judgeModels.length === 1;
  const canStart =
    !isLoading &&
    !isSubmitting &&
    Boolean(selectedValues.datasetPublicId) &&
    Boolean(selectedValues.rubricVersionPublicId) &&
    Boolean(selectedValues.targetConnectorPublicId) &&
    Boolean(selectedValues.judgeModelPublicId);

  React.useEffect(() => {
    if (datasets.length === 1 && !selectedValues.datasetPublicId) {
      setValue('datasetPublicId', datasets[0].publicId);
    }
    if (rubricVersions.length === 1 && !selectedValues.rubricVersionPublicId) {
      setValue('rubricVersionPublicId', rubricVersions[0].publicId);
    }
    if (connectors.length === 1 && !selectedValues.targetConnectorPublicId) {
      setValue('targetConnectorPublicId', connectors[0].publicId);
    }
    if (judgeModels.length === 1 && !selectedValues.judgeModelPublicId) {
      setValue('judgeModelPublicId', judgeModels[0].publicId);
    }
  }, [
    connectors,
    datasets,
    judgeModels,
    rubricVersions,
    selectedValues.datasetPublicId,
    selectedValues.judgeModelPublicId,
    selectedValues.rubricVersionPublicId,
    selectedValues.targetConnectorPublicId,
    setValue,
  ]);

  if (!open) return null;

  return (
    <div
      data-slot="start-evaluation-dialog"
      className="fixed inset-0 z-50 flex items-center justify-center"
    >
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={() => !isSubmitting && onOpenChange(false)}
        aria-hidden="true"
      />

      {/* Card */}
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="start-eval-title"
        className={cn(
          'relative z-10 w-full max-w-lg rounded-lg border bg-card p-6 shadow-lg',
          'animate-in fade-in-0 zoom-in-95',
        )}
      >
        <h2
          id="start-eval-title"
          className="text-lg font-semibold text-card-foreground"
        >
          {t('startEvaluation')}
        </h2>

        {submitError && (
          <div className="mt-3 rounded-md bg-red-50 p-3 text-sm text-red-800 dark:bg-red-950 dark:text-red-300">
            {submitError}
          </div>
        )}

        {jobPublicId ? (
          <div className="mt-4 space-y-2">
            <p className="text-sm text-muted-foreground animate-pulse">
              {t('progress')}: {job?.progress ?? 0}%
            </p>
          </div>
        ) : (
          <form
            onSubmit={handleSubmit(onSubmit)}
            className="mt-4 space-y-4"
          >
            {/* Dataset select */}
            <RequirementField
              empty={datasets.length === 0}
              label={t('dataset')}
              actionHref={`/projects/${projectId}/datasets`}
              actionLabel={t('createMissingDataset')}
            >
              <label
                htmlFor="datasetPublicId"
                className="text-sm font-medium"
              >
                {t('dataset')}
              </label>
              <select
                id="datasetPublicId"
                {...register('datasetPublicId')}
                disabled={isLoading || isSubmitting}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50"
              >
                <option value="">{t('selectDataset')}</option>
                {datasets.map((d) => (
                  <option key={d.publicId} value={d.publicId}>
                    {d.name}
                  </option>
                ))}
              </select>
              {errors.datasetPublicId && (
                <p className="text-xs text-red-600">
                  {errors.datasetPublicId.message}
                </p>
              )}
            </RequirementField>

            {/* Rubric version select */}
            <RequirementField
              empty={rubricVersions.length === 0}
              label={t('rubricVersion')}
              actionHref="/rubrics"
              actionLabel={t('createMissingRubric')}
            >
              <label
                htmlFor="rubricVersionPublicId"
                className="text-sm font-medium"
              >
                {t('rubricVersion')}
              </label>
              <select
                id="rubricVersionPublicId"
                {...register('rubricVersionPublicId')}
                disabled={isLoading || isSubmitting}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50"
              >
                <option value="">{t('selectRubricVersion')}</option>
                {rubricVersions.map((rv) => (
                  <option key={rv.publicId} value={rv.publicId}>
                    {rv.rubricName} v{rv.versionNumber}
                  </option>
                ))}
              </select>
              {errors.rubricVersionPublicId && (
                <p className="text-xs text-red-600">
                  {errors.rubricVersionPublicId.message}
                </p>
              )}
            </RequirementField>

            {/* Connector select */}
            <RequirementField
              empty={connectors.length === 0}
              label={t('connector')}
              actionHref={`/projects/${projectId}/connectors`}
              actionLabel={t('createMissingConnector')}
            >
              <label
                htmlFor="targetConnectorPublicId"
                className="text-sm font-medium"
              >
                {t('connector')}
              </label>
              <select
                id="targetConnectorPublicId"
                {...register('targetConnectorPublicId')}
                disabled={isLoading || isSubmitting}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50"
              >
                <option value="">{t('selectConnector')}</option>
                {connectors.map((c) => (
                  <option key={c.publicId} value={c.publicId}>
                    {c.name}
                  </option>
                ))}
              </select>
              {errors.targetConnectorPublicId && (
                <p className="text-xs text-red-600">
                  {errors.targetConnectorPublicId.message}
                </p>
              )}
            </RequirementField>

            {/* Judge model select */}
            <RequirementField
              empty={judgeModels.length === 0}
              label={t('judgeModel')}
              actionHref={`/projects/${projectId}/judge-models`}
              actionLabel={t('createMissingJudgeModel')}
            >
              <label
                htmlFor="judgeModelPublicId"
                className="text-sm font-medium"
              >
                {t('judgeModel')}
              </label>
              <select
                id="judgeModelPublicId"
                {...register('judgeModelPublicId')}
                disabled={isLoading || isSubmitting}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50"
              >
                <option value="">{t('selectJudgeModel')}</option>
                {judgeModels.map((jm) => (
                  <option key={jm.publicId} value={jm.publicId}>
                    {jm.name} ({jm.provider}: {jm.modelName})
                  </option>
                ))}
              </select>
              {errors.judgeModelPublicId && (
                <p className="text-xs text-red-600">
                  {errors.judgeModelPublicId.message}
                </p>
              )}
            </RequirementField>

            {/* Actions */}
            <div className="flex items-center justify-between gap-3 pt-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={isSubmitting || !canQuickEvaluate}
                onClick={handleQuickEvaluate}
              >
                <LightningIcon weight="bold" />
                {t('quickEvaluate')}
              </Button>

              <div className="flex items-center gap-3">
                <Button
                  ref={cancelRef}
                  type="button"
                  variant="outline"
                  disabled={isSubmitting}
                  onClick={() => onOpenChange(false)}
                >
                  {tCommon('cancel')}
                </Button>
                <Button type="submit" disabled={!canStart}>
                  {isSubmitting && (
                    <svg
                      className="mr-1.5 size-4 animate-spin"
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
                  {t('startEvaluation')}
                </Button>
              </div>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
