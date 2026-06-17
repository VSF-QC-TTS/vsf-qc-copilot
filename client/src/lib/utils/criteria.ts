import type { CriterionResult, EvaluationResultRow } from '@/lib/api/types';

// ---------------------------------------------------------------------------
// Criteria JSON parsing
// ---------------------------------------------------------------------------

/**
 * Parse the raw `criteriaResultsJson` string returned by the backend into
 * a typed `CriterionResult[]`. Returns an empty array on `null`, invalid JSON,
 * or non-array payloads.
 */
export function parseCriteriaJson(raw: string | null): CriterionResult[] {
  if (!raw) return [];
  try {
    const parsed: unknown = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.map((item: unknown) => {
      const obj =
        typeof item === 'object' && item !== null
          ? (item as Record<string, unknown>)
          : {};
      return {
        metricKey: typeof obj['metricKey'] === 'string' ? obj['metricKey'] : null,
        name: typeof obj['name'] === 'string' ? obj['name'] : '',
        status: typeof obj['status'] === 'string' ? obj['status'] : '',
        score: typeof obj['score'] === 'number' ? obj['score'] : null,
        reason: typeof obj['reason'] === 'string' ? obj['reason'] : null,
        graderError: obj['graderError'] === true,
      };
    });
  } catch {
    return [];
  }
}

/**
 * Get criteria from a result row, preferring the already-parsed
 * `criteriaResults` array over the raw JSON string.
 */
export function getCriteria(row: Pick<EvaluationResultRow, 'criteriaResults' | 'criteriaResultsJson'>): CriterionResult[] {
  if (Array.isArray(row.criteriaResults) && row.criteriaResults.length > 0) {
    return row.criteriaResults;
  }
  return parseCriteriaJson(row.criteriaResultsJson);
}

/**
 * Sort rank for criterion status — used to display worst-first in the detail panel.
 */
export function criterionStatusRank(status: string): number {
  switch (status) {
    case 'ERROR':
      return 0;
    case 'FAIL':
      return 1;
    case 'WARNING':
      return 2;
    case 'PASS':
      return 3;
    default:
      return 4;
  }
}
