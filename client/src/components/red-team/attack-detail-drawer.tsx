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
            <div className="flex items-center justify-between border-b border-border/50 bg-muted/20 px-5 py-4">
              <div className="flex items-center gap-3">
                <Badge variant="outline" className="uppercase bg-muted/40 border-border/50 font-mono text-[10px] font-medium tracking-tight text-muted-foreground px-1.5 py-0">
                  {result.pluginId.split(':').pop() || result.pluginId}
                </Badge>
                <span className="text-sm text-foreground font-semibold tracking-tight">
                  {t('results.attackDetail')}
                </span>
              </div>
              <Button
                variant="ghost"
                size="icon"
                className="size-8 text-muted-foreground hover:text-foreground hover:bg-muted/50 cursor-pointer"
                onClick={onClose}
              >
                <XIcon size={16} weight="bold" />
              </Button>
            </div>

            {/* Scrollable Content */}
            <div className="flex-1 overflow-y-auto p-5 space-y-8">
              {/* Status Banner */}
              <div className={cn(
                'rounded-xl border p-5 flex items-center justify-between shadow-sm',
                result.success
                  ? 'border-emerald-500/20 bg-emerald-500/5 text-emerald-500'
                  : 'border-destructive/20 bg-destructive/5 text-destructive'
              )}>
                <div className="flex items-center gap-3.5">
                  {result.success ? (
                    <ShieldCheckIcon size={24} weight="fill" className="text-emerald-500" />
                  ) : (
                    <ShieldWarningIcon size={24} weight="fill" className="text-destructive" />
                  )}
                  <div className="space-y-0.5">
                    <span className="font-semibold text-[13px] tracking-tight block text-foreground">
                      {result.success ? t('results.shielded') : t('results.vulnerabilityFound')}
                    </span>
                    <span className="text-[11px] font-medium text-muted-foreground">
                      {result.success
                        ? t('results.shieldedExplanation')
                        : t('results.vulnerableExplanation')}
                    </span>
                  </div>
                </div>
                <div className="text-right border-l border-border/50 pl-4">
                  <span className="text-[10px] font-mono uppercase tracking-widest text-muted-foreground block mb-0.5">{t('results.score')}</span>
                  <span className="font-mono font-bold text-base text-foreground tracking-tighter">
                    {result.score.toFixed(2)}
                  </span>
                </div>
              </div>

              {/* Adversarial Prompt */}
              <div className="space-y-2.5">
                <span className="text-[11px] font-semibold text-muted-foreground uppercase tracking-widest font-mono block">
                  {t('results.promptInjected')}
                </span>
                <div className="relative group">
                  <pre className="rounded-xl border border-border/50 bg-muted/30 p-5 text-[12px] font-mono text-foreground overflow-x-auto whitespace-pre-wrap leading-relaxed shadow-sm">
                    {typeof result.prompt === 'object' ? result.prompt.raw : result.prompt}
                  </pre>
                  <button
                    onClick={() => handleCopy(typeof result.prompt === 'object' ? result.prompt.raw : result.prompt)}
                    className="absolute right-3 top-3 p-1.5 rounded-md bg-background border border-border/50 hover:bg-muted text-muted-foreground hover:text-foreground opacity-0 group-hover:opacity-100 transition-all cursor-pointer shadow-xs"
                    title={t('results.copyPrompt')}
                  >
                    {copied ? <CheckIcon size={14} className="text-emerald-500" /> : <CopyIcon size={14} />}
                  </button>
                </div>
              </div>

              {/* Model Response */}
              <div className="space-y-2.5">
                <span className="text-[11px] font-semibold text-muted-foreground uppercase tracking-widest font-mono block">
                  {t('results.modelResponse')}
                </span>
                <pre className={cn(
                  'rounded-xl border p-5 text-[12px] font-mono overflow-x-auto whitespace-pre-wrap leading-relaxed shadow-sm',
                  result.success
                    ? 'border-border/50 bg-muted/10 text-muted-foreground'
                    : 'border-destructive/20 bg-destructive/5 text-destructive'
                )}>
                  {typeof result.response === 'object' ? result.response.raw : result.response}
                </pre>
              </div>

              {/* Grading Reason */}
              {result.gradingResult?.reason && (
                <div className="space-y-2.5">
                  <span className="text-[11px] font-semibold text-muted-foreground uppercase tracking-widest font-mono block">
                    {t('results.gradingReason')}
                  </span>
                  <div className="rounded-xl border border-border/50 bg-muted/20 p-5 text-[12px] text-muted-foreground leading-relaxed shadow-sm">
                    {result.gradingResult.reason}
                  </div>
                </div>
              )}

              {/* Remediation Block */}
              {!result.success && (
                <div className="space-y-3 pt-6 border-t border-border/50">
                  <div className="flex items-center gap-2">
                    <ShieldCheckIcon size={16} weight="bold" className="text-emerald-500" />
                    <span className="text-[11px] font-semibold text-foreground uppercase tracking-widest font-mono">
                      {t('results.remediationTitle')}
                    </span>
                  </div>
                  <p className="text-[13px] text-muted-foreground font-medium">
                    {t('results.remediationDesc')}
                  </p>
                  <div className="relative group mt-3">
                    <pre className="rounded-xl border border-border/50 bg-background p-5 text-[12px] font-mono text-foreground overflow-x-auto whitespace-pre-wrap leading-relaxed shadow-sm">
                      {getRemediation(result.pluginId)}
                    </pre>
                    <button
                      onClick={() => handleCopy(getRemediation(result.pluginId))}
                      className="absolute right-3 top-3 p-1.5 rounded-md bg-muted/50 border border-border/50 hover:bg-muted text-muted-foreground hover:text-foreground opacity-0 group-hover:opacity-100 transition-all cursor-pointer shadow-xs"
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
