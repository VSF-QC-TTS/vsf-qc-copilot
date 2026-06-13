'use client';

import { useTranslations } from 'next-intl';
import { useQuery } from '@tanstack/react-query';
import { Briefcase } from '@phosphor-icons/react';

import { Link } from '@/i18n/navigation';
import { Button } from '@/components/ui/button';
import { MetricCard } from '@/components/ui/metric-card';
import { EmptyState } from '@/components/feedback/empty-state';
import { PageShell } from '@/components/layout/page-shell';
import { useAuthStore } from '@/lib/store/auth-store';
import { apiClient } from '@/lib/api/client';
import type { PageResponse } from '@/lib/api/types';

export default function DashboardPage() {
  const t = useTranslations('dashboard');
  const user = useAuthStore((s) => s.user);

  const { data, isLoading } = useQuery({
    queryKey: ['projects', 'count'],
    queryFn: () =>
      apiClient.get<PageResponse<any>>('/api/v1/projects?page=0&size=1'),
  });

  const totalProjects = data?.totalItems ?? 0;

  return (
    <PageShell
      title={t('welcome', { name: user?.displayName ?? '' })}
      description={t('welcomeSubtitle')}
    >
      {/* Metrics */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <MetricCard
          label={t('totalProjects')}
          value={totalProjects}
          icon={<Briefcase weight="duotone" />}
          loading={isLoading}
        />
      </div>

      {/* Empty state */}
      {!isLoading && totalProjects === 0 && (
        <EmptyState
          title={t('createFirstProject')}
          description={t('createFirstProjectDesc')}
          icon={<Briefcase size={48} weight="duotone" />}
          action={
            <Button asChild>
              <Link href="/projects">{t('createFirstProject')}</Link>
            </Button>
          }
        />
      )}

      {/* Quick actions */}
      <div className="flex items-center gap-3">
        <Button variant="outline" asChild>
          <Link href="/projects">{t('viewAllProjects')}</Link>
        </Button>
      </div>
    </PageShell>
  );
}
