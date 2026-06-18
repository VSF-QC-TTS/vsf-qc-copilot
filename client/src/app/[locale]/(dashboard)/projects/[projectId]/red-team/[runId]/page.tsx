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
        <div className="rounded-xl border bg-card/50 backdrop-blur-xs p-5 space-y-4 text-foreground shadow-sm">
          <h2 className="text-[11px] font-semibold text-muted-foreground uppercase tracking-widest font-mono">
            {t('progress.scanInfo')}
          </h2>
          <div className="grid gap-4 sm:grid-cols-2 text-[13px]">
            <div className="space-y-1">
              <span className="text-muted-foreground font-mono text-[11px] uppercase tracking-wider">{t('progress.apiConnector')}</span>
              <div className="font-medium text-foreground">{run.connectorName || '—'}</div>
            </div>
            <div className="space-y-1">
              <span className="text-muted-foreground font-mono text-[11px] uppercase tracking-wider">{t('progress.judgeModel')}</span>
              <div className="font-medium text-foreground">{run.judgeModelDisplayName || t('progress.judgeDefault')}</div>
            </div>
            <div className="sm:col-span-2 space-y-1">
              <span className="text-muted-foreground font-mono text-[11px] uppercase tracking-wider">{t('progress.chatbotPurpose')}</span>
              <p className="text-xs text-muted-foreground leading-relaxed bg-muted/30 p-3 rounded-md border font-mono">
                {run.purpose}
              </p>
            </div>
            <div className="space-y-1">
              <span className="text-muted-foreground font-mono text-[11px] uppercase tracking-wider">{t('progress.numAttacks')}</span>
              <div className="font-semibold text-foreground">{t('progress.numAttacksPerType', { count: run.numTests })}</div>
            </div>
            <div className="space-y-1">
              <span className="text-muted-foreground font-mono text-[11px] uppercase tracking-wider">{t('progress.pluginsActivated')}</span>
              <div className="flex flex-wrap gap-1.5 mt-0.5">
                {run.plugins.map((p) => (
                  <Badge key={p} variant="outline" className="text-[10px] bg-muted/40 border py-0 text-muted-foreground uppercase tracking-tight font-medium">
                    {p.split(':').pop() || p}
                  </Badge>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Live progress cockpit */}
        <div className={cn(
          "rounded-xl border p-6 space-y-6 text-foreground shadow-sm relative overflow-hidden transition-colors",
          status === 'RUNNING' ? 'border-primary/30 bg-primary/5' : 'bg-card'
        )}>
          {status === 'RUNNING' && (
            <motion.div
              className="absolute left-0 top-0 bottom-0 w-[2px] bg-primary/50"
              animate={{ opacity: [0.3, 1, 0.3] }}
              transition={{ repeat: Infinity, duration: 1.5 }}
            />
          )}
          <div className="flex items-center justify-between">
            <div className="space-y-1.5">
              <h3 className="font-semibold text-base text-foreground tracking-tight flex items-center gap-2">
                {status === 'RUNNING' && <span className="relative flex size-2"><span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75"></span><span className="relative inline-flex rounded-full size-2 bg-primary"></span></span>}
                {status === 'PENDING' && t('progress.preparing')}
                {status === 'RUNNING' && t('progress.scanning')}
                {status === 'COMPLETED' && t('progress.completed')}
                {status === 'FAILED' && t('progress.failed')}
                {status === 'CANCELLED' && t('progress.cancelled')}
              </h3>
              <p className="text-[13px] text-muted-foreground font-medium">
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
          <div className="space-y-2.5">
            <div className="flex items-center justify-between text-[11px] font-mono uppercase tracking-widest text-muted-foreground">
              <span>{t('progress.progressLabel')}</span>
              <span className={cn(
                'font-bold',
                status === 'COMPLETED' ? 'text-emerald-500' : 
                status === 'RUNNING' ? 'text-primary' : 'text-muted-foreground'
              )}>
                {percent}%
              </span>
            </div>
            <div className="h-1.5 w-full bg-muted overflow-hidden rounded-full border border-background/50">
              <motion.div
                className={cn(
                  'h-full',
                  status === 'COMPLETED' ? 'bg-emerald-500' : 
                  status === 'FAILED' ? 'bg-destructive' : 'bg-primary'
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
              className="w-full font-medium flex items-center justify-center gap-2 py-5 shadow-sm"
              variant="default"
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
