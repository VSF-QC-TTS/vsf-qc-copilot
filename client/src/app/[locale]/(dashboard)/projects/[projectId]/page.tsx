'use client';

import * as React from 'react';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  PlugsIcon,
  DatabaseIcon,
  ListChecksIcon,
  ChartBarIcon,
  ArchiveIcon,
} from '@phosphor-icons/react';

import { Link, useRouter } from '@/i18n/navigation';
import { cn } from '@/lib/utils';
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

// ---------------------------------------------------------------------------
// Quick-link definitions
// ---------------------------------------------------------------------------
type QuickLink = {
  labelKey: string;
  href: string;
  icon: React.ElementType;
};

type EvaluationRunSummary = {
  publicId: string;
  status: EvaluationRunStatus;
  createdAt: string;
};

function useQuickLinks(projectId: string): QuickLink[] {
  return React.useMemo(
    () => [
      {
        labelKey: 'connectors',
        href: `/projects/${projectId}/connectors`,
        icon: PlugsIcon,
      },

      {
        labelKey: 'datasets',
        href: `/projects/${projectId}/datasets`,
        icon: DatabaseIcon,
      },
      {
        labelKey: 'rubrics',
        href: '/rubrics',
        icon: ListChecksIcon,
      },
      {
        labelKey: 'evaluations',
        href: `/projects/${projectId}/evaluations`,
        icon: ChartBarIcon,
      },
    ],
    [projectId],
  );
}

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
  const tCommon = useTranslations('common');
  const params = useParams();
  const router = useRouter();
  const queryClient = useQueryClient();
  const projectId = params.projectId as string;

  const [archiveOpen, setArchiveOpen] = React.useState(false);

  // --- Fetch project ---
  const {
    data: project,
    isLoading: projectLoading,
  } = useQuery({
    queryKey: ['project', projectId],
    queryFn: () =>
      apiClient.get<ProjectResponse>('/api/v1/projects/' + projectId),
  });

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

  // --- ArchiveIcon mutation ---
  const archiveMutation = useMutation({
    mutationFn: () => apiClient.del('/api/v1/projects/' + projectId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      queryClient.invalidateQueries({ queryKey: ['project', projectId] });
      router.push('/projects');
    },
  });

  const quickLinks = useQuickLinks(projectId);
  const recentEvaluations = evaluationsData?.items ?? [];

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
      {/* ---- Project info card ---- */}
      <div className="rounded-lg border bg-card p-6 space-y-4">
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
      </div>

      {/* ---- Quick links ---- */}
      <section className="space-y-3">
        <h2 className="text-lg font-semibold tracking-tight">
          {t('quickLinks')}
        </h2>

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {quickLinks.map((link) => {
            const Icon = link.icon;
            return (
              <Link
                key={link.labelKey}
                href={link.href}
                className={cn(
                  'flex items-center gap-3 rounded-lg border bg-card p-4',
                  'transition-colors hover:bg-accent hover:text-accent-foreground',
                )}
              >
                <Icon size={24} weight="duotone" />
                <span className="font-medium">{t(link.labelKey)}</span>
              </Link>
            );
          })}
        </div>
      </section>

      {/* ---- Recent evaluations ---- */}
      <section className="space-y-3">
        <h2 className="text-lg font-semibold tracking-tight">
          {t('recentEvaluations')}
        </h2>

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
          />
        ) : (
          <div className="divide-y rounded-lg border bg-card">
            {recentEvaluations.map((run) => (
              <div
                key={run.publicId}
                className="flex items-center justify-between px-4 py-3"
              >
                <span className="text-sm font-medium truncate">
                  {run.publicId}
                </span>
                <div className="flex items-center gap-3">
                  <StatusBadge status={run.status} size="sm" />
                  <span className="text-xs text-muted-foreground">
                    {new Date(run.createdAt).toLocaleDateString()}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

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
    </PageShell>
  );
}
