'use client';

import * as React from 'react';
import { useTranslations } from 'next-intl';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
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
import { useRouter } from '@/i18n/navigation';

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

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function StartEvaluationDialog({
  open,
  onOpenChange,
  projectId,
}: StartEvaluationDialogProps) {
  const t = useTranslations('evaluations');
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
    reset,
    formState: { errors },
  } = useForm<StartEvaluationFormValues>({
    resolver: zodResolver(startEvaluationSchema),
    defaultValues: {
      datasetPublicId: '',
      rubricVersionPublicId: '',
      connectorPublicId: '',
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
      queryKey: ['rubric-versions-published', projectId],
      queryFn: () =>
        apiClient.get<PageResponse<RubricVersionOption>>(
          `/api/v1/projects/${projectId}/rubric-versions?status=PUBLISHED&size=100`,
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

  const datasets = datasetsData?.items ?? [];
  const rubricVersions = rubricVersionsData?.items ?? [];
  const connectors = connectorsData?.items ?? [];

  // Job progress polling
  useJobProgress(jobPublicId, {
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

  if (!open) return null;

  const isLoading = datasetsLoading || rubricVersionsLoading || connectorsLoading;

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
              {t('progress')}...
            </p>
          </div>
        ) : (
          <form
            onSubmit={handleSubmit(onSubmit)}
            className="mt-4 space-y-4"
          >
            {/* Dataset select */}
            <div className="space-y-1.5">
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
            </div>

            {/* Rubric version select */}
            <div className="space-y-1.5">
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
            </div>

            {/* Connector select */}
            <div className="space-y-1.5">
              <label
                htmlFor="connectorPublicId"
                className="text-sm font-medium"
              >
                {t('connector')}
              </label>
              <select
                id="connectorPublicId"
                {...register('connectorPublicId')}
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
              {errors.connectorPublicId && (
                <p className="text-xs text-red-600">
                  {errors.connectorPublicId.message}
                </p>
              )}
            </div>

            {/* Actions */}
            <div className="flex items-center justify-between gap-3 pt-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={isSubmitting}
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
                  Cancel
                </Button>
                <Button type="submit" disabled={isSubmitting}>
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
