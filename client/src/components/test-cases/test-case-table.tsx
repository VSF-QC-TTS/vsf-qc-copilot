'use client';

import * as React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslations } from 'next-intl';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { StatusBadge } from '@/components/ui/status-badge';
import { apiClient } from '@/lib/api/client';
import type { PageResponse, TestCaseStatus } from '@/lib/api/types';
import {
  Plus,
  UploadSimple,
  Robot,
  CaretLeft,
  CaretRight,
} from '@phosphor-icons/react';

import { TestCaseEditor } from './test-case-editor';
import { ImportDialog } from './import-dialog';
import { AIGenerateDialog } from './ai-generate-dialog';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type TestCaseRow = {
  publicId: string;
  question: string;
  groundTruth: string | null;
  precondition: string | null;
  metadata: string | null;
  status: TestCaseStatus;
  createdAt: string;
  updatedAt: string;
};

interface TestCaseTableProps {
  datasetId: string;
  datasetStatus: string;
  projectId: string;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function truncate(text: string | null, max: number): string {
  if (!text) return '—';
  return text.length > max ? text.slice(0, max) + '…' : text;
}

// ---------------------------------------------------------------------------
// Shared styles
// ---------------------------------------------------------------------------

const thClassName =
  'px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground';
const tdClassName = 'px-4 py-3 text-sm text-foreground';

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function TestCaseTable({
  datasetId,
  datasetStatus,
  projectId,
}: TestCaseTableProps) {
  const t = useTranslations('testCases');
  const tCommon = useTranslations('common');

  const [page, setPage] = React.useState(0);
  const [statusFilter, setStatusFilter] = React.useState<TestCaseStatus>('ACTIVE');

  // Side panel
  const [selectedTestCase, setSelectedTestCase] =
    React.useState<TestCaseRow | null>(null);
  const [editorOpen, setEditorOpen] = React.useState(false);

  // Dialogs
  const [importOpen, setImportOpen] = React.useState(false);
  const [generateOpen, setGenerateOpen] = React.useState(false);

  const isDraft = datasetStatus === 'DRAFT';

  // ---------- Fetch ----------
  const { data, isLoading, isError } = useQuery<PageResponse<TestCaseRow>>({
    queryKey: ['test-cases', datasetId, page, statusFilter],
    queryFn: () =>
      apiClient.get<PageResponse<TestCaseRow>>(
        `/api/v1/datasets/${datasetId}/test-cases?page=${page}&size=20&status=${statusFilter}`,
      ),
  });

  // ---------- Handlers ----------
  function handleRowClick(tc: TestCaseRow) {
    setSelectedTestCase(tc);
    setEditorOpen(true);
  }

  function handleCreate() {
    setSelectedTestCase(null);
    setEditorOpen(true);
  }

  function handleEditorClose() {
    setEditorOpen(false);
    setSelectedTestCase(null);
  }

  // ---------- Render ----------
  return (
    <div data-slot="test-case-table" className="space-y-4">
      {/* Toolbar */}
      <div className="flex items-center justify-between gap-3">
        {/* Status filter */}
        <div className="flex gap-1">
          {(['ACTIVE', 'INACTIVE'] as const).map((s) => (
            <Button
              key={s}
              variant={statusFilter === s ? 'default' : 'outline'}
              size="sm"
              onClick={() => {
                setStatusFilter(s);
                setPage(0);
              }}
            >
              {t(`status${s.charAt(0) + s.slice(1).toLowerCase()}` as 'statusActive')}
            </Button>
          ))}
        </div>

        {/* Actions (DRAFT only) */}
        {isDraft && (
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={handleCreate}>
              <Plus className="mr-1.5 size-4" />
              {t('create')}
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setImportOpen(true)}
            >
              <UploadSimple className="mr-1.5 size-4" />
              {t('import')}
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setGenerateOpen(true)}
            >
              <Robot className="mr-1.5 size-4" />
              {t('aiGenerate')}
            </Button>
          </div>
        )}
      </div>

      {/* Table */}
      {isLoading && (
        <div className="flex justify-center py-12 text-sm text-muted-foreground">
          {tCommon('loading')}
        </div>
      )}

      {isError && (
        <div className="flex justify-center py-12 text-sm text-destructive">
          {tCommon('errorLoadingData')}
        </div>
      )}

      {data && data.items.length === 0 && (
        <div className="flex flex-col items-center justify-center py-16 text-center text-muted-foreground">
          <p className="text-sm">{t('emptyState')}</p>
          {isDraft && (
            <p className="mt-1 text-xs">{t('emptyStateHint')}</p>
          )}
        </div>
      )}

      {data && data.items.length > 0 && (
        <div className="overflow-hidden rounded-md border">
          <table className="w-full">
            <thead className="border-b bg-muted/50">
              <tr>
                <th className={thClassName}>{t('columnQuestion')}</th>
                <th className={thClassName}>{t('columnGroundTruth')}</th>
                <th className={thClassName}>{t('columnStatus')}</th>
                <th className={thClassName}>{t('columnCreatedAt')}</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {data.items.map((tc) => (
                <tr
                  key={tc.publicId}
                  onClick={() => handleRowClick(tc)}
                  className={cn(
                    'cursor-pointer transition-colors hover:bg-muted/50',
                  )}
                >
                  <td className={cn(tdClassName, 'max-w-xs')}>
                    {truncate(tc.question, 80)}
                  </td>
                  <td className={cn(tdClassName, 'max-w-xs')}>
                    {truncate(tc.groundTruth, 80)}
                  </td>
                  <td className={tdClassName}>
                    <StatusBadge status={tc.status} size="sm" />
                  </td>
                  <td className={cn(tdClassName, 'whitespace-nowrap')}>
                    {new Date(tc.createdAt).toLocaleDateString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <span>
            {t('pageInfo', {
              current: page + 1,
              total: data.totalPages,
            })}
          </span>
          <div className="flex gap-1">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
              <CaretLeft className="size-4" />
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={page >= data.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              <CaretRight className="size-4" />
            </Button>
          </div>
        </div>
      )}

      {/* Side panel editor */}
      <TestCaseEditor
        datasetId={datasetId}
        testCase={selectedTestCase}
        isOpen={editorOpen}
        onClose={handleEditorClose}
        isReadOnly={!isDraft}
      />

      {/* Import dialog */}
      <ImportDialog
        datasetId={datasetId}
        open={importOpen}
        onOpenChange={setImportOpen}
      />

      {/* AI Generate dialog */}
      <AIGenerateDialog
        datasetId={datasetId}
        projectId={projectId}
        open={generateOpen}
        onOpenChange={setGenerateOpen}
      />
    </div>
  );
}
