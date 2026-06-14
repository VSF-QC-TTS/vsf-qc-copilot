'use client';

import * as React from 'react';
import { useTranslations } from 'next-intl';
import { XIcon, CaretLeftIcon, CaretRightIcon } from '@phosphor-icons/react';

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
  criteriaResultJson: string | null;
  qcStatus: string;
  qcNote: string | null;
};

type CriterionResult = {
  name: string;
  status: string;
  score: number | null;
  reason: string | null;
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
          name: typeof obj['name'] === 'string' ? obj['name'] : '',
          status: typeof obj['status'] === 'string' ? obj['status'] : '',
          score: typeof obj['score'] === 'number' ? obj['score'] : null,
          reason: typeof obj['reason'] === 'string' ? obj['reason'] : null,
        };
      });
    }
    return [];
  } catch {
    return [];
  }
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

  const criteria = parseCriteriaJson(result.criteriaResultJson);

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
          >
            <XIcon className="size-4" />
          </Button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-4 space-y-6">
          {/* Question */}
          <Section label={t('question')}>
            <p className="text-sm whitespace-pre-wrap">{result.question}</p>
          </Section>

          {/* Precondition */}
          <Section label={t('precondition')}>
            <p className="text-sm text-muted-foreground whitespace-pre-wrap">
              {result.precondition ?? tCommon('notAvailable')}
            </p>
          </Section>

          {/* Ground Truth */}
          <Section label={t('groundTruth')}>
            <p className="text-sm whitespace-pre-wrap">
              {result.groundTruth ?? tCommon('notAvailable')}
            </p>
          </Section>

          {/* Actual Answer */}
          <Section label={t('actualAnswer')}>
            <p className="text-sm whitespace-pre-wrap">
              {result.actualAnswer ?? tCommon('notAvailable')}
            </p>
          </Section>

          {/* Judge Status & Score */}
          <Section label={t('judgeStatus')}>
            <div className="flex items-center gap-3">
              {result.judgeStatus ? (
                <StatusBadge status={result.judgeStatus} size="sm" />
              ) : (
                <span className="text-sm text-muted-foreground">
                  {tCommon('notAvailable')}
                </span>
              )}
              {result.judgeScore !== null && (
                <span className="text-sm font-medium">
                  {t('judgeScore')}: {result.judgeScore}
                </span>
              )}
            </div>
          </Section>

          {/* Criteria Breakdown */}
          {criteria.length > 0 && (
            <Section label={t('criteriaBreakdown')}>
              <div className="space-y-2">
                {criteria.map((c, idx) => (
                  <div
                    key={idx}
                    className="rounded-md border bg-muted/30 p-3 text-sm space-y-1"
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-medium">
                        {c.name || tCommon('notAvailable')}
                      </span>
                      {c.status ? (
                        <StatusBadge status={c.status} size="sm" />
                      ) : (
                        <span className="text-muted-foreground">
                          {tCommon('notAvailable')}
                        </span>
                      )}
                    </div>
                    {c.score !== null && (
                      <p className="text-muted-foreground">
                        {t('judgeScore')}: {c.score}
                      </p>
                    )}
                    {c.reason && (
                      <p className="text-muted-foreground whitespace-pre-wrap">
                        {c.reason}
                      </p>
                    )}
                  </div>
                ))}
              </div>
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

// ---------------------------------------------------------------------------
// Section helper
// ---------------------------------------------------------------------------

function Section({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-1">
      <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
        {label}
      </h3>
      {children}
    </div>
  );
}
