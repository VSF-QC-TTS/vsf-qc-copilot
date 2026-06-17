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
import { getCriteria, criterionStatusRank } from '@/lib/utils/criteria';
import type { EvaluationResultRow, CriterionResult } from '@/lib/api/types';

// Re-export types for backward compatibility
export type { EvaluationResultRow, CriterionResult } from '@/lib/api/types';

// ---------------------------------------------------------------------------
// Props
// ---------------------------------------------------------------------------

interface ResultDetailPanelProps {
  result: EvaluationResultRow | null;
  onClose: () => void;
  onPrev: (() => void) | null;
  onNext: (() => void) | null;
  className?: string;
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

  // Escape + arrow key navigation
  React.useEffect(() => {
    if (!result) return;
    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        e.stopPropagation();
        onClose();
      }
      if (e.key === 'ArrowLeft' && onPrev) {
        e.preventDefault();
        onPrev();
      }
      if (e.key === 'ArrowRight' && onNext) {
        e.preventDefault();
        onNext();
      }
    }
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [result, onClose, onPrev, onNext]);

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
    (a, b) => criterionStatusRank(a.status) - criterionStatusRank(b.status),
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

  const hasGroundTruth = result.groundTruth !== null;
  const hasActualAnswer = result.actualAnswer !== null;
  const showSideBySide = hasGroundTruth && hasActualAnswer;

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-40 bg-black/30 backdrop-blur-sm lg:bg-transparent lg:backdrop-blur-none"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Panel — wider for QC review workflow */}
      <aside
        ref={panelRef}
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        className={cn(
          'fixed inset-y-0 right-0 z-50 flex w-full flex-col border-l bg-card shadow-xl outline-none',
          'lg:w-[680px]',
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
            <span className="ml-1 text-xs text-muted-foreground hidden sm:inline">
              ← → {t('keyboardNav')}
            </span>
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
          {/* Status row */}
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
            content={result.precondition}
            fallback={tCommon('notAvailable')}
            tShow={t('showDetails')}
            tHide={t('hideDetails')}
          />

          {/* Ground Truth vs Actual Answer — side by side when both exist */}
          {showSideBySide ? (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
              <CollapsibleSection
                label={t('groundTruth')}
                content={result.groundTruth}
                fallback={tCommon('notAvailable')}
                tShow={t('showDetails')}
                tHide={t('hideDetails')}
              />
              <CollapsibleSection
                label={t('actualAnswer')}
                content={result.actualAnswer}
                fallback={tCommon('notAvailable')}
                tShow={t('showDetails')}
                tHide={t('hideDetails')}
              />
            </div>
          ) : (
            <>
              <CollapsibleSection
                label={t('groundTruth')}
                content={result.groundTruth}
                fallback={tCommon('notAvailable')}
                tShow={t('showDetails')}
                tHide={t('hideDetails')}
              />
              <CollapsibleSection
                label={t('actualAnswer')}
                content={result.actualAnswer}
                fallback={tCommon('notAvailable')}
                tShow={t('showDetails')}
                tHide={t('hideDetails')}
              />
            </>
          )}

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

          {/* QC Review */}
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

// ---------------------------------------------------------------------------
// CollapsibleSection — improved with fade-out gradient instead of hard clip
// ---------------------------------------------------------------------------

const COLLAPSE_THRESHOLD = 280;

function CollapsibleSection({
  label,
  content,
  fallback,
  tShow,
  tHide,
}: {
  label: string;
  content: string | null;
  fallback: string;
  tShow: string;
  tHide: string;
}) {
  const text = content ?? fallback;
  const isLong = text.length > COLLAPSE_THRESHOLD;
  const [expanded, setExpanded] = React.useState(!isLong);

  // Auto-expand short content when result changes
  React.useEffect(() => {
    setExpanded(text.length <= COLLAPSE_THRESHOLD);
  }, [text]);

  return (
    <div className="space-y-2 rounded-xl bg-card p-4 transition-all">
      <div className="flex items-center justify-between gap-3">
        <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
          {label}
        </h3>
        {isLong && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="h-7 px-2 text-xs"
            onClick={() => setExpanded((v) => !v)}
          >
            <CaretDownIcon
              className={cn('size-3.5 transition-transform duration-200', !expanded && '-rotate-90')}
            />
            {expanded ? tHide : tShow}
          </Button>
        )}
      </div>
      <div className="relative">
        <p
          className={cn(
            'whitespace-pre-wrap text-sm text-muted-foreground',
            !expanded && 'max-h-20 overflow-hidden',
          )}
        >
          {text}
        </p>
        {/* Fade-out gradient when collapsed */}
        {!expanded && isLong && (
          <div className="absolute inset-x-0 bottom-0 h-10 bg-gradient-to-t from-card to-transparent pointer-events-none" />
        )}
      </div>
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
    <div className={cn("space-y-3 rounded-xl bg-card p-4", className)}>
      <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
        {label}
      </h3>
      {children}
    </div>
  );
}
