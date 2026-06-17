'use client';

import * as React from 'react';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { ShieldCheckIcon, ShieldWarningIcon, ArrowRightIcon, CircleNotchIcon } from '@phosphor-icons/react';
import { motion } from 'motion/react';

import { cn } from '@/lib/utils';
import { PageShell } from '@/components/layout/page-shell';
import { Button } from '@/components/ui/button';
import { useJobProgress } from '@/hooks/use-job-progress';
import { getRedTeamRun } from '@/lib/api/redteam';
import type { RedTeamRunResponse } from '@/lib/api/types';
import { useRouter } from '@/i18n/navigation';
import { Badge } from '@/components/ui/badge';

export default function RedTeamRunProgressPage() {
  const t = useTranslations('redTeam');
  const tCommon = useTranslations('common');
  const params = useParams();
  const router = useRouter();
  const queryClient = useQueryClient();
  const projectId = params.projectId as string;
  const runId = params.runId as string;

  // 1. Fetch red-team run details
  const { data: run, isLoading: runLoading, refetch: refetchRun } = useQuery<RedTeamRunResponse>({
    queryKey: ['red-team-run', runId],
    queryFn: () => getRedTeamRun(runId),
  });

  // 2. Poll job progress if not in terminal state
  const jobPublicId = run?.jobPublicId ?? null;
  const isTerminalRun = run ? ['COMPLETED', 'FAILED', 'CANCELLED'].includes(run.status) : false;

  const { job } = useJobProgress(jobPublicId, {
    enabled: !!jobPublicId && !isTerminalRun,
    onCompleted: () => {
      queryClient.invalidateQueries({ queryKey: ['red-team-run', runId] });
      queryClient.invalidateQueries({ queryKey: ['red-team-runs', projectId] });
      refetchRun();
    },
    onFailed: () => {
      queryClient.invalidateQueries({ queryKey: ['red-team-run', runId] });
      refetchRun();
    },
  });

  // Auto redirect on completed
  React.useEffect(() => {
    if (run?.status === 'COMPLETED') {
      const timer = setTimeout(() => {
        router.push(`/projects/${projectId}/red-team/${runId}/results`);
      }, 1500);
      return () => clearTimeout(timer);
    }
  }, [run?.status, projectId, runId, router]);

  if (runLoading || !run) {
    return (
      <PageShell title={t('title')} backHref={`/projects/${projectId}/red-team`} backLabel={tCommon('back')}>
        <div className="flex flex-col items-center justify-center py-20 gap-3 text-sm text-muted-foreground">
          <CircleNotchIcon className="size-6 animate-spin" />
          {tCommon('loading')}
        </div>
      </PageShell>
    );
  }

  const status = run.status;
  const currentProgress = job?.progressCurrent ?? (run.passedCases + run.failedCases + run.errorCases);
  const totalProgress = job?.progressTotal ?? (run.totalCases > 0 ? run.totalCases : run.numTests * (run.plugins?.length ?? 1));
  const percent = totalProgress > 0 ? Math.min(100, Math.round((currentProgress / totalProgress) * 100)) : 0;
  const errorMsg = job?.errorMessage || run.errorMessage;

  return (
    <PageShell
      title={run.name || t('defaultScanName', { number: runId.slice(0, 8) })}
      backHref={`/projects/${projectId}/red-team`}
      backLabel={tCommon('back')}
    >
      <div className="max-w-2xl mx-auto space-y-6 mt-4">
        {/* Run context summary card */}
        <div className="rounded-xl border bg-card p-5 space-y-4 text-foreground">
          <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">
            {t('progress.scanInfo')}
          </h2>
          <div className="grid gap-3 sm:grid-cols-2 text-sm">
            <div>
              <span className="text-muted-foreground">{t('progress.apiConnector')}:</span>{' '}
              <span className="font-medium text-foreground">{run.connectorName || '—'}</span>
            </div>
            <div>
              <span className="text-muted-foreground">{t('progress.judgeModel')}:</span>{' '}
              <span className="font-medium text-foreground">{run.judgeModelDisplayName || t('progress.judgeDefault')}</span>
            </div>
            <div className="sm:col-span-2">
              <span className="text-muted-foreground">{t('progress.chatbotPurpose')}:</span>
              <p className="mt-1 text-xs text-muted-foreground leading-relaxed bg-muted/40 p-2.5 rounded-lg border font-mono">
                {run.purpose}
              </p>
            </div>
            <div>
              <span className="text-muted-foreground">{t('progress.numAttacks')}:</span>{' '}
              <span className="font-semibold text-foreground">{t('progress.numAttacksPerType', { count: run.numTests })}</span>
            </div>
            <div>
              <span className="text-muted-foreground">{t('progress.pluginsActivated')}:</span>
              <div className="flex flex-wrap gap-1 mt-1.5">
                {run.plugins.map((p) => (
                  <Badge key={p} variant="outline" className="text-[10px] bg-muted border py-0.5 text-muted-foreground">
                    {p.split(':').pop() || p}
                  </Badge>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Live progress cockpit */}
        <div className="rounded-xl border bg-card p-6 space-y-6 text-foreground">
          <div className="flex items-center justify-between">
            <div className="space-y-1">
              <h3 className="font-bold text-lg text-foreground">
                {status === 'PENDING' && t('progress.preparing')}
                {status === 'RUNNING' && t('progress.scanning')}
                {status === 'COMPLETED' && t('progress.completed')}
                {status === 'FAILED' && t('progress.failed')}
                {status === 'CANCELLED' && t('progress.cancelled')}
              </h3>
              <p className="text-xs text-muted-foreground">
                {status === 'RUNNING' && t('progress.scanningDesc', { current: currentProgress, total: totalProgress })}
                {status === 'COMPLETED' && t('progress.completedDesc')}
                {status === 'PENDING' && t('progress.preparingDesc')}
                {(status === 'FAILED' || status === 'CANCELLED') && t('progress.failedDesc')}
              </p>
            </div>
            <div>
              {['PENDING', 'RUNNING'].includes(status) && (
                <CircleNotchIcon className="size-6 animate-spin text-red-600 dark:text-red-500" />
              )}
              {status === 'COMPLETED' && (
                <div className="size-7 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-500 flex items-center justify-center">
                  <ShieldCheckIcon size={18} weight="fill" />
                </div>
              )}
              {status === 'FAILED' && (
                <div className="size-7 rounded-full bg-red-500/10 border border-red-500/20 text-red-600 dark:text-red-500 flex items-center justify-center">
                  <ShieldWarningIcon size={18} weight="fill" />
                </div>
              )}
            </div>
          </div>

          {/* Progress bar */}
          <div className="space-y-2">
            <div className="flex items-center justify-between text-xs font-mono">
              <span className="text-muted-foreground">{t('progress.progressLabel')}</span>
              <span className={cn(
                'font-bold',
                status === 'COMPLETED' ? 'text-emerald-600 dark:text-emerald-500' : 'text-red-600 dark:text-red-500'
              )}>
                {percent}%
              </span>
            </div>
            <div className="h-2 w-full bg-muted overflow-hidden rounded-full border">
              <motion.div
                className={cn(
                  'h-full',
                  status === 'COMPLETED' ? 'bg-emerald-500' : 'bg-red-500'
                )}
                initial={{ width: 0 }}
                animate={{ width: `${percent}%` }}
                transition={{ duration: 0.5, ease: 'easeOut' }}
              />
            </div>
          </div>

          {/* Error messages */}
          {status === 'FAILED' && errorMsg && (
            <div className="rounded-lg border border-destructive/30 bg-destructive/10 p-4 space-y-2 text-sm text-red-600 dark:text-red-400">
              <span className="font-semibold block">{t('progress.errorDetail')}:</span>
              <p className="font-mono text-xs leading-relaxed max-h-40 overflow-y-auto whitespace-pre-wrap">
                {errorMsg}
              </p>
            </div>
          )}

          {/* CTA when completed */}
          {status === 'COMPLETED' && (
            <Button
              onClick={() => router.push(`/projects/${projectId}/red-team/${runId}/results`)}
              className="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-semibold flex items-center justify-center gap-1.5 py-5 shadow-lg shadow-emerald-600/10"
            >
              {t('progress.viewReport')}
              <ArrowRightIcon weight="bold" size={16} />
            </Button>
          )}
        </div>
      </div>
    </PageShell>
  );
}
