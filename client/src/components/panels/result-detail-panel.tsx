'use client';

import * as React from 'react';
import { useTranslations } from 'next-intl';
import {
  XIcon,
  CaretLeftIcon,
  CaretRightIcon,
  CaretDownIcon,
} from '@phosphor-icons/react';
import { motion, AnimatePresence } from 'motion/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { StatusBadge } from '@/components/ui/status-badge';
import { ReviewDecisionForm } from '@/components/panels/review-decision-form';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export type EvaluationResultRow = {
  publicId: string;
  question: string;
  precondition: string | null;
  groundTruth: string | null;
  actualAnswer: string | null;
  judgeStatus: string | null;
  judgeScore: number | null;
  criteriaResultsJson: string | null;
  criteriaResults?: CriterionResult[] | null;
  qcStatus: string;
  qcNote: string | null;
};

export type CriterionResult = {
  metricKey?: string | null;
  name: string;
  status: string;
  score: number | null;
  reason: string | null;
  graderError?: boolean;
};

interface ResultDetailPanelProps {
  result: EvaluationResultRow | null;
  onClose: () => void;
  onPrev: (() => void) | null;
  onNext: (() => void) | null;
  className?: string;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function parseCriteriaJson(raw: string | null): CriterionResult[] {
  if (!raw) return [];
  try {
    const parsed: unknown = JSON.parse(raw);
    if (Array.isArray(parsed)) {
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
    }
    return [];
  } catch {
    return [];
  }
}

function getCriteria(result: EvaluationResultRow): CriterionResult[] {
  if (Array.isArray(result.criteriaResults) && result.criteriaResults.length > 0) {
    return result.criteriaResults;
  }
  return parseCriteriaJson(result.criteriaResultsJson);
}

function statusRank(status: string): number {
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

function isLongText(value: string | null | undefined): boolean {
  return (value?.length ?? 0) > 280;
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function ResultDetailPanel({
  result,
  onClose,
  onPrev,
  onNext,
  className,
}: ResultDetailPanelProps) {
  const t = useTranslations('resultDetail');
  const tQc = useTranslations('qcReview');
  const tCommon = useTranslations('common');
  const panelRef = React.useRef<HTMLDivElement>(null);
  const [expandedSections, setExpandedSections] = React.useState<Set<string>>(
    () => new Set(['question']),
  );
  const [expandedCriteria, setExpandedCriteria] = React.useState<Set<string>>(
    () => new Set(),
  );

  // Escape key
  React.useEffect(() => {
    if (!result) return;
    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        e.stopPropagation();
        onClose();
      }
    }
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [result, onClose]);

  // Lock body scroll when open
  React.useEffect(() => {
    if (!result) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prev;
    };
  }, [result]);

  // Focus panel on open
  React.useEffect(() => {
    if (result) {
      panelRef.current?.focus();
    }
  }, [result]);

  if (!result) return null;

  const criteria = getCriteria(result).toSorted(
    (a, b) => statusRank(a.status) - statusRank(b.status),
  );

  const toggleSection = (key: string) => {
    setExpandedSections((current) => {
      const next = new Set(current);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  };

  const toggleCriterion = (key: string) => {
    setExpandedCriteria((current) => {
      const next = new Set(current);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  };

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-40 bg-black/30 backdrop-blur-sm lg:bg-transparent lg:backdrop-blur-none"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Panel */}
      <aside
        ref={panelRef}
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        className={cn(
          'fixed inset-y-0 right-0 z-50 flex w-full flex-col border-l bg-card shadow-xl outline-none',
          'lg:w-[520px]',
          'animate-in slide-in-from-right',
          className,
        )}
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b px-4 py-3">
          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="icon"
              className="size-8"
              disabled={!onPrev}
              onClick={() => onPrev?.()}
              aria-label={t('prevResult')}
            >
              <CaretLeftIcon className="size-4" />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              className="size-8"
              disabled={!onNext}
              onClick={() => onNext?.()}
              aria-label={t('nextResult')}
            >
              <CaretRightIcon className="size-4" />
            </Button>
          </div>
          <Button
            variant="ghost"
            size="icon"
            className="size-8"
            onClick={onClose}
            aria-label={tCommon('close')}
          >
            <XIcon className="size-4" />
          </Button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-muted/30">
          <div className="flex flex-wrap items-center gap-2 mb-2">
            {result.judgeStatus ? (
              <StatusBadge status={result.judgeStatus} size="sm" />
            ) : (
              <span className="text-sm text-muted-foreground">
                {tCommon('notAvailable')}
              </span>
            )}
            <StatusBadge status={result.qcStatus} size="sm" />
            {result.judgeScore !== null && (
              <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium">
                {t('judgeScore')}: {result.judgeScore}
              </span>
            )}
          </div>

          {/* Question */}
          <Section label={t('question')}>
            <p className="text-sm whitespace-pre-wrap">{result.question}</p>
          </Section>

          {/* Precondition */}
          <CollapsibleSection
            label={t('precondition')}
            expanded={expandedSections.has('precondition') || !isLongText(result.precondition)}
            onToggle={() => toggleSection('precondition')}
            toggleable={isLongText(result.precondition)}
            previewText={result.precondition ?? tCommon('notAvailable')}
            tShow={t('showDetails')}
            tHide={t('hideDetails')}
          >
            <p className="text-sm text-muted-foreground whitespace-pre-wrap">
              {result.precondition ?? tCommon('notAvailable')}
            </p>
          </CollapsibleSection>

          {/* Ground Truth */}
          <CollapsibleSection
            label={t('groundTruth')}
            expanded={expandedSections.has('groundTruth') || !isLongText(result.groundTruth)}
            onToggle={() => toggleSection('groundTruth')}
            toggleable={isLongText(result.groundTruth)}
            previewText={result.groundTruth ?? tCommon('notAvailable')}
            tShow={t('showDetails')}
            tHide={t('hideDetails')}
          >
            <p className="text-sm whitespace-pre-wrap">
              {result.groundTruth ?? tCommon('notAvailable')}
            </p>
          </CollapsibleSection>

          {/* Actual Answer */}
          <CollapsibleSection
            label={t('actualAnswer')}
            expanded={expandedSections.has('actualAnswer') || !isLongText(result.actualAnswer)}
            onToggle={() => toggleSection('actualAnswer')}
            toggleable={isLongText(result.actualAnswer)}
            previewText={result.actualAnswer ?? tCommon('notAvailable')}
            tShow={t('showDetails')}
            tHide={t('hideDetails')}
          >
            <p className="text-sm whitespace-pre-wrap">
              {result.actualAnswer ?? tCommon('notAvailable')}
            </p>
          </CollapsibleSection>

          {/* Criteria Breakdown */}
          {criteria.length > 0 && (
            <Section label={t('criteriaBreakdown')}>
              <div className="space-y-2">
                {criteria.map((c, idx) => (
                  <CriterionCard
                    key={c.metricKey ?? `${c.name}-${idx}`}
                    criterion={c}
                    expanded={expandedCriteria.has(c.metricKey ?? `${c.name}-${idx}`)}
                    onToggle={() => toggleCriterion(c.metricKey ?? `${c.name}-${idx}`)}
                    tScore={t('judgeScore')}
                    tReason={t('reason')}
                    tGraderError={t('graderError')}
                    tNotAvailable={tCommon('notAvailable')}
                    tShow={t('showDetails')}
                    tHide={t('hideDetails')}
                  />
                ))}
              </div>
            </Section>
          )}

          {criteria.length === 0 && (
            <Section label={t('criteriaBreakdown')}>
              <p className="text-sm text-muted-foreground">{t('noCriteria')}</p>
            </Section>
          )}

          {/* QC Review (Epic 10) */}
          <Section label={tQc('title')}>
            <ReviewDecisionForm resultPublicId={result.publicId} />
          </Section>
        </div>
      </aside>
    </>
  );
}

function CriterionCard({
  criterion,
  expanded,
  onToggle,
  tScore,
  tReason,
  tGraderError,
  tNotAvailable,
  tShow,
  tHide,
}: {
  criterion: CriterionResult;
  expanded: boolean;
  onToggle: () => void;
  tScore: string;
  tReason: string;
  tGraderError: string;
  tNotAvailable: string;
  tShow: string;
  tHide: string;
}) {
  const shouldHighlight =
    criterion.status === 'FAIL' ||
    criterion.status === 'ERROR' ||
    criterion.graderError;
  const hasDetails = Boolean(criterion.reason) || Boolean(criterion.graderError);

  return (
    <div
      className={cn(
        'rounded-md border bg-muted/20 p-3 text-sm',
        shouldHighlight && 'border-destructive/40 bg-destructive/5',
      )}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 space-y-1">
          <div className="font-medium leading-5">
            {criterion.name || tNotAvailable}
          </div>
          {criterion.score !== null && (
            <div className="text-xs text-muted-foreground">
              {tScore}: {criterion.score}
            </div>
          )}
        </div>
        <StatusBadge status={criterion.status || 'NOT_REVIEWED'} size="sm" />
      </div>

      {hasDetails && (
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="mt-2 h-7 px-2 text-xs"
          onClick={onToggle}
        >
          <CaretDownIcon
            className={cn('size-3.5 transition-transform', !expanded && '-rotate-90')}
          />
          {expanded ? tHide : tShow}
        </Button>
      )}

      {hasDetails && (
        <AnimatePresence initial={false}>
          {expanded && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.2 }}
              className="overflow-hidden"
            >
              <div className="mt-2 space-y-2 border-t pt-2">
                {criterion.graderError && (
                  <p className="text-xs font-medium text-destructive">{tGraderError}</p>
                )}
                {criterion.reason && (
                  <div className="space-y-1">
                    <div className="text-xs font-medium text-muted-foreground">
                      {tReason}
                    </div>
                    <p className="whitespace-pre-wrap text-sm text-muted-foreground">
                      {criterion.reason}
                    </p>
                  </div>
                )}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      )}
    </div>
  );
}

function CollapsibleSection({
  label,
  expanded,
  toggleable,
  onToggle,
  previewText,
  tShow,
  tHide,
  children,
}: {
  label: string;
  expanded: boolean;
  toggleable: boolean;
  onToggle: () => void;
  previewText: string;
  tShow: string;
  tHide: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-2 rounded-xl border bg-card p-4 shadow-sm transition-all">
      <div className="flex items-center justify-between gap-3">
        <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
          {label}
        </h3>
        {toggleable && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="h-7 px-2 text-xs"
            onClick={onToggle}
          >
            <CaretDownIcon
              className={cn('size-3.5 transition-transform duration-200', !expanded && '-rotate-90')}
            />
            {expanded ? tHide : tShow}
          </Button>
        )}
      </div>
      <AnimatePresence initial={false} mode="popLayout">
        {expanded ? (
          <motion.div
            key="full"
            initial={{ opacity: 0, y: -5 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -5 }}
            transition={{ duration: 0.2 }}
          >
            {children}
          </motion.div>
        ) : (
          <motion.div
            key="preview"
            initial={{ opacity: 0, y: -5 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -5 }}
            transition={{ duration: 0.2 }}
          >
            <p className="max-h-16 overflow-hidden whitespace-pre-wrap text-sm text-muted-foreground">
              {previewText}
            </p>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Section helper
// ---------------------------------------------------------------------------

function Section({
  label,
  children,
  className,
}: {
  label: string;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("space-y-3 rounded-xl border bg-card p-4 shadow-sm", className)}>
      <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
        {label}
      </h3>
      {children}
    </div>
  );
}
