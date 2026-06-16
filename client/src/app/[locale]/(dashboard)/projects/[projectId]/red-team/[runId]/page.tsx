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
      // Invalidate query and refetch run info
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
      }, 1500); // Small delay so the user sees it's done
      return () => clearTimeout(timer);
    }
  }, [run?.status, projectId, runId, router]);

  if (runLoading || !run) {
    return (
      <PageShell title={t('title')} backHref={`/projects/${projectId}/red-team`} backLabel={tCommon('back')}>
        <div className="flex flex-col items-center justify-center py-20 gap-3 text-sm text-zinc-500">
          <CircleNotchIcon className="size-6 animate-spin text-zinc-500" />
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
      title={run.name || `Quét bảo mật #${runId.slice(0, 8)}`}
      backHref={`/projects/${projectId}/red-team`}
      backLabel={tCommon('back')}
    >
      <div className="max-w-2xl mx-auto space-y-6 mt-4">
        {/* Run context summary card */}
        <div className="rounded-xl border border-zinc-800 bg-zinc-950 p-5 space-y-4 text-zinc-200">
          <h2 className="text-sm font-semibold text-zinc-400 uppercase tracking-wider">
            Thông tin đợt quét
          </h2>
          <div className="grid gap-3 sm:grid-cols-2 text-sm">
            <div>
              <span className="text-zinc-500">API Connector:</span>{' '}
              <span className="font-medium text-zinc-300">{run.connectorName || '—'}</span>
            </div>
            <div>
              <span className="text-zinc-500">Mô hình đánh giá:</span>{' '}
              <span className="font-medium text-zinc-300">{run.judgeModelDisplayName || 'Mặc định (Promptfoo)'}</span>
            </div>
            <div className="sm:col-span-2">
              <span className="text-zinc-500">Mục đích Chatbot:</span>
              <p className="mt-1 text-xs text-zinc-400 leading-relaxed bg-zinc-900/40 p-2.5 rounded-lg border border-zinc-900 font-mono">
                {run.purpose}
              </p>
            </div>
            <div>
              <span className="text-zinc-500">Số lượng đòn:</span>{' '}
              <span className="font-semibold text-zinc-300">{run.numTests} đòn / loại</span>
            </div>
            <div>
              <span className="text-zinc-500">Plugins kích hoạt:</span>
              <div className="flex flex-wrap gap-1 mt-1.5">
                {run.plugins.map((p) => (
                  <Badge key={p} variant="outline" className="text-[10px] bg-zinc-900 border-zinc-800 py-0.5 text-zinc-400">
                    {p.split(':').pop() || p}
                  </Badge>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Live progress cockpit */}
        <div className="rounded-xl border border-zinc-800 bg-zinc-950 p-6 space-y-6 text-zinc-200">
          <div className="flex items-center justify-between">
            <div className="space-y-1">
              <h3 className="font-bold text-lg text-zinc-100">
                {status === 'PENDING' && 'Đang chuẩn bị quét bảo mật...'}
                {status === 'RUNNING' && 'Đang quét lỗ hổng bảo mật...'}
                {status === 'COMPLETED' && 'Quét bảo mật hoàn tất!'}
                {status === 'FAILED' && 'Đợt quét bảo mật thất bại'}
                {status === 'CANCELLED' && 'Đợt quét đã bị hủy'}
              </h3>
              <p className="text-xs text-zinc-500">
                {status === 'RUNNING' && `Đang xử lý ${currentProgress} trên tổng số ${totalProgress} kịch bản tấn công.`}
                {status === 'COMPLETED' && 'Đang tạo báo cáo và chuyển hướng...'}
                {status === 'PENDING' && 'Đang cấu hình Promptfoo CLI và khởi tạo container...'}
                {(status === 'FAILED' || status === 'CANCELLED') && 'Vui lòng kiểm tra lại cấu hình hoặc log lỗi bên dưới.'}
              </p>
            </div>
            <div>
              {['PENDING', 'RUNNING'].includes(status) && (
                <CircleNotchIcon className="size-6 animate-spin text-red-500" />
              )}
              {status === 'COMPLETED' && (
                <div className="size-7 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-500 flex items-center justify-center">
                  <ShieldCheckIcon size={18} weight="fill" />
                </div>
              )}
              {status === 'FAILED' && (
                <div className="size-7 rounded-full bg-red-500/10 border border-red-500/20 text-red-500 flex items-center justify-center">
                  <ShieldWarningIcon size={18} weight="fill" />
                </div>
              )}
            </div>
          </div>

          {/* Progress bar */}
          <div className="space-y-2">
            <div className="flex items-center justify-between text-xs font-mono">
              <span className="text-zinc-500">Tiến độ quét</span>
              <span className={cn(
                'font-bold',
                status === 'COMPLETED' ? 'text-emerald-500' : 'text-red-500'
              )}>
                {percent}%
              </span>
            </div>
            <div className="h-2 w-full bg-zinc-900 overflow-hidden rounded-full border border-zinc-800">
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

          {/* Error messages if failure occurs */}
          {status === 'FAILED' && errorMsg && (
            <div className="rounded-lg border border-red-950 bg-red-950/20 p-4 space-y-2 text-sm text-red-400">
              <span className="font-semibold block">Chi tiết lỗi từ máy chủ:</span>
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
              Xem báo cáo bảo mật chi tiết
              <ArrowRightIcon weight="bold" size={16} />
            </Button>
          )}
        </div>
      </div>
    </PageShell>
  );
}
