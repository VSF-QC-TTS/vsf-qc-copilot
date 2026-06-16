'use client';

import { useTranslations } from 'next-intl';
import { useQuery } from '@tanstack/react-query';
import { BriefcaseIcon, ListChecksIcon, ArrowRightIcon, PlusIcon } from '@phosphor-icons/react';
import { motion } from 'motion/react';

import { Link } from '@/i18n/navigation';
import { Button } from '@/components/ui/button';
import { MetricCard } from '@/components/ui/metric-card';
import { EmptyState } from '@/components/feedback/empty-state';
import { PageShell } from '@/components/layout/page-shell';
import { StatusBadge } from '@/components/ui/status-badge';
import { useAuthStore } from '@/lib/store/auth-store';
import { apiClient } from '@/lib/api/client';
import type { PageResponse, ProjectResponse } from '@/lib/api/types';

const containerVariants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: {
      staggerChildren: 0.08,
    },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 15 },
  show: {
    opacity: 1,
    y: 0,
    transition: {
      type: 'spring' as const,
      stiffness: 100,
      damping: 15,
    },
  },
};

export default function DashboardPage() {
  const t = useTranslations('dashboard');
  const tCommon = useTranslations('common');
  const user = useAuthStore((s) => s.user);

  // Fetch projects count & list
  const { data: projectsData, isLoading: projectsLoading } = useQuery({
    queryKey: ['projects', 'list', { size: 5 }],
    queryFn: () =>
      apiClient.get<PageResponse<ProjectResponse>>('/api/v1/projects?page=0&size=5&sort=updatedAt,desc'),
  });

  // Fetch rubrics count
  const { data: rubricsData, isLoading: rubricsLoading } = useQuery({
    queryKey: ['rubrics', 'count'],
    queryFn: () =>
      apiClient.get<PageResponse<unknown>>('/api/v1/rubrics?page=0&size=1'),
  });

  const totalProjects = projectsData?.totalItems ?? 0;
  const totalRubrics = rubricsData?.totalItems ?? 0;
  const recentProjects = projectsData?.items ?? [];

  const isLoading = projectsLoading || rubricsLoading;

  return (
    <PageShell
      title={t('welcome', { name: user?.displayName ?? '' })}
      description={t('welcomeSubtitle')}
    >
      {/* Metrics */}
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45, ease: 'easeOut' }}
        className="grid gap-4 sm:grid-cols-2 lg:grid-cols-2"
      >
        <MetricCard
          label={t('totalProjects')}
          value={totalProjects}
          icon={<BriefcaseIcon weight="duotone" />}
          loading={isLoading}
        />
        <MetricCard
          label={t('totalRubrics')}
          value={totalRubrics}
          icon={<ListChecksIcon weight="duotone" />}
          loading={isLoading}
        />
      </motion.div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Active Projects List */}
        <motion.div
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.45, delay: 0.1, ease: 'easeOut' }}
          className="lg:col-span-2 space-y-4"
        >
          <h2 className="text-lg font-semibold tracking-tight">
            {t('activeProjects')}
          </h2>

          {isLoading ? (
            <div className="space-y-3">
              {Array.from({ length: 3 }, (_, i) => (
                <div key={i} className="h-20 animate-pulse rounded-lg border bg-muted/20" />
              ))}
            </div>
          ) : recentProjects.length === 0 ? (
            <EmptyState
              title={t('createFirstProject')}
              description={t('createFirstProjectDesc')}
              icon={<BriefcaseIcon size={48} weight="duotone" />}
              action={
                <Button asChild>
                  <Link href="/projects">{t('createFirstProject')}</Link>
                </Button>
              }
            />
          ) : (
            <motion.div
              variants={containerVariants}
              initial="hidden"
              animate="show"
              className="grid gap-4"
            >
              {recentProjects.map((project) => (
                <motion.div
                  key={project.publicId}
                  variants={itemVariants}
                  whileHover={{ y: -3, transition: { duration: 0.2 } }}
                >
                  <Link
                    href={`/projects/${project.publicId}`}
                    className="group flex flex-col gap-2 rounded-lg border bg-card p-4 transition-colors hover:border-primary/30 hover:shadow-xs duration-200"
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-foreground group-hover:text-primary transition-colors">
                        {project.name}
                      </span>
                      <StatusBadge status={project.status} size="sm" />
                    </div>
                    {project.description && (
                      <p className="text-sm text-muted-foreground line-clamp-1">
                        {project.description}
                      </p>
                    )}
                    <div className="flex items-center justify-between text-xs text-muted-foreground pt-1 border-t border-border/50 mt-1">
                      <span>
                        {tCommon('back')}: {new Date(project.updatedAt).toLocaleDateString()}
                      </span>
                      <span className="flex items-center gap-1 text-primary/80 group-hover:translate-x-0.5 transition-transform">
                        {t('quickActions')} <ArrowRightIcon size={12} weight="bold" />
                      </span>
                    </div>
                  </Link>
                </motion.div>
              ))}
            </motion.div>
          )}
        </motion.div>

        {/* Quick Actions Panel */}
        <motion.div
          initial={{ opacity: 0, x: 15 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.45, delay: 0.2, ease: 'easeOut' }}
          className="space-y-4"
        >
          <h2 className="text-lg font-semibold tracking-tight">
            {t('quickActions')}
          </h2>
          <div className="flex flex-col gap-2 mt-1">
            <Button className="w-full justify-start gap-2" variant="outline" asChild>
              <Link href="/projects">
                <BriefcaseIcon size={16} weight="bold" />
                {t('viewAllProjects')}
              </Link>
            </Button>
            <Button className="w-full justify-start gap-2" variant="outline" asChild>
              <Link href="/rubrics">
                <ListChecksIcon size={16} weight="bold" />
                {t('totalRubrics')}
              </Link>
            </Button>
            <Button className="w-full justify-start gap-2" asChild>
              <Link href="/projects">
                <PlusIcon size={16} weight="bold" />
                {t('createNewProject')}
              </Link>
            </Button>
          </div>
        </motion.div>
      </div>
    </PageShell>
  );
}
