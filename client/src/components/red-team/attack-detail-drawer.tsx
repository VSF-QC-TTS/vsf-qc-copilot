'use client';

import { useState } from 'react';
import { useTranslations } from 'next-intl';
import {
  ShieldWarningIcon,
  ShieldCheckIcon,
  XIcon,
  CopyIcon,
  CheckIcon,
} from '@phosphor-icons/react';
import { motion, AnimatePresence } from 'motion/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export type AttackResultItem = {
  pluginId: string;
  prompt: { raw: string; label: string } | string;
  vars: Record<string, unknown>;
  response: { raw: string; data?: unknown } | string;
  success: boolean;
  score: number;
  latencyMs?: number;
  gradingResult?: {
    pass: boolean;
    score: number;
    reason: string;
    componentResults?: unknown[];
    assertion?: unknown;
  };
  provider?: {
    id: string;
  };
};

// ---------------------------------------------------------------------------
// Props
// ---------------------------------------------------------------------------

interface AttackDetailDrawerProps {
  result: AttackResultItem | null;
  onClose: () => void;
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function AttackDetailDrawer({ result, onClose }: AttackDetailDrawerProps) {
  const t = useTranslations('redTeam');
  const [copied, setCopied] = useState(false);

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const getRemediation = (pluginId: string): string => {
    switch (pluginId) {
      case 'prompt-extraction':
        return t('results.remediationPromptExtraction');
      case 'harmful:privacy':
      case 'pii:direct':
        return t('results.remediationPrivacy');
      default:
        return t('results.remediationDefault');
    }
  };

  return (
    <AnimatePresence>
      {result && (
        <>
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-40 bg-black/60 backdrop-blur-xs"
            onClick={onClose}
            aria-hidden="true"
          />

          {/* Panel drawer */}
          <motion.aside
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ type: 'tween', ease: 'easeInOut', duration: 0.25 }}
            className="fixed inset-y-0 right-0 z-50 flex w-full flex-col border-l bg-background shadow-2xl lg:w-[680px] text-foreground overflow-hidden"
          >
            {/* Header */}
            <div className="flex items-center justify-between border-b px-4 py-4">
              <div className="flex items-center gap-2">
                <Badge variant="outline" className="uppercase bg-muted border font-mono text-xs text-muted-foreground">
                  {result.pluginId.split(':').pop() || result.pluginId}
                </Badge>
                <span className="text-sm text-muted-foreground font-medium">
                  {t('results.attackDetail')}
                </span>
              </div>
              <Button
                variant="ghost"
                size="icon"
                className="size-8 text-muted-foreground hover:text-foreground hover:bg-muted cursor-pointer"
                onClick={onClose}
              >
                <XIcon size={18} />
              </Button>
            </div>

            {/* Scrollable Content */}
            <div className="flex-1 overflow-y-auto p-5 space-y-6">
              {/* Status Banner */}
              <div className={cn(
                'rounded-lg border p-4 flex items-center justify-between',
                result.success
                  ? 'border-emerald-500/20 bg-emerald-500/5 text-emerald-600 dark:text-emerald-400'
                  : 'border-destructive/20 bg-destructive/5 text-red-600 dark:text-red-400'
              )}>
                <div className="flex items-center gap-3">
                  {result.success ? (
                    <ShieldCheckIcon size={24} weight="fill" className="text-emerald-600 dark:text-emerald-500" />
                  ) : (
                    <ShieldWarningIcon size={24} weight="fill" className="text-red-600 dark:text-red-500" />
                  )}
                  <div className="space-y-0.5">
                    <span className="font-bold text-sm block">
                      {result.success ? t('results.shielded') : t('results.vulnerabilityFound')}
                    </span>
                    <span className="text-[10px] text-muted-foreground">
                      {result.success
                        ? t('results.shieldedExplanation')
                        : t('results.vulnerableExplanation')}
                    </span>
                  </div>
                </div>
                <div className="text-right">
                  <span className="text-xs text-muted-foreground block">{t('results.score')}</span>
                  <span className="font-mono font-bold text-sm">
                    {result.score.toFixed(2)}
                  </span>
                </div>
              </div>

              {/* Adversarial Prompt */}
              <div className="space-y-2">
                <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider block">
                  {t('results.promptInjected')}
                </span>
                <div className="relative group">
                  <pre className="rounded-lg border bg-muted/40 p-4 text-xs font-mono text-foreground overflow-x-auto whitespace-pre-wrap leading-relaxed">
                    {typeof result.prompt === 'object' ? result.prompt.raw : result.prompt}
                  </pre>
                  <button
                    onClick={() => handleCopy(typeof result.prompt === 'object' ? result.prompt.raw : result.prompt)}
                    className="absolute right-3 top-3 p-1.5 rounded bg-background border hover:border-muted-foreground/50 text-muted-foreground hover:text-foreground opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer"
                    title={t('results.copyPrompt')}
                  >
                    {copied ? <CheckIcon size={14} className="text-emerald-500" /> : <CopyIcon size={14} />}
                  </button>
                </div>
              </div>

              {/* Model Response */}
              <div className="space-y-2">
                <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider block">
                  {t('results.modelResponse')}
                </span>
                <pre className={cn(
                  'rounded-lg border p-4 text-xs font-mono overflow-x-auto whitespace-pre-wrap leading-relaxed',
                  result.success
                    ? 'bg-muted/20 text-muted-foreground'
                    : 'border-destructive/30 bg-destructive/5 text-red-600 dark:text-red-300'
                )}>
                  {typeof result.response === 'object' ? result.response.raw : result.response}
                </pre>
              </div>

              {/* Grading Reason */}
              {result.gradingResult?.reason && (
                <div className="space-y-2">
                  <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider block">
                    {t('results.gradingReason')}
                  </span>
                  <div className="rounded-lg border bg-muted/30 p-4 text-xs text-muted-foreground leading-relaxed">
                    {result.gradingResult.reason}
                  </div>
                </div>
              )}

              {/* Remediation Block */}
              {!result.success && (
                <div className="space-y-2 pt-2 border-t">
                  <span className="text-xs font-semibold text-red-600 dark:text-red-400 uppercase tracking-wider flex items-center gap-1">
                    <ShieldCheckIcon size={16} />
                    {t('results.remediationTitle')}
                  </span>
                  <p className="text-xs text-muted-foreground">
                    {t('results.remediationDesc')}
                  </p>
                  <div className="relative group mt-2">
                    <pre className="rounded-lg border border-destructive/20 bg-destructive/5 p-4 text-xs font-mono text-foreground overflow-x-auto whitespace-pre-wrap leading-relaxed">
                      {getRemediation(result.pluginId)}
                    </pre>
                    <button
                      onClick={() => handleCopy(getRemediation(result.pluginId))}
                      className="absolute right-3 top-3 p-1.5 rounded bg-background border hover:border-muted-foreground/50 text-muted-foreground hover:text-foreground opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer"
                      title={t('results.copyRemediation')}
                    >
                      {copied ? <CheckIcon size={14} className="text-emerald-500" /> : <CopyIcon size={14} />}
                    </button>
                  </div>
                </div>
              )}
            </div>
          </motion.aside>
        </>
      )}
    </AnimatePresence>
  );
}
