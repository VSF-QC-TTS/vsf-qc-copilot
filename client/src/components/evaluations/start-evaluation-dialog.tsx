'use client';

import * as React from 'react';
import { useTranslations } from 'next-intl';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  CheckIcon,
  DatabaseIcon,
  BookOpenIcon,
  HardDrivesIcon,
  BrainIcon,
  CaretRightIcon,
  CaretLeftIcon,
  LightningIcon,
} from '@phosphor-icons/react';
import { motion, AnimatePresence } from 'motion/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { apiClient } from '@/lib/api/client';
import {
  startEvaluationSchema,
  type StartEvaluationFormValues,
} from '@/lib/validations/evaluation';
import type { PageResponse } from '@/lib/api/types';
import { Link, useRouter } from '@/i18n/navigation';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type DatasetOption = {
  publicId: string;
  name: string;
  status: string;
};

type RubricVersionOption = {
  publicId: string;
  versionNumber: number;
  rubricName: string;
  status: string;
};

type ConnectorOption = {
  publicId: string;
  name: string;
  active: boolean;
};

type JudgeModelOption = {
  publicId: string;
  name: string;
  provider: string;
  modelName: string;
  active: boolean;
};

type StartEvaluationResponse = {
  runPublicId: string;
  jobPublicId: string;
  status: string;
  message: string;
};

interface StartEvaluationDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  projectId: string;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function EmptyStateBlock({
  message,
  actionHref,
  actionLabel,
}: {
  message: string;
  actionHref: string;
  actionLabel: string;
}) {
  return (
    <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-8 text-center bg-muted/20">
      <p className="mb-4 text-sm text-muted-foreground">{message}</p>
      <Button asChild size="sm" variant="outline">
        <Link href={actionHref}>{actionLabel}</Link>
      </Button>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function StartEvaluationDialog({
  open,
  onOpenChange,
  projectId,
}: StartEvaluationDialogProps) {
  const t = useTranslations('evaluations');
  const tw = useTranslations('evaluations.wizard');
  const tCommon = useTranslations('common');
  const router = useRouter();
  const queryClient = useQueryClient();

  const [step, setStep] = React.useState(1);
  const [submitError, setSubmitError] = React.useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = React.useState(false);

  const {
    handleSubmit,
    setValue,
    control,
    reset,
  } = useForm<StartEvaluationFormValues>({
    resolver: zodResolver(startEvaluationSchema),
    defaultValues: {
      datasetPublicId: '',
      rubricVersionPublicId: '',
      targetConnectorPublicId: '',
      judgeModelPublicId: '',
    },
  });

  const selectedValues = useWatch({ control });

  // -------------------------------------------------------------------------
  // Fetch Data
  // -------------------------------------------------------------------------

  const { data: datasetsData, isLoading: datasetsLoading } = useQuery({
    queryKey: ['datasets-approved', projectId],
    queryFn: () =>
      apiClient.get<PageResponse<DatasetOption>>(
        `/api/v1/projects/${projectId}/datasets?status=APPROVED&size=100`,
      ),
    enabled: open,
  });

  const { data: rubricVersionsData, isLoading: rubricVersionsLoading } =
    useQuery({
      queryKey: ['rubric-versions-published'],
      queryFn: () =>
        apiClient.get<PageResponse<RubricVersionOption>>(
          '/api/v1/rubric-versions?status=PUBLISHED&size=100',
        ),
      enabled: open,
    });

  const { data: connectorsData, isLoading: connectorsLoading } = useQuery({
    queryKey: ['connectors-active', projectId],
    queryFn: () =>
      apiClient.get<PageResponse<ConnectorOption>>(
        `/api/v1/projects/${projectId}/target-api-connectors?active=true&size=100`,
      ),
    enabled: open,
  });

  const { data: judgeModelsData, isLoading: judgeModelsLoading } = useQuery({
    queryKey: ['judge-models-active', projectId],
    queryFn: () =>
      apiClient.get<PageResponse<JudgeModelOption>>(
        `/api/v1/projects/${projectId}/judge-models?active=true&size=100`,
      ),
    enabled: open,
  });

  const datasets = React.useMemo(() => datasetsData?.items ?? [], [datasetsData]);
  const rubricVersions = React.useMemo(
    () => rubricVersionsData?.items ?? [],
    [rubricVersionsData],
  );
  const connectors = React.useMemo(() => connectorsData?.items ?? [], [connectorsData]);
  const judgeModels = React.useMemo(
    () => judgeModelsData?.items ?? [],
    [judgeModelsData],
  );

  // -------------------------------------------------------------------------
  // Handlers
  // -------------------------------------------------------------------------

  // Reset dialog state when closed
  React.useEffect(() => {
    if (open) return;
    let cancelled = false;
    queueMicrotask(() => {
      if (cancelled) return;
      reset();
      setStep(1);
      setSubmitError(null);
      setIsSubmitting(false);
    });
    return () => {
      cancelled = true;
    };
  }, [open, reset]);

  // Lock body scroll and Escape key handling
  React.useEffect(() => {
    if (!open) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    
    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        e.stopPropagation();
        onOpenChange(false);
      }
    }
    document.addEventListener('keydown', handleKey);
    return () => {
      document.body.style.overflow = prev;
      document.removeEventListener('keydown', handleKey);
    };
  }, [open, onOpenChange]);

  // Smart Defaults
  React.useEffect(() => {
    if (datasets.length === 1 && !selectedValues.datasetPublicId) {
      setValue('datasetPublicId', datasets[0].publicId);
    }
    if (rubricVersions.length === 1 && !selectedValues.rubricVersionPublicId) {
      setValue('rubricVersionPublicId', rubricVersions[0].publicId);
    }
    if (connectors.length === 1 && !selectedValues.targetConnectorPublicId) {
      setValue('targetConnectorPublicId', connectors[0].publicId);
    }
    if (judgeModels.length === 1 && !selectedValues.judgeModelPublicId) {
      setValue('judgeModelPublicId', judgeModels[0].publicId);
    }
  }, [connectors, datasets, judgeModels, rubricVersions, selectedValues.datasetPublicId, selectedValues.judgeModelPublicId, selectedValues.rubricVersionPublicId, selectedValues.targetConnectorPublicId, setValue]);

  const onSubmit = async (values: StartEvaluationFormValues) => {
    setSubmitError(null);
    setIsSubmitting(true);
    try {
      const res = await apiClient.post<StartEvaluationResponse>(
        `/api/v1/projects/${projectId}/evaluation-runs`,
        values,
      );
      
      void queryClient.invalidateQueries({ queryKey: ['evaluation-runs', projectId] });
      void queryClient.invalidateQueries({ queryKey: ['evaluations', projectId] });
      
      onOpenChange(false);
      router.push(`/projects/${projectId}/evaluations/${res.runPublicId}`);
    } catch (err: unknown) {
      const msg =
        err instanceof Object && 'message' in err
          ? (err as { message: string }).message
          : 'Failed to start evaluation';
      setSubmitError(msg);
      setIsSubmitting(false);
    }
  };

  const handleNext = () => setStep((s) => Math.min(s + 1, 4));
  const handlePrev = () => setStep((s) => Math.max(s - 1, 1));

  const isStep1Valid = Boolean(selectedValues.datasetPublicId);
  const isStep2Valid = Boolean(selectedValues.rubricVersionPublicId);
  const isStep3Valid = Boolean(selectedValues.targetConnectorPublicId) && Boolean(selectedValues.judgeModelPublicId);

  const canProceed = () => {
    if (step === 1) return isStep1Valid;
    if (step === 2) return isStep2Valid;
    if (step === 3) return isStep3Valid;
    return true;
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={() => !isSubmitting && onOpenChange(false)} />
      
      <div
        role="dialog"
        className={cn(
          'relative z-10 w-full max-w-2xl rounded-xl border bg-card shadow-2xl flex flex-col max-h-[85vh] overflow-hidden',
          'animate-in fade-in-0 zoom-in-95',
        )}
      >
        {/* Header - Stepper */}
        <div className="px-6 pt-6 pb-4 border-b bg-muted/10 shrink-0">
          <h2 className="text-xl font-semibold mb-6">{t('startEvaluation')}</h2>
          <div className="flex items-center justify-between relative px-2">
            <div className="absolute top-1/2 left-4 right-4 h-0.5 bg-border -z-10 -translate-y-1/2" />
            
            {[
              { num: 1, label: tw('step1'), valid: isStep1Valid },
              { num: 2, label: tw('step2'), valid: isStep2Valid },
              { num: 3, label: tw('step3'), valid: isStep3Valid },
              { num: 4, label: tw('step4'), valid: false }, // Final step
            ].map((st) => (
              <div key={st.num} className="flex flex-col items-center gap-2 bg-card px-2">
                <div 
                  className={cn(
                    "w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium transition-colors",
                    step === st.num ? "bg-primary text-primary-foreground ring-4 ring-primary/20" :
                    step > st.num || st.valid ? "bg-primary text-primary-foreground" : "bg-secondary text-muted-foreground border"
                  )}
                >
                  {step > st.num || st.valid ? <CheckIcon weight="bold" /> : st.num}
                </div>
                <span className={cn("text-xs font-medium whitespace-nowrap", step === st.num ? "text-foreground" : "text-muted-foreground")}>
                  {st.label}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Content area */}
        <div className="flex-1 overflow-y-auto p-6 relative min-h-[300px]">
          {submitError && (
            <div className="mb-4 rounded-md bg-destructive/10 p-3 text-sm text-destructive border border-destructive/20">
              {submitError}
            </div>
          )}

          <AnimatePresence mode="wait" custom={step}>
            {step === 1 && (
              <motion.div
                key="step1"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                transition={{ duration: 0.2 }}
                className="space-y-4"
              >
                <div className="flex items-center gap-2 mb-4 text-foreground/80">
                  <DatabaseIcon size={20} className="text-primary" />
                  <h3 className="font-medium">{tw('step1')}</h3>
                </div>
                
                {datasetsLoading ? (
                  <div className="animate-pulse flex gap-4">
                    <div className="h-24 w-1/2 bg-muted rounded-lg" />
                    <div className="h-24 w-1/2 bg-muted rounded-lg" />
                  </div>
                ) : datasets.length === 0 ? (
                  <EmptyStateBlock 
                    message={tw('emptyDataset')}
                    actionHref={`/projects/${projectId}/datasets`}
                    actionLabel={t('createMissingDataset')}
                  />
                ) : (
                  <div className="grid grid-cols-2 gap-4">
                    {datasets.map(d => (
                      <div 
                        key={d.publicId} 
                        onClick={() => setValue('datasetPublicId', d.publicId)}
                        className={cn(
                          "border p-4 rounded-xl cursor-pointer transition-all hover:shadow-sm relative",
                          selectedValues.datasetPublicId === d.publicId 
                            ? "border-primary bg-primary/5 ring-1 ring-primary" 
                            : "hover:border-foreground/30 bg-card"
                        )}
                      >
                        {selectedValues.datasetPublicId === d.publicId && (
                          <div className="absolute top-3 right-3 text-primary">
                            <CheckIcon weight="bold" size={18} />
                          </div>
                        )}
                        <h4 className="font-semibold text-sm mb-1 pr-6">{d.name}</h4>
                        <span className="text-xs text-muted-foreground bg-secondary px-2 py-0.5 rounded-md">
                          {d.status}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </motion.div>
            )}

            {step === 2 && (
              <motion.div
                key="step2"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                transition={{ duration: 0.2 }}
                className="space-y-4"
              >
                <div className="flex items-center gap-2 mb-4 text-foreground/80">
                  <BookOpenIcon size={20} className="text-primary" />
                  <h3 className="font-medium">{tw('step2')}</h3>
                </div>

                {rubricVersionsLoading ? (
                  <div className="animate-pulse flex gap-4">
                    <div className="h-24 w-1/2 bg-muted rounded-lg" />
                    <div className="h-24 w-1/2 bg-muted rounded-lg" />
                  </div>
                ) : rubricVersions.length === 0 ? (
                  <EmptyStateBlock 
                    message={tw('emptyRubric')}
                    actionHref="/rubrics"
                    actionLabel={t('createMissingRubric')}
                  />
                ) : (
                  <div className="grid grid-cols-2 gap-4">
                    {rubricVersions.map(rv => (
                      <div 
                        key={rv.publicId} 
                        onClick={() => setValue('rubricVersionPublicId', rv.publicId)}
                        className={cn(
                          "border p-4 rounded-xl cursor-pointer transition-all hover:shadow-sm relative",
                          selectedValues.rubricVersionPublicId === rv.publicId 
                            ? "border-primary bg-primary/5 ring-1 ring-primary" 
                            : "hover:border-foreground/30 bg-card"
                        )}
                      >
                        {selectedValues.rubricVersionPublicId === rv.publicId && (
                          <div className="absolute top-3 right-3 text-primary">
                            <CheckIcon weight="bold" size={18} />
                          </div>
                        )}
                        <h4 className="font-semibold text-sm mb-1 pr-6">{rv.rubricName}</h4>
                        <span className="text-xs text-muted-foreground bg-secondary px-2 py-0.5 rounded-md font-mono">
                          v{rv.versionNumber}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </motion.div>
            )}

            {step === 3 && (
              <motion.div
                key="step3"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                transition={{ duration: 0.2 }}
                className="space-y-6"
              >
                {/* Connectors */}
                <div>
                  <div className="flex items-center gap-2 mb-4 text-foreground/80">
                    <HardDrivesIcon size={20} className="text-primary" />
                    <h3 className="font-medium">{t('connector')}</h3>
                  </div>

                  {connectorsLoading ? (
                    <div className="animate-pulse h-16 w-full bg-muted rounded-lg" />
                  ) : connectors.length === 0 ? (
                    <EmptyStateBlock 
                      message={tw('emptyConnector')}
                      actionHref={`/projects/${projectId}/connectors`}
                      actionLabel={t('createMissingConnector')}
                    />
                  ) : (
                    <div className="grid grid-cols-2 gap-4">
                      {connectors.map(c => (
                        <div 
                          key={c.publicId} 
                          onClick={() => setValue('targetConnectorPublicId', c.publicId)}
                          className={cn(
                            "border p-3 rounded-xl cursor-pointer transition-all hover:shadow-sm relative flex items-center justify-between",
                            selectedValues.targetConnectorPublicId === c.publicId 
                              ? "border-primary bg-primary/5 ring-1 ring-primary" 
                              : "hover:border-foreground/30 bg-card"
                          )}
                        >
                          <span className="font-medium text-sm truncate pr-4">{c.name}</span>
                          {selectedValues.targetConnectorPublicId === c.publicId && (
                            <CheckIcon className="text-primary shrink-0" weight="bold" size={16} />
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                <div className="h-px w-full bg-border" />

                {/* Judge Models */}
                <div>
                  <div className="flex items-center gap-2 mb-4 text-foreground/80">
                    <BrainIcon size={20} className="text-primary" />
                    <h3 className="font-medium">{t('judgeModel')}</h3>
                  </div>

                  {judgeModelsLoading ? (
                    <div className="animate-pulse h-16 w-full bg-muted rounded-lg" />
                  ) : judgeModels.length === 0 ? (
                    <EmptyStateBlock 
                      message={tw('emptyJudgeModel')}
                      actionHref={`/projects/${projectId}/judge-models`}
                      actionLabel={t('createMissingJudgeModel')}
                    />
                  ) : (
                    <div className="grid grid-cols-2 gap-4">
                      {judgeModels.map(jm => (
                        <div 
                          key={jm.publicId} 
                          onClick={() => setValue('judgeModelPublicId', jm.publicId)}
                          className={cn(
                            "border p-3 rounded-xl cursor-pointer transition-all hover:shadow-sm relative flex items-center justify-between",
                            selectedValues.judgeModelPublicId === jm.publicId 
                              ? "border-primary bg-primary/5 ring-1 ring-primary" 
                              : "hover:border-foreground/30 bg-card"
                          )}
                        >
                          <div className="flex flex-col overflow-hidden pr-4">
                            <span className="font-medium text-sm truncate">{jm.name}</span>
                            <span className="text-xs text-muted-foreground truncate">{jm.provider}: {jm.modelName}</span>
                          </div>
                          {selectedValues.judgeModelPublicId === jm.publicId && (
                            <CheckIcon className="text-primary shrink-0" weight="bold" size={16} />
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </motion.div>
            )}

            {step === 4 && (
              <motion.div
                key="step4"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                transition={{ duration: 0.2 }}
                className="space-y-4"
              >
                <div className="flex items-center gap-2 mb-4 text-foreground/80">
                  <LightningIcon size={20} className="text-primary" />
                  <h3 className="font-medium">{tw('reviewAndRun')}</h3>
                </div>

                <div className="bg-secondary/50 rounded-xl border p-5 space-y-4">
                  <div className="grid grid-cols-2 gap-6">
                    <div className="space-y-1">
                      <span className="text-xs text-muted-foreground uppercase tracking-wider font-semibold">{tw('step1')}</span>
                      <p className="font-medium text-sm">{datasets.find(d => d.publicId === selectedValues.datasetPublicId)?.name}</p>
                    </div>
                    <div className="space-y-1">
                      <span className="text-xs text-muted-foreground uppercase tracking-wider font-semibold">{tw('step2')}</span>
                      <p className="font-medium text-sm">{rubricVersions.find(r => r.publicId === selectedValues.rubricVersionPublicId)?.rubricName}</p>
                    </div>
                    <div className="space-y-1">
                      <span className="text-xs text-muted-foreground uppercase tracking-wider font-semibold">{t('connector')}</span>
                      <p className="font-medium text-sm">{connectors.find(c => c.publicId === selectedValues.targetConnectorPublicId)?.name}</p>
                    </div>
                    <div className="space-y-1">
                      <span className="text-xs text-muted-foreground uppercase tracking-wider font-semibold">{t('judgeModel')}</span>
                      <p className="font-medium text-sm">{judgeModels.find(j => j.publicId === selectedValues.judgeModelPublicId)?.name}</p>
                    </div>
                  </div>
                </div>

                <Button 
                  type="button" 
                  size="lg" 
                  className="w-full mt-6 shadow-md"
                  disabled={isSubmitting}
                  onClick={handleSubmit(onSubmit)}
                >
                  {isSubmitting ? (
                    <svg className="mr-2 size-5 animate-spin" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                    </svg>
                  ) : (
                    <LightningIcon className="mr-2" weight="bold" size={20} />
                  )}
                  {tw('runNow')}
                </Button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* Footer actions */}
        <div className="border-t bg-muted/10 p-4 shrink-0 flex items-center justify-between">
          <Button variant="ghost" onClick={() => onOpenChange(false)} disabled={isSubmitting}>
            {tCommon('cancel')}
          </Button>

          <div className="flex gap-2">
            {step > 1 && (
              <Button variant="outline" onClick={handlePrev} disabled={isSubmitting}>
                <CaretLeftIcon className="mr-1" />
                {tw('back')}
              </Button>
            )}
            
            {step < 4 && (
              <Button onClick={handleNext} disabled={!canProceed() || isSubmitting}>
                {tw('next')}
                <CaretRightIcon className="ml-1" />
              </Button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
