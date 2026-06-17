'use client';

import * as React from 'react';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { type ColumnDef } from '@tanstack/react-table';
import { PlusIcon, PencilSimpleIcon, TrashIcon } from '@phosphor-icons/react';
import { motion } from 'motion/react';

import { Button } from '@/components/ui/button';
import { DataTable } from '@/components/data-table/data-table';
import { DataTablePagination } from '@/components/data-table/data-table-pagination';
import { PageShell } from '@/components/layout/page-shell';
import { StatusBadge } from '@/components/ui/status-badge';
import { JudgeModelDialog } from '@/components/judge-models/create-judge-model-dialog';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { apiClient } from '@/lib/api/client';
import type {
  JudgeModelResponse,
  PageResponse,
} from '@/lib/api/types';

const PAGE_SIZE = 10;
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

function formatDate(iso: string): string {
  if (!iso) return '-';
  const d = new Date(iso);
  return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

export default function JudgeModelsPage() {
  const t = useTranslations('judgeModels');
  const tCommon = useTranslations('common');
  const params = useParams();
  const projectId = params.projectId as string;

  const [page, setPage] = React.useState(0);
  const [createDialogOpen, setCreateDialogOpen] = React.useState(false);
  
  const [editingJudgeModel, setEditingJudgeModel] = React.useState<JudgeModelResponse | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = React.useState(false);
  const [deletingJudgeModel, setDeletingJudgeModel] = React.useState<JudgeModelResponse | null>(null);
  const [isDeleting, setIsDeleting] = React.useState(false);

  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['judge-models', projectId, { page, size: PAGE_SIZE }],
    queryFn: () =>
      apiClient.get<PageResponse<JudgeModelResponse>>(
        `/api/v1/projects/${projectId}/judge-models?page=${page}&size=${PAGE_SIZE}`,
      ),
  });



  const judgeModels = data?.items ?? [];
  const totalItems = data?.totalItems ?? 0;
  const totalPages = data?.totalPages ?? 0;

  async function handleDelete() {
    if (!deletingJudgeModel) return;
    setIsDeleting(true);
    try {
      await apiClient.delete(`/api/v1/projects/${projectId}/judge-models/${deletingJudgeModel.publicId}`);
      await queryClient.invalidateQueries({
        queryKey: ['judge-models', projectId],
      });
      await queryClient.invalidateQueries({
        queryKey: ['judge-models-active', projectId],
      });
      await queryClient.invalidateQueries({
        queryKey: ['project-readiness', projectId],
      });
      setDeleteDialogOpen(false);
      setDeletingJudgeModel(null);
    } catch {
      // apiClient emits toast on error
    } finally {
      setIsDeleting(false);
    }
  }

  const columns = React.useMemo<ColumnDef<JudgeModelResponse, unknown>[]>(
    () => [
      {
        accessorKey: 'name',
        header: t('columns.name'),
        cell: ({ row }) => (
          <span className="font-medium">{row.original.name}</span>
        ),
      },
      {
        accessorKey: 'provider',
        header: t('columns.provider'),
        size: 120,
      },
      {
        accessorKey: 'modelName',
        header: t('columns.model'),
        cell: ({ row }) => (
          <span className="font-mono text-xs text-muted-foreground">
            {row.original.modelName}
          </span>
        ),
      },
      {
        accessorKey: 'apiKeyMasked',
        header: t('columns.apiKey'),
        size: 140,
        cell: ({ row }) => (
          <span className="font-mono text-xs text-muted-foreground">
            {row.original.apiKeyMasked}
          </span>
        ),
      },
      {
        accessorKey: 'active',
        header: t('columns.status'),
        size: 120,
        cell: ({ row }) => (
          <StatusBadge
            status={row.original.active ? 'ACTIVE' : 'INACTIVE'}
            size="sm"
          />
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
        size: 80,
        cell: ({ row }) => (
          <div className="flex items-center justify-end gap-2">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setEditingJudgeModel(row.original)}
              title={tCommon('edit')}
            >
              <PencilSimpleIcon weight="bold" />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              className="text-destructive hover:bg-destructive/10 hover:text-destructive"
              onClick={() => {
                setDeletingJudgeModel(row.original);
                setDeleteDialogOpen(true);
              }}
              title={tCommon('delete')}
            >
              <TrashIcon weight="bold" />
            </Button>
          </div>
        ),
      },
    ],
    [t, tCommon],
  );



  return (
    <PageShell
      title={t('title')}
      description={t('description')}
      backHref={`/projects/${projectId}`}
      backLabel={tCommon('back')}
      actions={
        <Button onClick={() => setCreateDialogOpen(true)}>
          <PlusIcon weight="bold" className="mr-2" />
          {t('createJudgeModel')}
        </Button>
      }
    >
      <motion.div
        variants={containerVariants}
        initial="hidden"
        animate="show"
        className="space-y-4"
      >
        <motion.div variants={itemVariants}>
          <DataTable
            columns={columns}
            data={judgeModels}
            totalItems={totalItems}
            pageIndex={page}
            pageSize={PAGE_SIZE}
            onPaginationChange={setPage}
            loading={isLoading}
            emptyMessage={t('noJudgeModels')}
            emptyAction={
              <Button onClick={() => setCreateDialogOpen(true)}>
                <PlusIcon weight="bold" className="mr-2" />
                {t('createJudgeModel')}
              </Button>
            }
          />
        </motion.div>

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

      <JudgeModelDialog
        open={createDialogOpen}
        onOpenChange={setCreateDialogOpen}
        projectId={projectId}
      />

      <JudgeModelDialog
        open={!!editingJudgeModel}
        onOpenChange={(open) => {
          if (!open) setEditingJudgeModel(null);
        }}
        projectId={projectId}
        initialData={editingJudgeModel}
      />

      <ConfirmDialog
        open={deleteDialogOpen}
        onOpenChange={(open) => {
          if (!open) {
            setDeleteDialogOpen(false);
            setDeletingJudgeModel(null);
          }
        }}
        title={tCommon('delete')}
        description={t('deleteConfirmDescription')}
        confirmLabel={tCommon('delete')}
        cancelLabel={tCommon('cancel')}
        variant="destructive"
        loading={isDeleting}
        onConfirm={handleDelete}
      />
    </PageShell>
  );
}
