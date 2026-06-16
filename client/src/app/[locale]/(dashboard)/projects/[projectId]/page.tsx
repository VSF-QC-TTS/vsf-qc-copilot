'use client';

import * as React from 'react';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  PlusIcon,
  PlugsIcon,
  DatabaseIcon,
  ListChecksIcon,
  ChartBarIcon,
  ArchiveIcon,
  BrainIcon,
  ArrowRightIcon,
  ShieldCheckIcon,
} from '@phosphor-icons/react';

import { Link, useRouter } from '@/i18n/navigation';
import { cn } from '@/lib/utils';
import { useBreadcrumbStore } from '@/lib/store/breadcrumb-store';
import { apiClient } from '@/lib/api/client';
import type {
  EvaluationRunStatus,
  PageResponse,
  ProjectResponse,
} from '@/lib/api/types';
import { Button } from '@/components/ui/button';
import { StatusBadge } from '@/components/ui/status-badge';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { EmptyState } from '@/components/feedback/empty-state';
import { PageShell } from '@/components/layout/page-shell';
import { Skeleton, SkeletonText } from '@/components/feedback/loading-skeleton';
import { StartEvaluationDialog } from '@/components/evaluations/start-evaluation-dialog';
import { motion } from 'motion/react';
import dynamic from 'next/dynamic';

const QualityTrendChart = dynamic(() => import('@/components/projects/quality-trend-chart'), {
  ssr: false,
  loading: () => <div className="h-64 w-full animate-pulse bg-muted/20 rounded-lg" />,
});

// ---------------------------------------------------------------------------
// Types & Motion Variants
// ---------------------------------------------------------------------------

const containerVariants = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.1 } },
};

const itemVariants = {
  hidden: { opacity: 0, y: 15 },
  show: { opacity: 1, y: 0, transition: { type: 'spring' as const, stiffness: 100, damping: 15 } },
};

type EvaluationRunSummary = {
  publicId: string;
  status: EvaluationRunStatus;
  createdAt: string;
  totalCases: number;
  completedCases: number;
  passedCases: number;
  failedCases: number;
};

type ReadinessItem = {
  key: string;
  href: string;
  ready: boolean;
  icon: React.ElementType;
};

// ---------------------------------------------------------------------------
// Loading skeleton
// ---------------------------------------------------------------------------
function ProjectDetailSkeleton() {
  return (
    <div className="space-y-6">
      {/* Header skeleton */}
      <div className="flex items-center justify-between gap-4">
        <div className="space-y-2">
          <Skeleton className="h-7 w-48" />
          <Skeleton className="h-4 w-32" />
        </div>
        <Skeleton className="h-9 w-24" />
      </div>

      {/* Info skeleton */}
      <div className="rounded-lg border bg-card p-6 space-y-4">
        <SkeletonText width="w-3/4" />
        <div className="grid gap-4 sm:grid-cols-2">
          <SkeletonText width="w-1/2" />
          <SkeletonText width="w-1/2" />
        </div>
      </div>

      {/* Quick links skeleton */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 5 }, (_, i) => (
          <Skeleton key={i} className="h-20 rounded-lg" />
        ))}
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Page component
// ---------------------------------------------------------------------------
export default function ProjectDetailPage() {
  const t = useTranslations('projects');
  const tEval = useTranslations('evaluations');
  const tCommon = useTranslations('common');
  const params = useParams();
  const router = useRouter();
  const queryClient = useQueryClient();
  const projectId = params.projectId as string;

  const [archiveOpen, setArchiveOpen] = React.useState(false);
  const [startDialogOpen, setStartDialogOpen] = React.useState(false);

  // --- Fetch project ---
  const {
    data: project,
    isLoading: projectLoading,
  } = useQuery({
    queryKey: ['project', projectId],
    queryFn: () =>
      apiClient.get<ProjectResponse>('/api/v1/projects/' + projectId),
  });

  React.useEffect(() => {
    if (project) {
      useBreadcrumbStore.getState().setMapping(projectId, project.name);
    }
  }, [project, projectId]);

  // --- Fetch recent evaluations ---
  const {
    data: evaluationsData,
    isLoading: evaluationsLoading,
  } = useQuery({
    queryKey: ['evaluations', projectId, 'recent'],
    queryFn: () =>
      apiClient.get<PageResponse<EvaluationRunSummary>>(
        '/api/v1/projects/' +
          projectId +
          '/evaluation-runs?page=0&size=5&sort=createdAt,desc',
      ),
  });

  const { data: approvedDatasetsData } = useQuery({
    queryKey: ['project-readiness', projectId, 'datasets'],
    queryFn: () =>
      apiClient.get<PageResponse<{ publicId: string }>>(
        `/api/v1/projects/${projectId}/datasets?status=APPROVED&page=0&size=1`,
      ),
  });

  const { data: activeConnectorsData } = useQuery({
    queryKey: ['project-readiness', projectId, 'connectors'],
    queryFn: () =>
      apiClient.get<PageResponse<{ publicId: string }>>(
        `/api/v1/projects/${projectId}/target-api-connectors?active=true&page=0&size=1`,
      ),
  });

  const { data: activeJudgeModelsData } = useQuery({
    queryKey: ['project-readiness', projectId, 'judge-models'],
    queryFn: () =>
      apiClient.get<PageResponse<{ publicId: string }>>(
        `/api/v1/projects/${projectId}/judge-models?active=true&page=0&size=1`,
      ),
  });

  const { data: publishedRubricsData } = useQuery({
    queryKey: ['project-readiness', projectId, 'rubric-versions'],
    queryFn: () =>
      apiClient.get<PageResponse<{ publicId: string }>>(
        '/api/v1/rubric-versions?status=PUBLISHED&page=0&size=1',
      ),
  });

  const archiveMutation = useMutation({
    mutationFn: () => apiClient.del('/api/v1/projects/' + projectId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      queryClient.invalidateQueries({ queryKey: ['project', projectId] });
      router.push('/projects');
    },
  });

  const recentEvaluations = evaluationsData?.items ?? [];

  const trendData = React.useMemo(() => {
    if (!evaluationsData) return [];
    
    // Reverse recent runs so they are rendered from oldest to newest (left to right)
    const runs = [...(evaluationsData.items ?? [])].reverse();
    const totalRuns = evaluationsData.totalItems ?? 0;
    
    return runs.map((run, index) => {
      // Index of this run in the full list
      const runIndex = totalRuns - runs.length + index + 1;
      const passRate = run.totalCases > 0 ? Math.round((run.passedCases / run.totalCases) * 100) : 0;
      
      return {
        name: tEval('runNumber', { number: runIndex }),
        passRate,
      };
    });
  }, [evaluationsData, tEval]);

  const readinessItems: ReadinessItem[] = [
    {
      key: 'readinessConnector',
      href: `/projects/${projectId}/connectors`,
      ready: (activeConnectorsData?.totalItems ?? 0) > 0,
      icon: PlugsIcon,
    },
    {
      key: 'readinessDataset',
      href: `/projects/${projectId}/datasets`,
      ready: (approvedDatasetsData?.totalItems ?? 0) > 0,
      icon: DatabaseIcon,
    },
    {
      key: 'readinessJudgeModel',
      href: `/projects/${projectId}/judge-models`,
      ready: (activeJudgeModelsData?.totalItems ?? 0) > 0,
      icon: BrainIcon,
    },
    {
      key: 'readinessRubric',
      href: '/rubrics',
      ready: (publishedRubricsData?.totalItems ?? 0) > 0,
      icon: ListChecksIcon,
    },
  ];
  const allReady = readinessItems.every((item) => item.ready);

  // --- Loading state ---
  if (projectLoading) {
    return (
      <PageShell title={t('overview')} backHref="/projects" backLabel={tCommon('back')}>
        <ProjectDetailSkeleton />
      </PageShell>
    );
  }

  if (!project) {
    return null;
  }

  return (
    <PageShell
      title={project.name}
      description={t('overview')}
      backHref="/projects"
      backLabel={tCommon('back')}
      actions={
        project.status === 'ACTIVE' ? (
          <Button
            variant="outline"
            onClick={() => setArchiveOpen(true)}
          >
            <ArchiveIcon className="mr-2 size-4" weight="bold" />
            {t('archiveProject')}
          </Button>
        ) : undefined
      }
    >
      <motion.div
        variants={containerVariants}
        initial="hidden"
        animate="show"
        className="space-y-6"
      >
        {/* ---- Project info card ---- */}
      <motion.div variants={itemVariants} className="rounded-lg border bg-card p-4 sm:p-6 space-y-4">
        <div className="flex items-center gap-3">
          <StatusBadge status={project.status} />
        </div>

        {project.description && (
          <p className="text-sm text-muted-foreground">
            {project.description}
          </p>
        )}

        <div className="grid gap-4 sm:grid-cols-2 text-sm text-muted-foreground">
          <div>
            <span className="font-medium text-foreground">
              {t('columns.createdAt')}:
            </span>{' '}
            {new Date(project.createdAt).toLocaleDateString()}
          </div>
          <div>
            <span className="font-medium text-foreground">
              {t('columns.updatedAt')}:
            </span>{' '}
            {new Date(project.updatedAt).toLocaleDateString()}
          </div>
        </div>
      </motion.div>

      {/* ---- Contextual Layout based on Readiness ---- */}
      {!allReady ? (
        <motion.section variants={itemVariants} className="space-y-3">
          <div className="flex flex-col gap-1">
            <h2 className="text-lg font-semibold tracking-tight">
              {t('setupChecklist')}
            </h2>
            <p className="text-sm text-muted-foreground">
              {t('readinessMissing')}
            </p>
          </div>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {readinessItems.map((item) => {
              const Icon = item.icon;
              return (
              <Link
                key={item.key}
                href={item.href}
                className={cn(
                  'rounded-lg border bg-card p-4 transition-colors hover:bg-accent hover:text-accent-foreground flex flex-col',
                  item.ready ? 'border-emerald-200' : 'border-amber-200',
                )}
              >
                <div className="flex items-center justify-between gap-3 mb-2">
                  <div className="flex items-center gap-2">
                    <Icon size={20} weight="duotone" className={item.ready ? 'text-emerald-500' : 'text-amber-500'} />
                    <span className="text-sm font-medium">{t(item.key)}</span>
                  </div>
                  <StatusBadge
                    status={item.ready ? 'ACTIVE' : 'PENDING'}
                    size="sm"
                  />
                </div>
                <p className="mt-1 text-xs text-muted-foreground">
                  {item.ready ? t('readinessReady') : t('readinessMissing')}
                </p>
              </Link>
              );
            })}
          </div>
        </motion.section>
      ) : (
        <motion.section variants={itemVariants} className="space-y-6">
          {/* Hero Action Banner */}
          <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/5 p-6 flex flex-col sm:flex-row items-center justify-between gap-6 relative overflow-hidden">
            <div className="absolute -right-10 -top-10 text-emerald-500/10 rotate-12">
               <BrainIcon size={120} weight="fill" />
            </div>
            <div className="flex flex-col gap-2 relative z-10">
              <h2 className="text-2xl font-bold tracking-tight text-emerald-700 dark:text-emerald-500">
                {t('readyToEvaluate')}
              </h2>
              <p className="text-sm text-emerald-700/80 dark:text-emerald-500/80 max-w-[50ch]">
                {t('readyToEvaluateDesc')}
              </p>
            </div>
            <Button size="lg" onClick={() => setStartDialogOpen(true)} className="w-full sm:w-auto shrink-0 bg-emerald-600 hover:bg-emerald-700 text-white shadow-lg shadow-emerald-600/20 z-10">
              <PlusIcon className="mr-2 h-5 w-5" weight="bold" />
              {tEval('startEvaluation')}
            </Button>
          </div>

          {/* Quick Metrics Bento */}
          <div className="grid gap-4 grid-cols-2 sm:grid-cols-3">
            <div className="rounded-xl border bg-card p-4 sm:p-5 flex flex-col justify-center gap-1">
              <div className="text-xs font-medium text-muted-foreground uppercase tracking-wider flex items-center gap-2">
                <ChartBarIcon size={16} />
                {t('latestPassRate')}
              </div>
              <div className="text-3xl font-bold tracking-tight">
                {trendData.length > 0 ? `${trendData[trendData.length - 1].passRate}%` : '--%'}
              </div>
            </div>
            <div className="rounded-xl border bg-card p-4 sm:p-5 flex flex-col justify-center gap-1">
              <div className="text-xs font-medium text-muted-foreground uppercase tracking-wider flex items-center gap-2">
                <ArchiveIcon size={16} />
                {t('totalRuns')}
              </div>
              <div className="text-3xl font-bold tracking-tight">
                {evaluationsData?.totalItems ?? 0}
              </div>
            </div>
            <div className="rounded-xl border bg-card p-4 sm:p-5 flex flex-col justify-center gap-1 col-span-2 sm:col-span-1">
              <div className="text-xs font-medium text-muted-foreground uppercase tracking-wider flex items-center gap-2">
                <DatabaseIcon size={16} />
                {t('readiness')}
              </div>
              <div className="flex items-center gap-3 mt-1.5">
                <div className="flex -space-x-2">
                  {readinessItems.map((item) => {
                    const Icon = item.icon;
                    return (
                      <div key={item.key} className="size-8 rounded-full border border-background bg-emerald-100 dark:bg-emerald-900/50 flex items-center justify-center text-emerald-600 dark:text-emerald-400" title={t(item.key)}>
                        <Icon size={14} weight="bold" />
                      </div>
                    )
                  })}
                </div>
                <StatusBadge status="ACTIVE" size="sm" />
              </div>
            </div>
          </div>
        </motion.section>
      )}

      {/* ---- Red-Teaming (Security Testing) Banner ---- */}
      <motion.section variants={itemVariants} className="space-y-3">
        <div className="rounded-xl border border-red-500/20 bg-red-500/5 p-5 flex flex-col sm:flex-row items-center justify-between gap-6 relative overflow-hidden">
          <div className="absolute -right-6 -top-6 text-red-500/10 rotate-12 pointer-events-none">
            <ShieldCheckIcon size={110} weight="fill" />
          </div>
          <div className="flex items-start gap-3 relative z-10">
            <div className="p-2 rounded-lg bg-red-500/10 text-red-500 border border-red-500/20 shrink-0 hidden sm:block">
              <ShieldCheckIcon size={24} weight="duotone" />
            </div>
            <div className="flex flex-col gap-1">
              <h3 className="text-base font-bold tracking-tight text-red-700 dark:text-red-400">
                Kiểm thử bảo mật (Red-Teaming)
              </h3>
              <p className="text-xs text-red-700/80 dark:text-red-400/80 max-w-[55ch] leading-relaxed">
                Tự động giả lập các đòn tấn công xâm nhập bằng Promptfoo để đánh giá độ an toàn của chatbot trước nguy cơ rò rỉ dữ liệu hoặc Prompt Injection.
              </p>
            </div>
          </div>
          <Button variant="outline" asChild className="w-full sm:w-auto shrink-0 border-red-500/30 text-red-600 dark:text-red-400 hover:bg-red-500/10 z-10">
            <Link href={`/projects/${projectId}/red-team`}>
              Quản lý quét bảo mật <ArrowRightIcon className="ml-1.5 size-4" />
            </Link>
          </Button>
        </div>
      </motion.section>

      {/* ---- Recent evaluations ---- */}
      <motion.section variants={itemVariants} className="space-y-3">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <h2 className="text-lg font-semibold tracking-tight">
            {t('recentEvaluations')}
          </h2>
          <Button variant="ghost" size="sm" asChild className="w-fit -mr-3 text-muted-foreground hover:text-foreground">
            <Link href={`/projects/${projectId}/evaluations`}>
              {tCommon('viewAll')} <ArrowRightIcon className="ml-1 size-4" />
            </Link>
          </Button>
        </div>

        {evaluationsLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 3 }, (_, i) => (
              <Skeleton key={i} className="h-12 rounded-lg" />
            ))}
          </div>
        ) : recentEvaluations.length === 0 ? (
          <EmptyState
            title={t('noEvaluations')}
            icon={<ChartBarIcon size={48} weight="duotone" />}
            action={
              !allReady ? (
                <Button variant="outline" asChild>
                  <Link href="#setup-checklist">{t('readinessMissing')}</Link>
                </Button>
              ) : undefined
            }
          />
        ) : (
          <div className="grid gap-6 lg:grid-cols-3">
            {/* Trend Chart (left/top) */}
            <div className="lg:col-span-2 rounded-lg border bg-card p-4 shadow-xs flex flex-col justify-between">
              <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-2">
                {tEval('passRate')}
              </h3>
              {trendData.length > 1 ? (
                <QualityTrendChart data={trendData} />
              ) : (
                <div className="h-64 flex items-center justify-center text-sm text-muted-foreground">
                  {tCommon('notAvailable')}
                </div>
              )}
            </div>

            {/* Recent Runs list (right/bottom) */}
            <div className="lg:col-span-1 divide-y rounded-lg border bg-card h-fit">
              {recentEvaluations.map((run, index) => {
                const runNumber = (evaluationsData?.totalItems ?? 0) - index;
                return (
                  <Link
                    key={run.publicId}
                    href={`/projects/${projectId}/evaluations/${run.publicId}`}
                    className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3 sm:gap-4 px-4 py-3 transition-colors hover:bg-muted/50"
                  >
                    <div className="flex flex-col min-w-0">
                      <span className="text-sm font-medium text-foreground">
                        {tEval('runNumber', { number: runNumber })}
                      </span>
                      <span className="text-[10px] text-muted-foreground font-mono truncate">
                        {run.publicId.slice(0, 8)}
                      </span>
                    </div>
                    <div className="flex items-center gap-3 w-full sm:w-auto justify-between sm:justify-end sm:shrink-0">
                      <StatusBadge status={run.status} size="sm" />
                      <span className="text-xs text-muted-foreground">
                        {new Date(run.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                  </Link>
                );
              })}
            </div>
          </div>
        )}
      </motion.section>
      </motion.div>

      {/* ---- ArchiveIcon confirm dialog ---- */}
      <ConfirmDialog
        open={archiveOpen}
        onOpenChange={setArchiveOpen}
        title={t('archiveProject')}
        description={t('archiveConfirm', { name: project.name })}
        confirmLabel={t('archiveProject')}
        variant="destructive"
        loading={archiveMutation.isPending}
        onConfirm={() => archiveMutation.mutate()}
      />

      {/* ---- Start Evaluation Dialog ---- */}
      <StartEvaluationDialog
        open={startDialogOpen}
        onOpenChange={setStartDialogOpen}
        projectId={projectId}
      />
    </PageShell>
  );
}
