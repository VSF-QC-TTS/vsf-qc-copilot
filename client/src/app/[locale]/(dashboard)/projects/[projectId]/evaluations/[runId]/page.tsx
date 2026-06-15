'use client';

import * as React from 'react';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  TableIcon,
  ExportIcon,
  ClockIcon,
} from '@phosphor-icons/react';
import { motion, AnimatePresence } from 'motion/react';

import { Button } from '@/components/ui/button';
import { PageShell } from '@/components/layout/page-shell';
import { MetricCard } from '@/components/ui/metric-card';
import { StatusBadge } from '@/components/ui/status-badge';
import { useJobProgress } from '@/hooks/use-job-progress';
import { apiClient } from '@/lib/api/client';
import { useRouter } from '@/i18n/navigation';
import { ExportDialog } from '@/components/evaluations/export-dialog';
import type { PageResponse } from '@/lib/api/types';
import dynamic from 'next/dynamic';

const RunDonutChart = dynamic(() => import('@/components/evaluations/run-donut-chart'), {
  ssr: false,
  loading: () => <div className="h-64 w-full animate-pulse bg-muted/20 rounded-lg" />,
});

const CriteriaBarChart = dynamic(() => import('@/components/evaluations/criteria-bar-chart'), {
  ssr: false,
  loading: () => <div className="h-80 w-full animate-pulse bg-muted/20 rounded-lg" />,
});

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type EvaluationRunDetail = {
  publicId: string;
  datasetPublicId: string;
  datasetName: string | null;
  rubricVersionPublicId: string;
  rubricName: string | null;
  rubricVersionNumber: number;
  targetConnectorPublicId: string;
  connectorName: string | null;
  judgeModelPublicId: string | null;
  judgeModelDisplayName: string | null;
  jobPublicId: string | null;
  status: string;
  description: string | null;
  totalCases: number;
  completedCases: number;
  passedCases: number;
  failedCases: number;
  warningCases: number;
  errorCases: number;
  passRate: number;
  maxConcurrency: number;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

type RunEvent = {
  publicId: string;
  eventType: string;
  payloadJson: string;
  createdAt: string;
};

type EvaluationResultRow = {
  publicId: string;
  criteriaResultsJson: string | null;
};

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

const containerVariants = {
  hidden: { opacity: 0, y: 15 },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.4,
      staggerChildren: 0.08,
    },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 10 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.3 },
  },
};

// ---------------------------------------------------------------------------
// Page component
// ---------------------------------------------------------------------------

export default function RunDetailPage() {
  const t = useTranslations('evaluations');
  const tCommon = useTranslations('common');
  const router = useRouter();
  const queryClient = useQueryClient();
  const params = useParams();
  const projectId = params.projectId as string;
  const runId = params.runId as string;

  const [exportOpen, setExportOpen] = React.useState(false);

  // Fetch run detail
  const { data: run, isLoading: runLoading } = useQuery<EvaluationRunDetail>({
    queryKey: ['evaluation-run', runId],
    queryFn: () =>
      apiClient.get<EvaluationRunDetail>(
        `/api/v1/evaluation-runs/${runId}`,
      ),
  });

  // Fetch all runs of this project to find sequential index
  const { data: runsListData } = useQuery({
    queryKey: ['evaluation-runs', projectId, 'all'],
    queryFn: () =>
      apiClient.get<PageResponse<{ publicId: string }>>(
        `/api/v1/projects/${projectId}/evaluation-runs?page=0&size=1000&sort=createdAt,asc`,
      ),
  });

  const runNumber = React.useMemo(() => {
    if (!runsListData || !runId) return null;
    const index = runsListData.items.findIndex((item) => item.publicId === runId);
    return index !== -1 ? index + 1 : null;
  }, [runsListData, runId]);

  // Fetch events
  const { data: events } = useQuery<RunEvent[]>({
    queryKey: ['evaluation-run-events', runId],
    queryFn: () =>
      apiClient.get<RunEvent[]>(
        `/api/v1/evaluation-runs/${runId}/events`,
      ),
  });

  // Fetch results to aggregate criteria performance
  const { data: resultsData, isLoading: resultsLoading } = useQuery({
    queryKey: ['evaluation-results-all', runId],
    queryFn: () =>
      apiClient.get<PageResponse<EvaluationResultRow>>(
        `/api/v1/evaluation-runs/${runId}/results?page=0&size=100`,
      ),
    enabled: !!run && (run.status === 'COMPLETED' || run.status === 'FAILED'),
  });

  const criteriaData = React.useMemo(() => {
    if (!resultsData) return [];
    
    const results = resultsData.items ?? [];
    const aggMap: Record<string, { total: number; passed: number }> = {};
    
    results.forEach((row) => {
      if (!row.criteriaResultsJson) return;
      try {
        const parsed = JSON.parse(row.criteriaResultsJson);
        if (Array.isArray(parsed)) {
          parsed.forEach((crit: { name?: string; status?: string; score?: number }) => {
            const name = crit.name || 'Unknown';
            const isPassed = crit.status === 'PASS' || crit.score === 1 || crit.score === 100;
            
            if (!aggMap[name]) {
              aggMap[name] = { total: 0, passed: 0 };
            }
            aggMap[name].total += 1;
            if (isPassed) {
              aggMap[name].passed += 1;
            }
          });
        }
      } catch {
        // Ignore json parsing errors
      }
    });
    
    return Object.entries(aggMap).map(([name, stats]) => ({
      name,
      total: stats.total,
      passed: stats.passed,
      passRate: stats.total > 0 ? Math.round((stats.passed / stats.total) * 100) : 0,
    })).sort((a, b) => a.passRate - b.passRate);
  }, [resultsData]);

  // Poll job progress when active
  const isActive =
    run?.status === 'RUNNING' || run?.status === 'PENDING';
  const refreshRunState = React.useCallback(() => {
    void queryClient.invalidateQueries({ queryKey: ['evaluation-run', runId] });
    void queryClient.invalidateQueries({ queryKey: ['evaluation-run-events', runId] });
    void queryClient.invalidateQueries({ queryKey: ['evaluation-runs', projectId] });
    void queryClient.invalidateQueries({ queryKey: ['evaluations', projectId] });
  }, [projectId, queryClient, runId]);

  const { job, isPolling } = useJobProgress(
    isActive ? (run?.jobPublicId ?? null) : null,
    {
      enabled: isActive,
      onCompleted: refreshRunState,
      onFailed: refreshRunState,
    },
  );

  const progressPercent =
    job && job.progressTotal > 0
      ? `${Math.round((job.progressCurrent / job.progressTotal) * 100)}%`
      : null;

  const renderEventMessage = React.useCallback(
    (evt: RunEvent) => {
      try {
        const payload = JSON.parse(evt.payloadJson || '{}');
        return t(`eventTypes.${evt.eventType}`, payload);
      } catch {
        return evt.eventType;
      }
    },
    [t],
  );

  return (
    <>
      <PageShell
        title={runNumber ? t('runNumber', { number: runNumber }) : (run?.publicId ?? t('runDetail'))}
        description={runNumber ? run?.publicId : undefined}
        backHref={`/projects/${projectId}/evaluations`}
        backLabel={tCommon('back')}
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              onClick={() =>
                router.push(
                  `/projects/${projectId}/evaluations/${runId}/results`,
                )
              }
            >
              <TableIcon weight="bold" />
              {t('results')}
            </Button>
            <Button variant="outline" onClick={() => setExportOpen(true)}>
              <ExportIcon weight="bold" />
              {t('export')}
            </Button>
          </div>
        }
      >
        <motion.div
          variants={containerVariants}
          initial="hidden"
          animate="visible"
          className="space-y-6"
        >
          {/* Status */}
          <motion.div variants={itemVariants} className="flex items-center gap-3">
            {run && <StatusBadge status={run.status} />}
            {isPolling && progressPercent && (
              <span className="text-sm text-muted-foreground animate-pulse">
                {progressPercent}
              </span>
            )}
          </motion.div>

          {/* Configuration Card */}
          {run && (
            <motion.div
              variants={itemVariants}
              className="rounded-lg border bg-card p-4 space-y-3 shadow-xs"
            >
              <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">
                {t('configuration')}
              </h2>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 text-sm">
                <div className="space-y-1">
                  <span className="text-muted-foreground block text-xs">{t('dataset')}</span>
                  <span className="font-medium text-foreground">{run.datasetName ?? '-'}</span>
                </div>
                <div className="space-y-1">
                  <span className="text-muted-foreground block text-xs">{t('rubric')}</span>
                  <span className="font-medium text-foreground">
                    {run.rubricName ?? '-'} {run.rubricVersionNumber ? `(v${run.rubricVersionNumber})` : ''}
                  </span>
                </div>
                <div className="space-y-1">
                  <span className="text-muted-foreground block text-xs">{t('connector')}</span>
                  <span className="font-medium text-foreground">{run.connectorName ?? '-'}</span>
                </div>
                <div className="space-y-1">
                  <span className="text-muted-foreground block text-xs">{t('judgeModel')}</span>
                  <span className="font-medium text-foreground">{run.judgeModelDisplayName ?? '-'}</span>
                </div>
              </div>
            </motion.div>
          )}

          {/* Summary cards */}
          <motion.div
            variants={itemVariants}
            className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4"
          >
            <MetricCard
              label={t('totalResults')}
              value={run?.totalCases ?? null}
              loading={runLoading}
            />
            <MetricCard
              label={t('passRate')}
              value={
                run?.passRate !== undefined && run?.passRate !== null
                  ? `${Math.round(run.passRate * 100)}%`
                  : null
              }
              loading={runLoading}
            />
            <MetricCard
              label={t('passed')}
              value={run?.passedCases ?? null}
              loading={runLoading}
            />
            <MetricCard
              label={t('failed')}
              value={run?.failedCases ?? null}
              loading={runLoading}
            />
          </motion.div>

          {/* Charts Section */}
          {run && (run.status === 'COMPLETED' || run.status === 'FAILED') && (
            <motion.div
              variants={itemVariants}
              className="grid gap-6 md:grid-cols-3"
            >
              {/* Donut Chart (Status distribution) */}
              <div className="md:col-span-1 rounded-lg border bg-card p-4 shadow-xs flex flex-col justify-between">
                <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-2">
                  {t('summary')}
                </h3>
                <RunDonutChart
                  passed={run.passedCases}
                  failed={run.failedCases}
                  warning={run.warningCases ?? 0}
                  error={run.errorCases ?? 0}
                  passRate={run.passRate}
                />
              </div>

              {/* Bar Chart (Criteria Success Rate) */}
              <div className="md:col-span-2 rounded-lg border bg-card p-4 shadow-xs flex flex-col justify-between">
                <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-2">
                  {t('passRate')}
                </h3>
                {resultsLoading ? (
                  <div className="h-80 w-full animate-pulse bg-muted/20 rounded-lg" />
                ) : criteriaData.length > 0 ? (
                  <CriteriaBarChart data={criteriaData} />
                ) : (
                  <div className="h-80 flex items-center justify-center text-sm text-muted-foreground">
                    {tCommon('notAvailable')}
                  </div>
                )}
              </div>
            </motion.div>
          )}

          {/* Events timeline */}
          <motion.div variants={itemVariants} className="space-y-3">
            <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">
              {t('events')}
            </h2>

            {events && events.length > 0 ? (
              <div className="space-y-3">
                <AnimatePresence initial={false}>
                  {events.map((evt) => (
                    <motion.div
                      key={evt.publicId}
                      initial={{ opacity: 0, x: -10 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: 10 }}
                      transition={{ duration: 0.2 }}
                      className="flex items-start gap-3 rounded-md border p-3 bg-card shadow-xs"
                    >
                      <ClockIcon
                        className="mt-0.5 size-4 shrink-0 text-muted-foreground"
                        weight="duotone"
                      />
                      <div className="flex-1 space-y-0.5">
                        <p className="text-sm text-foreground">{renderEventMessage(evt)}</p>
                        <p className="text-xs text-muted-foreground">
                          {formatDateTime(evt.createdAt)}
                        </p>
                      </div>
                      <StatusBadge status={evt.eventType} size="sm" />
                    </motion.div>
                  ))}
                </AnimatePresence>
              </div>
            ) : (
              <p className="text-sm text-muted-foreground">
                {t('noEvents')}
              </p>
            )}
          </motion.div>
        </motion.div>
      </PageShell>

      {/* ExportIcon dialog */}
      <ExportDialog
        runPublicId={runId}
        open={exportOpen}
        onOpenChange={setExportOpen}
      />
    </>
  );
}
