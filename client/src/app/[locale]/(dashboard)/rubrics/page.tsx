'use client';

import { useState, useMemo, useCallback } from 'react';
import { useTranslations } from 'next-intl';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { type ColumnDef } from '@tanstack/react-table';
import { PlusIcon, CopyIcon, ListChecksIcon } from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { PageShell } from '@/components/layout/page-shell';
import { DataTable } from '@/components/data-table/data-table';
import { DataTablePagination } from '@/components/data-table/data-table-pagination';
import { CreateRubricDialog } from '@/components/rubrics/create-rubric-dialog';
import { motion, AnimatePresence } from 'motion/react';
import { apiClient } from '@/lib/api/client';
import type { PageResponse } from '@/lib/api/types';
import { useRouter } from '@/i18n/navigation';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type RubricResponse = {
  publicId: string;
  name: string;
  description: string | null;
  projectName: string | null;
  createdAt: string;
  updatedAt: string;
};

// ---------------------------------------------------------------------------
// Tab type
// ---------------------------------------------------------------------------

type TabValue = 'my' | 'templates';
const PAGE_SIZE = 10;

// ---------------------------------------------------------------------------
// Date formatter
// ---------------------------------------------------------------------------

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

// ---------------------------------------------------------------------------
// Motion Variants
// ---------------------------------------------------------------------------

const containerVariants = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.1 } },
};

const itemVariants = {
  hidden: { opacity: 0, y: 15 },
  show: { opacity: 1, y: 0, transition: { type: 'spring' as const, stiffness: 100, damping: 15 } },
};

// ---------------------------------------------------------------------------
// Page component
// ---------------------------------------------------------------------------

export default function RubricsPage() {
  const t = useTranslations('rubrics');
  const tCommon = useTranslations('common');
  const router = useRouter();
  const queryClient = useQueryClient();

  // State
  const [activeTab, setActiveTab] = useState<TabValue>('my');
  const [page, setPage] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);

  // Fetch my rubrics
  const { data: myData, isLoading: myLoading } = useQuery({
    queryKey: ['rubrics', 'my', { page, size: PAGE_SIZE }],
    queryFn: () =>
      apiClient.get<PageResponse<RubricResponse>>(
        `/api/v1/rubrics?page=${page}&size=${PAGE_SIZE}`,
      ),
    enabled: activeTab === 'my',
  });

  // Fetch templates
  const { data: templateData, isLoading: templateLoading } = useQuery({
    queryKey: ['rubrics', 'templates', { page, size: PAGE_SIZE }],
    queryFn: () =>
      apiClient.get<PageResponse<RubricResponse>>(
        `/api/v1/rubrics/templates?page=${page}&size=${PAGE_SIZE}`,
      ),
    enabled: activeTab === 'templates',
  });

  const data = activeTab === 'my' ? myData : templateData;
  const isLoading = activeTab === 'my' ? myLoading : templateLoading;
  const rubrics = data?.items ?? [];
  const totalItems = data?.totalItems ?? 0;
  const totalPages = data?.totalPages ?? 0;

  // Clone mutation
  const cloneMutation = useMutation({
    mutationFn: (rubricId: string) =>
      apiClient.post<RubricResponse>(`/api/v1/rubrics/${rubricId}/clone`),
    onSuccess: (cloned) => {
      void queryClient.invalidateQueries({ queryKey: ['rubrics'] });
      router.push(`/rubrics/${cloned.publicId}`);
    },
  });

  const handleClone = useCallback(
    (e: React.MouseEvent, rubricId: string) => {
      e.stopPropagation();
      cloneMutation.mutate(rubricId);
    },
    [cloneMutation],
  );

  // Table columns — My Rubrics
  const myColumns = useMemo<ColumnDef<RubricResponse, unknown>[]>(
    () => [
      {
        accessorKey: 'name',
        header: t('columns.name'),
        cell: ({ row }) => (
          <span className="font-medium">{row.original.name}</span>
        ),
      },
      {
        accessorKey: 'projectName',
        header: t('columns.project'),
        size: 160,
        cell: ({ row }) => (
          <span className="text-muted-foreground">
            {row.original.projectName ?? tCommon('notAvailable')}
          </span>
        ),
      },
      {
        accessorKey: 'createdAt',
        header: t('columns.createdAt'),
        size: 140,
        cell: ({ row }) => (
          <span className="text-muted-foreground">
            {formatDate(row.original.createdAt)}
          </span>
        ),
      },
    ],
    [t, tCommon],
  );

  // Table columns — Templates (with clone button)
  const templateColumns = useMemo<ColumnDef<RubricResponse, unknown>[]>(
    () => [
      {
        accessorKey: 'name',
        header: t('columns.name'),
        cell: ({ row }) => (
          <span className="font-medium">{row.original.name}</span>
        ),
      },
      {
        accessorKey: 'createdAt',
        header: t('columns.createdAt'),
        size: 140,
        cell: ({ row }) => (
          <span className="text-muted-foreground">
            {formatDate(row.original.createdAt)}
          </span>
        ),
      },
      {
        id: 'actions',
        header: '',
        size: 100,
        cell: ({ row }) => (
          <Button
            variant="outline"
            size="sm"
            disabled={cloneMutation.isPending}
            onClick={(e) => handleClone(e, row.original.publicId)}
          >
            <CopyIcon weight="bold" className="mr-1 size-4" />
            {t('clone')}
          </Button>
        ),
      },
    ],
    [t, cloneMutation.isPending, handleClone],
  );

  const columns = activeTab === 'my' ? myColumns : templateColumns;

  // Handlers
  const handleRowClick = (row: RubricResponse) => {
    if (activeTab === 'my') {
      router.push(`/rubrics/${row.publicId}`);
    }
  };

  const handlePaginationChange = (nextPage: number) => {
    setPage(nextPage);
  };

  const handleTabChange = (tab: TabValue) => {
    setActiveTab(tab);
    setPage(0);
  };

  // Tab options
  const tabs: { label: string; value: TabValue }[] = [
    { label: t('myRubrics'), value: 'my' },
    { label: t('templates'), value: 'templates' },
  ];

  return (
    <PageShell
      title={t('title')}
      actions={
        <Button onClick={() => setDialogOpen(true)}>
          <PlusIcon weight="bold" />
          {t('createRubric')}
        </Button>
      }
    >
      <motion.div
        variants={containerVariants}
        initial="hidden"
        animate="show"
        className="space-y-6"
      >
        <motion.div variants={itemVariants} className="flex items-center gap-1 p-1 bg-muted/40 border border-border/50 rounded-lg w-fit overflow-x-auto flex-nowrap">
          {tabs.map((tab) => {
            const isActive = activeTab === tab.value;
            return (
              <button
                key={tab.value}
                onClick={() => handleTabChange(tab.value)}
                className={cn(
                  "relative px-4 py-1.5 text-sm font-medium rounded-md transition-colors whitespace-nowrap",
                  isActive ? "text-foreground" : "text-muted-foreground hover:text-foreground hover:bg-muted/50"
                )}
              >
                {isActive && (
                  <motion.div
                    layoutId="rubricsFilterTab"
                    className="absolute inset-0 bg-background shadow-xs border border-border/50 rounded-md"
                    transition={{ type: "spring", stiffness: 400, damping: 30 }}
                  />
                )}
                <span className="relative z-10">{tab.label}</span>
              </button>
            );
          })}
        </motion.div>

        <AnimatePresence mode="wait">
          <motion.div
            key={activeTab}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.2 }}
            className="space-y-4"
          >
            {/* Data views */}
            <motion.div variants={itemVariants}>
              {activeTab === 'my' ? (
                <DataTable
                  columns={columns}
                  data={rubrics}
                  totalItems={totalItems}
                  pageIndex={page}
                  pageSize={PAGE_SIZE}
                  onPaginationChange={handlePaginationChange}
                  loading={isLoading}
                  onRowClick={handleRowClick}
                  emptyMessage={t('noRubrics')}
                  emptyAction={
                    <Button onClick={() => setDialogOpen(true)}>
                      <ListChecksIcon weight="bold" className="mr-2" />
                      {t('createRubric')}
                    </Button>
                  }
                />
              ) : (
                <>
                  {isLoading ? (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 animate-pulse">
                      {[1, 2, 3].map((i) => (
                        <div key={i} className="h-40 rounded-xl bg-muted/50 border border-border/50"></div>
                      ))}
                    </div>
                  ) : rubrics.length > 0 ? (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                      {rubrics.map((template) => (
                        <div 
                          key={template.publicId} 
                          className="group relative flex flex-col justify-between rounded-xl border border-border bg-card p-6 shadow-sm transition-all hover:border-primary/50 hover:shadow-md"
                        >
                          <div className="space-y-3">
                            <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10 text-primary group-hover:bg-primary/20 transition-colors">
                              <ListChecksIcon size={24} weight="duotone" />
                            </div>
                            <div>
                              <h3 className="font-semibold text-lg tracking-tight line-clamp-1">{template.name}</h3>
                              {template.description && (
                                <p className="mt-1 text-sm text-muted-foreground line-clamp-2 leading-relaxed">
                                  {template.description}
                                </p>
                              )}
                            </div>
                          </div>
                          <div className="mt-6 flex items-center justify-between">
                            <span className="text-xs text-muted-foreground">
                              {formatDate(template.createdAt)}
                            </span>
                            <Button
                              variant="secondary"
                              size="sm"
                              className="w-full sm:w-auto font-medium"
                              disabled={cloneMutation.isPending}
                              onClick={(e) => handleClone(e, template.publicId)}
                            >
                              <CopyIcon weight="bold" className="mr-2 size-4" />
                              {t('clone')}
                            </Button>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border/60 bg-muted/20 py-16 text-center">
                      <ListChecksIcon className="size-10 text-muted-foreground/50 mb-4" />
                      <p className="text-sm font-medium text-muted-foreground">{t('noRubrics')}</p>
                    </div>
                  )}
                </>
              )}
            </motion.div>

            {/* Pagination */}
            {totalItems > 0 && (
              <motion.div variants={itemVariants}>
                <DataTablePagination
                  pageIndex={page}
                  pageSize={PAGE_SIZE}
                  totalItems={totalItems}
                  totalPages={totalPages}
                  onPageChange={setPage}
                />
              </motion.div>
            )}
          </motion.div>
        </AnimatePresence>
      </motion.div>

      {/* Create rubric dialog */}
      <CreateRubricDialog open={dialogOpen} onOpenChange={setDialogOpen} />
    </PageShell>
  );
}
