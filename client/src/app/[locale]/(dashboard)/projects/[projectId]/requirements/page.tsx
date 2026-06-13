'use client';

import { useState, useCallback } from 'react';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, PencilSimple, Archive, X, FloppyDisk, ListDashes } from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { PageShell } from '@/components/layout/page-shell';
import { StatusBadge } from '@/components/ui/status-badge';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { apiClient } from '@/lib/api/client';
import type { PageResponse } from '@/lib/api/types';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type RequirementStatus = 'ACTIVE' | 'ARCHIVED';

type RequirementResponse = {
  publicId: string;
  content: string;
  version: number;
  status: RequirementStatus;
  createdAt: string;
  updatedAt: string;
};

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

function truncate(text: string, maxLength = 200): string {
  if (text.length <= maxLength) return text;
  return text.slice(0, maxLength) + '…';
}

// ---------------------------------------------------------------------------
// Page component
// ---------------------------------------------------------------------------

export default function RequirementsPage() {
  const t = useTranslations('requirements');
  const { projectId } = useParams<{ projectId: string }>();
  const queryClient = useQueryClient();

  // State
  const [statusFilter, setStatusFilter] = useState<RequirementStatus>('ACTIVE');
  const [showCreate, setShowCreate] = useState(false);
  const [createContent, setCreateContent] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editContent, setEditContent] = useState('');
  const [archiveTarget, setArchiveTarget] = useState<string | null>(null);

  // Query key
  const queryKey = ['requirements', projectId, statusFilter];

  // ---------------------------------------------------------------------------
  // Fetch requirements
  // ---------------------------------------------------------------------------

  const { data, isLoading } = useQuery({
    queryKey,
    queryFn: () =>
      apiClient.get<PageResponse<RequirementResponse>>(
        `/api/v1/projects/${projectId}/requirements?page=0&size=50&status=${statusFilter}`,
      ),
  });

  const requirements = data?.items ?? [];

  // ---------------------------------------------------------------------------
  // Mutations
  // ---------------------------------------------------------------------------

  const createMutation = useMutation({
    mutationFn: (content: string) =>
      apiClient.post<RequirementResponse>(
        `/api/v1/projects/${projectId}/requirements`,
        { content },
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['requirements', projectId] });
      setCreateContent('');
      setShowCreate(false);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ reqId, content }: { reqId: string; content: string }) =>
      apiClient.patch<RequirementResponse>(
        `/api/v1/requirements/${reqId}`,
        { content },
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['requirements', projectId] });
      setEditingId(null);
      setEditContent('');
    },
  });

  const archiveMutation = useMutation({
    mutationFn: (reqId: string) =>
      apiClient.patch<RequirementResponse>(
        `/api/v1/requirements/${reqId}`,
        { status: 'ARCHIVED' },
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['requirements', projectId] });
      setArchiveTarget(null);
    },
  });

  // ---------------------------------------------------------------------------
  // Handlers
  // ---------------------------------------------------------------------------

  const handleCreate = useCallback(() => {
    const trimmed = createContent.trim();
    if (!trimmed) return;
    createMutation.mutate(trimmed);
  }, [createContent, createMutation]);

  const handleStartEdit = useCallback((req: RequirementResponse) => {
    setEditingId(req.publicId);
    setEditContent(req.content);
  }, []);

  const handleSaveEdit = useCallback(() => {
    if (!editingId) return;
    const trimmed = editContent.trim();
    if (!trimmed) return;
    updateMutation.mutate({ reqId: editingId, content: trimmed });
  }, [editingId, editContent, updateMutation]);

  const handleCancelEdit = useCallback(() => {
    setEditingId(null);
    setEditContent('');
  }, []);

  const handleArchiveConfirm = useCallback(() => {
    if (!archiveTarget) return;
    archiveMutation.mutate(archiveTarget);
  }, [archiveTarget, archiveMutation]);

  // ---------------------------------------------------------------------------
  // Filter tabs
  // ---------------------------------------------------------------------------

  const filterOptions: { label: string; value: RequirementStatus }[] = [
    { label: t('filterActive'), value: 'ACTIVE' },
    { label: t('filterArchived'), value: 'ARCHIVED' },
  ];

  // ---------------------------------------------------------------------------
  // Render
  // ---------------------------------------------------------------------------

  return (
    <PageShell
      title={t('title')}
      description={t('description')}
      actions={
        <Button onClick={() => setShowCreate((prev) => !prev)}>
          {showCreate ? (
            <>
              <X weight="bold" />
              {t('cancelCreate')}
            </>
          ) : (
            <>
              <Plus weight="bold" />
              {t('createRequirement')}
            </>
          )}
        </Button>
      }
    >
      {/* Inline create form */}
      {showCreate && (
        <div className="rounded-lg border bg-card p-4 space-y-3">
          <label htmlFor="create-content" className="text-sm font-medium">
            {t('contentLabel')}
          </label>
          <textarea
            id="create-content"
            className={cn(
              'w-full min-h-[120px] rounded-md border bg-background px-3 py-2 text-sm',
              'placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
              'resize-y',
            )}
            placeholder={t('contentPlaceholder')}
            value={createContent}
            onChange={(e) => setCreateContent(e.target.value)}
            maxLength={5000}
          />
          <div className="flex justify-end">
            <Button
              onClick={handleCreate}
              disabled={!createContent.trim() || createMutation.isPending}
            >
              <FloppyDisk weight="bold" />
              {t('save')}
            </Button>
          </div>
        </div>
      )}

      {/* Status filter tabs */}
      <div className="flex items-center gap-1">
        {filterOptions.map((opt) => (
          <Button
            key={opt.value}
            variant="outline"
            size="sm"
            className={cn(
              statusFilter === opt.value &&
                'bg-accent text-accent-foreground',
            )}
            onClick={() => setStatusFilter(opt.value)}
          >
            {opt.label}
          </Button>
        ))}
      </div>

      {/* Requirements list */}
      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <span className="text-sm text-muted-foreground">{t('loading')}</span>
        </div>
      ) : requirements.length === 0 ? (
        /* Empty state */
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <ListDashes className="size-12 text-muted-foreground/50" weight="thin" />
          <h3 className="mt-4 text-lg font-medium">{t('emptyTitle')}</h3>
          <p className="mt-1 text-sm text-muted-foreground">
            {t('emptyDescription')}
          </p>
          {statusFilter === 'ACTIVE' && (
            <Button className="mt-4" onClick={() => setShowCreate(true)}>
              <Plus weight="bold" />
              {t('createRequirement')}
            </Button>
          )}
        </div>
      ) : (
        <div className="space-y-3">
          {requirements.map((req) => (
            <div
              key={req.publicId}
              className={cn(
                'rounded-lg border bg-card p-4 transition-colors',
                editingId !== req.publicId && 'hover:border-foreground/20',
              )}
            >
              {editingId === req.publicId ? (
                /* Inline edit mode */
                <div className="space-y-3">
                  <textarea
                    className={cn(
                      'w-full min-h-[120px] rounded-md border bg-background px-3 py-2 text-sm',
                      'placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
                      'resize-y',
                    )}
                    value={editContent}
                    onChange={(e) => setEditContent(e.target.value)}
                    maxLength={5000}
                  />
                  <div className="flex items-center justify-end gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={handleCancelEdit}
                      disabled={updateMutation.isPending}
                    >
                      {t('cancel')}
                    </Button>
                    <Button
                      size="sm"
                      onClick={handleSaveEdit}
                      disabled={!editContent.trim() || updateMutation.isPending}
                    >
                      <FloppyDisk weight="bold" />
                      {t('save')}
                    </Button>
                  </div>
                </div>
              ) : (
                /* Display mode */
                <>
                  <div className="flex items-start justify-between gap-4">
                    <p className="text-sm leading-relaxed whitespace-pre-wrap">
                      {truncate(req.content)}
                    </p>
                    <div className="flex shrink-0 items-center gap-1">
                      {req.status === 'ACTIVE' && (
                        <>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleStartEdit(req)}
                          >
                            <PencilSimple weight="bold" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setArchiveTarget(req.publicId)}
                          >
                            <Archive weight="bold" />
                          </Button>
                        </>
                      )}
                    </div>
                  </div>
                  <div className="mt-3 flex items-center gap-3 text-xs text-muted-foreground">
                    <StatusBadge status={req.status} size="sm" />
                    <span>{t('version', { number: req.version })}</span>
                    <span>{formatDate(req.updatedAt)}</span>
                  </div>
                </>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Archive confirm dialog */}
      <ConfirmDialog
        open={archiveTarget !== null}
        onOpenChange={(open) => {
          if (!open) setArchiveTarget(null);
        }}
        title={t('archiveTitle')}
        description={t('archiveDescription')}
        confirmLabel={t('archiveConfirm')}
        cancelLabel={t('cancel')}
        variant="destructive"
        onConfirm={handleArchiveConfirm}
        loading={archiveMutation.isPending}
      />
    </PageShell>
  );
}
