'use client';

import { useState, useMemo, useCallback } from 'react';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useQuery } from '@tanstack/react-query';
import { type ColumnDef } from '@tanstack/react-table';

import { PageShell } from '@/components/layout/page-shell';
import { DataTable } from '@/components/data-table/data-table';
import { DataTablePagination } from '@/components/data-table/data-table-pagination';
import { StatusBadge } from '@/components/ui/status-badge';
import {
  ResultDetailPanel,
  type EvaluationResultRow,
  type CriterionResult,
} from '@/components/panels/result-detail-panel';
import { apiClient } from '@/lib/api/client';
import type { PageResponse } from '@/lib/api/types';

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
const PAGE_SIZE = 10;

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function truncate(str: string, max = 80): string {
  return str.length > max ? str.slice(0, max) + '...' : str;
}

function parseCriteriaJson(raw: string | null): CriterionResult[] {
  if (!raw) return [];
  try {
    const parsed: unknown = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.map((item) => {
      const obj =
        typeof item === 'object' && item !== null
          ? (item as Record<string, unknown>)
          : {};
      return {
        metricKey: typeof obj.metricKey === 'string' ? obj.metricKey : null,
        name: typeof obj.name === 'string' ? obj.name : '',
        status: typeof obj.status === 'string' ? obj.status : '',
        score: typeof obj.score === 'number' ? obj.score : null,
        reason: typeof obj.reason === 'string' ? obj.reason : null,
        graderError: obj.graderError === true,
      };
    });
  } catch {
    return [];
  }
}

function getCriteria(row: EvaluationResultRow): CriterionResult[] {
  if (Array.isArray(row.criteriaResults) && row.criteriaResults.length > 0) {
    return row.criteriaResults;
  }
  return parseCriteriaJson(row.criteriaResultsJson);
}

function CriteriaSummary({ row }: { row: EvaluationResultRow }) {
  const criteria = getCriteria(row);
  if (criteria.length === 0) {
    return <span className="text-muted-foreground">-</span>;
  }

  const passCount = criteria.filter((item) => item.status === 'PASS').length;
  const failLike = criteria.filter(
    (item) =>
      item.status === 'FAIL' ||
      item.status === 'ERROR' ||
      item.status === 'WARNING' ||
      item.graderError,
  );
  const names = failLike
    .map((item) => item.name)
    .filter(Boolean)
    .slice(0, 2)
    .join(', ');

  return (
    <div className="min-w-[160px] space-y-1">
      <div className="flex flex-wrap gap-1">
        <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-800 dark:bg-green-950 dark:text-green-300">
          {passCount}P
        </span>
        {failLike.length > 0 && (
          <span className="rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-800 dark:bg-red-950 dark:text-red-300">
            {failLike.length}F/E
          </span>
        )}
      </div>
      {names && (
        <p className="max-w-[220px] truncate text-xs text-muted-foreground">
          {names}
        </p>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Page component
// ---------------------------------------------------------------------------

export default function ResultsPage() {
  const t = useTranslations('evaluations');
  const tDetail = useTranslations('resultDetail');
  const tCommon = useTranslations('common');
  const params = useParams();
  const projectId = params.projectId as string;
  const runId = params.runId as string;

  const [page, setPage] = useState(0);
  const [judgeFilter, setJudgeFilter] = useState('');
  const [qcFilter, setQcFilter] = useState('');
  const [selectedIdx, setSelectedIdx] = useState<number | null>(null);

  // Build query params
  const queryParams = useMemo(() => {
    const parts = [`page=${page}`, `size=${PAGE_SIZE}`];
    if (judgeFilter) parts.push(`judgeStatus=${judgeFilter}`);
    if (qcFilter) parts.push(`qcStatus=${qcFilter}`);
    return parts.join('&');
  }, [page, judgeFilter, qcFilter]);

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
        id: 'criteria',
        header: tDetail('criteriaBreakdown'),
        size: 190,
        cell: ({ row }) => <CriteriaSummary row={row.original} />,
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
      backHref={`/projects/${projectId}/evaluations/${runId}`}
      backLabel={tCommon('back')}
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
        pageSize={PAGE_SIZE}
        onPaginationChange={(nextPage) => setPage(nextPage)}
        loading={isLoading}
        onRowClick={handleRowClick}
        emptyMessage={t('noEvaluations')}
      />

      {totalItems > 0 && (
        <DataTablePagination
          pageIndex={page}
          pageSize={PAGE_SIZE}
          totalItems={totalItems}
          totalPages={totalPages}
          onPageChange={setPage}
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
