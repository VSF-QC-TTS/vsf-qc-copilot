'use client';

import * as React from 'react';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useQuery } from '@tanstack/react-query';
import {
  TableIcon,
  ExportIcon,
  ClockIcon,
} from '@phosphor-icons/react';

import { Button } from '@/components/ui/button';
import { PageShell } from '@/components/layout/page-shell';
import { MetricCard } from '@/components/ui/metric-card';
import { StatusBadge } from '@/components/ui/status-badge';
import { useJobProgress } from '@/hooks/use-job-progress';
import { apiClient } from '@/lib/api/client';
import { useRouter } from '@/i18n/navigation';
import { ExportDialog } from '@/components/evaluations/export-dialog';

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
  message: string;
  createdAt: string;
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

function safePercent(count: number | null, total: number | null): string | null {
  if (total === null || total === 0 || count === null) return null;
  return `${Math.round((count / total) * 100)}%`;
}

// ---------------------------------------------------------------------------
// Page component
// ---------------------------------------------------------------------------

export default function RunDetailPage() {
  const t = useTranslations('evaluations');
  const tCommon = useTranslations('common');
  const router = useRouter();
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

  // Fetch events
  const { data: events } = useQuery<RunEvent[]>({
    queryKey: ['evaluation-run-events', runId],
    queryFn: () =>
      apiClient.get<RunEvent[]>(
        `/api/v1/evaluation-runs/${runId}/events`,
      ),
  });

  // Poll job progress when active
  const isActive =
    run?.status === 'RUNNING' || run?.status === 'PENDING';
  const { job, isPolling } = useJobProgress(
    isActive ? (run?.jobPublicId ?? null) : null,
    { enabled: isActive },
  );

  const progressPercent =
    job?.progress !== null && job?.progress !== undefined && job?.progressTotal
      ? `${Math.round((job.progress / job.progressTotal) * 100)}%`
      : null;

  return (
    <>
    <PageShell
      title={run?.publicId ?? t('runDetail')}
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
          {/* ExportIcon (Epic 11) */}
          <Button variant="outline" onClick={() => setExportOpen(true)}>
            <ExportIcon weight="bold" />
            {t('export')}
          </Button>
        </div>
      }
    >
      {/* Status */}
      <div className="flex items-center gap-3">
        {run && <StatusBadge status={run.status} />}
        {isPolling && progressPercent && (
          <span className="text-sm text-muted-foreground animate-pulse">
            {progressPercent}
          </span>
        )}
      </div>

      {/* Summary cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
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
      </div>

      {/* Events timeline */}
      <div className="space-y-2">
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">
          {t('events')}
        </h2>

        {events && events.length > 0 ? (
          <div className="space-y-3">
            {events.map((evt) => (
              <div
                key={evt.publicId}
                className="flex items-start gap-3 rounded-md border p-3"
              >
                <ClockIcon
                  className="mt-0.5 size-4 shrink-0 text-muted-foreground"
                  weight="duotone"
                />
                <div className="flex-1 space-y-0.5">
                  <p className="text-sm">{evt.message}</p>
                  <p className="text-xs text-muted-foreground">
                    {formatDateTime(evt.createdAt)}
                  </p>
                </div>
                <StatusBadge status={evt.eventType} size="sm" />
              </div>
            ))}
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">
            {t('noEvents')}
          </p>
        )}
      </div>
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
