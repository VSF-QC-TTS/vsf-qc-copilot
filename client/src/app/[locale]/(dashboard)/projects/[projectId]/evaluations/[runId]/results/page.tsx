'use client';

import { useState, useMemo, useCallback } from 'react';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useQuery } from '@tanstack/react-query';
import { type ColumnDef } from '@tanstack/react-table';
import { ArrowLeft } from '@phosphor-icons/react';

import { Button } from '@/components/ui/button';
import { PageShell } from '@/components/layout/page-shell';
import { DataTable } from '@/components/data-table/data-table';
import { DataTablePagination } from '@/components/data-table/data-table-pagination';
import { StatusBadge } from '@/components/ui/status-badge';
import {
  ResultDetailPanel,
  type EvaluationResultRow,
} from '@/components/panels/result-detail-panel';
import { apiClient } from '@/lib/api/client';
import type { PageResponse } from '@/lib/api/types';
import { useRouter } from '@/i18n/navigation';

// ---------------------------------------------------------------------------
// Filter constants
// ---------------------------------------------------------------------------

const JUDGE_STATUS_OPTIONS = ['', 'PASS', 'FAIL', 'WARNING', 'ERROR'] as const;
const QC_STATUS_OPTIONS = [
  '',
  'NOT_REVIEWED',
  'PASS',
  'FAIL',
  'NEED_FIX',
  'IGNORED',
] as const;

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function truncate(str: string, max = 80): string {
  return str.length > max ? str.slice(0, max) + '...' : str;
}

// ---------------------------------------------------------------------------
// Page component
// ---------------------------------------------------------------------------

export default function ResultsPage() {
  const t = useTranslations('evaluations');
  const tDetail = useTranslations('resultDetail');
  const tCommon = useTranslations('common');
  const router = useRouter();
  const params = useParams();
  const projectId = params.projectId as string;
  const runId = params.runId as string;

  const [page, setPage] = useState(0);
  const pageSize = 20;
  const [judgeFilter, setJudgeFilter] = useState('');
  const [qcFilter, setQcFilter] = useState('');
  const [selectedIdx, setSelectedIdx] = useState<number | null>(null);

  // Build query params
  const queryParams = useMemo(() => {
    const parts = [`page=${page}`, `size=${pageSize}`];
    if (judgeFilter) parts.push(`judgeStatus=${judgeFilter}`);
    if (qcFilter) parts.push(`qcStatus=${qcFilter}`);
    return parts.join('&');
  }, [page, pageSize, judgeFilter, qcFilter]);

  // Fetch results
  const { data, isLoading } = useQuery({
    queryKey: ['evaluation-results', runId, queryParams],
    queryFn: () =>
      apiClient.get<PageResponse<EvaluationResultRow>>(
        `/api/v1/evaluation-runs/${runId}/results?${queryParams}`,
      ),
  });

  const results = data?.items ?? [];
  const totalItems = data?.totalItems ?? 0;
  const totalPages = data?.totalPages ?? 0;

  const selectedResult = selectedIdx !== null ? (results[selectedIdx] ?? null) : null;

  const handlePrev = useCallback(() => {
    if (selectedIdx !== null && selectedIdx > 0) {
      setSelectedIdx(selectedIdx - 1);
    }
  }, [selectedIdx]);

  const handleNext = useCallback(() => {
    if (selectedIdx !== null && selectedIdx < results.length - 1) {
      setSelectedIdx(selectedIdx + 1);
    }
  }, [selectedIdx, results.length]);

  // Columns
  const columns = useMemo<ColumnDef<EvaluationResultRow, unknown>[]>(
    () => [
      {
        accessorKey: 'question',
        header: tDetail('question'),
        cell: ({ row }) => (
          <span className="font-medium">
            {truncate(row.original.question)}
          </span>
        ),
      },
      {
        accessorKey: 'judgeStatus',
        header: tDetail('judgeStatus'),
        size: 130,
        cell: ({ row }) =>
          row.original.judgeStatus ? (
            <StatusBadge status={row.original.judgeStatus} size="sm" />
          ) : (
            <span className="text-muted-foreground">{tCommon('notAvailable')}</span>
          ),
      },
      {
        accessorKey: 'judgeScore',
        header: tDetail('judgeScore'),
        size: 90,
        cell: ({ row }) => (
          <span className="text-muted-foreground">
            {row.original.judgeScore !== null
              ? row.original.judgeScore
              : tCommon('notAvailable')}
          </span>
        ),
      },
      {
        accessorKey: 'qcStatus',
        header: tDetail('qcStatus'),
        size: 130,
        cell: ({ row }) => (
          <StatusBadge status={row.original.qcStatus} size="sm" />
        ),
      },
    ],
    [tDetail, tCommon],
  );

  const handleRowClick = (row: EvaluationResultRow) => {
    const idx = results.findIndex((r) => r.publicId === row.publicId);
    setSelectedIdx(idx >= 0 ? idx : null);
  };

  return (
    <PageShell
      title={t('results')}
      actions={
        <Button
          variant="outline"
          onClick={() =>
            router.push(
              `/projects/${projectId}/evaluations/${runId}`,
            )
          }
        >
          <ArrowLeft weight="bold" />
          {t('runDetail')}
        </Button>
      }
    >
      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="flex items-center gap-2">
          <label
            htmlFor="judge-filter"
            className="text-sm text-muted-foreground"
          >
            {tDetail('judgeStatus')}
          </label>
          <select
            id="judge-filter"
            value={judgeFilter}
            onChange={(e) => {
              setJudgeFilter(e.target.value);
              setPage(0);
            }}
            className="h-8 rounded-md border border-input bg-background px-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            {JUDGE_STATUS_OPTIONS.map((opt) => (
              <option key={opt} value={opt}>
                {opt || tCommon('all')}
              </option>
            ))}
          </select>
        </div>

        <div className="flex items-center gap-2">
          <label
            htmlFor="qc-filter"
            className="text-sm text-muted-foreground"
          >
            {tDetail('qcStatus')}
          </label>
          <select
            id="qc-filter"
            value={qcFilter}
            onChange={(e) => {
              setQcFilter(e.target.value);
              setPage(0);
            }}
            className="h-8 rounded-md border border-input bg-background px-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            {QC_STATUS_OPTIONS.map((opt) => (
              <option key={opt} value={opt}>
                {opt || tCommon('all')}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Table */}
      <DataTable
        columns={columns}
        data={results}
        totalItems={totalItems}
        pageIndex={page}
        pageSize={pageSize}
        onPaginationChange={(nextPage) => setPage(nextPage)}
        loading={isLoading}
        onRowClick={handleRowClick}
        emptyMessage={t('noEvaluations')}
      />

      {totalItems > 0 && (
        <DataTablePagination
          pageIndex={page}
          pageSize={pageSize}
          totalItems={totalItems}
          totalPages={totalPages}
          onPageChange={setPage}
          onPageSizeChange={() => setPage(0)}
        />
      )}

      {/* Result detail panel */}
      <ResultDetailPanel
        result={selectedResult}
        onClose={() => setSelectedIdx(null)}
        onPrev={
          selectedIdx !== null && selectedIdx > 0
            ? handlePrev
            : null
        }
        onNext={
          selectedIdx !== null && selectedIdx < results.length - 1
            ? handleNext
            : null
        }
      />
    </PageShell>
  );
}
