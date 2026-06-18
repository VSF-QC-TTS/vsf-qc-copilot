'use client';

import * as React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslations } from 'next-intl';
import { useQueryClient } from '@tanstack/react-query';
import {
  XIcon,
  TrashSimpleIcon,
  PlusIcon,
  ArrowUpIcon,
  ArrowDownIcon,
  UserIcon,
  RobotIcon,
  GearSixIcon,
} from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { apiClient } from '@/lib/api/client';
import type { ApiError, TestCaseStatus } from '@/lib/api/types';
import { getErrorMessageKey } from '@/lib/utils/error-messages';
import {
  createTestCaseSchema,
  type CreateTestCaseFormValues,
} from '@/lib/validations/test-case';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type Turn = { role: string; content: string };

type TestCaseResponse = {
  publicId: string;
  question: string | null;
  turns: Turn[] | null;
  groundTruth: string | null;
  precondition: Record<string, unknown> | null;
  metadata: Record<string, unknown> | null;
  status: TestCaseStatus;
  createdAt: string;
  updatedAt: string;
};

interface TestCaseEditorProps {
  datasetId: string;
  testCase?: TestCaseResponse | null;
  isOpen: boolean;
  onClose: () => void;
  isReadOnly?: boolean;
}

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const ROLES = ['user', 'assistant', 'system'] as const;

const ROLE_ICON: Record<string, React.ElementType> = {
  user: UserIcon,
  assistant: RobotIcon,
  system: GearSixIcon,
};

const ROLE_COLORS: Record<string, string> = {
  user: 'border-l-blue-500 bg-blue-500/5',
  assistant: 'border-l-emerald-500 bg-emerald-500/5',
  system: 'border-l-amber-500 bg-amber-500/5',
};

// ---------------------------------------------------------------------------
// Shared input styles
// ---------------------------------------------------------------------------

const inputClassName =
  'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

const textareaClassName =
  'flex min-h-[100px] w-full resize-none rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function TestCaseEditor({
  datasetId,
  testCase,
  isOpen,
  onClose,
  isReadOnly = false,
}: TestCaseEditorProps) {
  const t = useTranslations('testCases');
  const tCommon = useTranslations('common');
  const tErrors = useTranslations('errors');
  const queryClient = useQueryClient();

  const isEditMode = !!testCase;

  const [serverError, setServerError] = React.useState<string | null>(null);
  const [turns, setTurns] = React.useState<Turn[]>([]);
  const [mode, setMode] = React.useState<'single' | 'multi'>('single');
  const [confirmDeleteOpen, setConfirmDeleteOpen] = React.useState(false);
  const [deleting, setDeleting] = React.useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateTestCaseFormValues>({
    resolver: zodResolver(createTestCaseSchema),
    defaultValues: {
      question: '',
      turns: null,
      groundTruth: '',
      precondition: '',
      metadata: '',
    },
  });

  /* ---- Sync form when testCase changes ---- */
  React.useEffect(() => {
    if (!isOpen) return;

    let cancelled = false;
    queueMicrotask(() => {
      if (cancelled) return;

      const hasTurns = testCase?.turns && testCase.turns.length > 0;
      setMode(hasTurns ? 'multi' : 'single');
      setTurns(hasTurns ? [...testCase.turns!] : []);

      reset({
        question: testCase?.question ?? '',
        turns: testCase?.turns ?? null,
        groundTruth: testCase?.groundTruth ?? '',
        precondition: testCase?.precondition ? JSON.stringify(testCase.precondition, null, 2) : '',
        metadata: testCase?.metadata ? JSON.stringify(testCase.metadata, null, 2) : '',
      });
      setServerError(null);
    });

    return () => {
      cancelled = true;
    };
  }, [isOpen, testCase, reset]);

  /* ---- Escape key ---- */
  React.useEffect(() => {
    if (!isOpen) return;

    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        e.stopPropagation();
        onClose();
      }
    }

    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [isOpen, onClose]);

  /* ---- Lock body scroll ---- */
  React.useEffect(() => {
    if (!isOpen) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prev;
    };
  }, [isOpen]);

  // -----------------------------------------------------------------------
  // Turn management helpers
  // -----------------------------------------------------------------------

  const addTurn = () => {
    // Alternate roles: if the last turn is 'user', add 'assistant' and vice versa
    const lastRole = turns.length > 0 ? turns[turns.length - 1].role : 'assistant';
    const nextRole = lastRole === 'user' ? 'assistant' : 'user';
    setTurns((prev) => [...prev, { role: nextRole, content: '' }]);
  };

  const removeTurn = (index: number) => {
    setTurns((prev) => prev.filter((_, i) => i !== index));
  };

  const updateTurnRole = (index: number, role: string) => {
    setTurns((prev) =>
      prev.map((turn, i) => (i === index ? { ...turn, role } : turn)),
    );
  };

  const updateTurnContent = (index: number, content: string) => {
    setTurns((prev) =>
      prev.map((turn, i) => (i === index ? { ...turn, content } : turn)),
    );
  };

  const moveTurn = (index: number, direction: 'up' | 'down') => {
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= turns.length) return;
    setTurns((prev) => {
      const next = [...prev];
      [next[index], next[targetIndex]] = [next[targetIndex], next[index]];
      return next;
    });
  };

  // -----------------------------------------------------------------------
  // Submit (create / edit)
  // -----------------------------------------------------------------------

  async function onSubmit(values: CreateTestCaseFormValues) {
    setServerError(null);

    try {
      const payload = {
        ...values,
        turns: mode === 'multi' && turns.length > 0 ? turns : null,
        // In multi-turn mode, use the first user message as the question fallback
        question:
          mode === 'multi'
            ? (turns.find((t) => t.role === 'user')?.content ?? values.question ?? '')
            : (values.question ?? ''),
        precondition: values.precondition ? JSON.parse(values.precondition) : null,
        metadata: values.metadata ? JSON.parse(values.metadata) : null,
      };

      if (isEditMode) {
        await apiClient.patch(
          `/api/v1/test-cases/${testCase.publicId}`,
          payload,
        );
      } else {
        await apiClient.post(
          `/api/v1/datasets/${datasetId}/test-cases`,
          payload,
        );
      }

      await queryClient.invalidateQueries({
        queryKey: ['test-cases', datasetId],
      });
      await queryClient.invalidateQueries({
        queryKey: ['dataset', datasetId],
      });
      onClose();
    } catch (error: unknown) {
      if (
        typeof error === 'object' &&
        error !== null &&
        'code' in error &&
        'status' in error &&
        'message' in error
      ) {
        const apiError = error as ApiError;
        const messageKey = getErrorMessageKey(apiError);
        const key = messageKey.replace(/^errors\./, '');
        setServerError(tErrors(key));
      } else {
        setServerError(tErrors('network'));
      }
    }
  }

  /* ---- Delete ---- */
  async function handleDelete() {
    if (!testCase) return;
    setDeleting(true);

    try {
      await apiClient.del(`/api/v1/test-cases/${testCase.publicId}`);
      await queryClient.invalidateQueries({
        queryKey: ['test-cases', datasetId],
      });
      await queryClient.invalidateQueries({
        queryKey: ['dataset', datasetId],
      });
      setConfirmDeleteOpen(false);
      onClose();
    } catch (error: unknown) {
      if (
        typeof error === 'object' &&
        error !== null &&
        'code' in error &&
        'status' in error &&
        'message' in error
      ) {
        const apiError = error as ApiError;
        const messageKey = getErrorMessageKey(apiError);
        const key = messageKey.replace(/^errors\./, '');
        setServerError(tErrors(key));
      } else {
        setServerError(tErrors('network'));
      }
    } finally {
      setDeleting(false);
    }
  }

  if (!isOpen) return null;

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-40 bg-black/50 backdrop-blur-sm"
        onClick={() => !isSubmitting && onClose()}
        aria-hidden="true"
      />

      {/* Panel */}
      <div
        data-slot="test-case-editor"
        role="dialog"
        aria-modal="true"
        aria-labelledby="test-case-editor-title"
        className={cn(
          'fixed right-0 top-0 z-40 h-full w-full max-w-lg border-l bg-background shadow-xl',
          'animate-in slide-in-from-right',
        )}
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b px-6 py-4">
          <h2
            id="test-case-editor-title"
            className="text-lg font-semibold text-foreground"
          >
            {isReadOnly
              ? t('viewTitle')
              : isEditMode
                ? t('editTitle')
                : t('createTitle')}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded-md p-1 text-muted-foreground transition-colors hover:text-foreground"
          >
            <XIcon className="size-5" />
          </button>
        </div>

        {/* Body */}
        <form noValidate
          onSubmit={handleSubmit(onSubmit)}
          className="flex h-[calc(100%-65px)] flex-col"
        >
          <div className="flex-1 space-y-4 overflow-y-auto px-6 py-4">
            {serverError && (
              <div className="rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
                {serverError}
              </div>
            )}

            {/* Mode toggle: Single-turn vs Multi-turn */}
            <div className="flex gap-1 rounded-lg border bg-muted/30 p-1">
              <button
                type="button"
                onClick={() => setMode('single')}
                disabled={isReadOnly}
                className={cn(
                  'flex-1 rounded-md px-3 py-1.5 text-sm font-medium transition-colors',
                  mode === 'single'
                    ? 'bg-background text-foreground shadow-sm'
                    : 'text-muted-foreground hover:text-foreground',
                )}
              >
                {t('singleTurn')}
              </button>
              <button
                type="button"
                onClick={() => setMode('multi')}
                disabled={isReadOnly}
                className={cn(
                  'flex-1 rounded-md px-3 py-1.5 text-sm font-medium transition-colors',
                  mode === 'multi'
                    ? 'bg-background text-foreground shadow-sm'
                    : 'text-muted-foreground hover:text-foreground',
                )}
              >
                {t('multiTurn')}
              </button>
            </div>

            {/* Single-turn: Question field */}
            {mode === 'single' && (
              <div className="space-y-2">
                <label
                  htmlFor="tc-question"
                  className="text-sm font-medium leading-none text-foreground"
                >
                  {t('fieldQuestion')} <span className="text-destructive">*</span>
                </label>
                <textarea
                  id="tc-question"
                  disabled={isSubmitting || isReadOnly}
                  placeholder={t('fieldQuestionPlaceholder')}
                  className={cn(
                    textareaClassName,
                    errors.question &&
                      'border-destructive focus-visible:ring-destructive',
                  )}
                  {...register('question')}
                />
                {errors.question && (
                  <p className="text-sm text-destructive">
                    {errors.question.message}
                  </p>
                )}
              </div>
            )}

            {/* Multi-turn: Dynamic message array */}
            {mode === 'multi' && (
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <label className="text-sm font-medium leading-none text-foreground">
                    {t('multiTurn')}
                  </label>
                  {!isReadOnly && (
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={addTurn}
                      disabled={isSubmitting}
                      className="h-7 gap-1 text-xs"
                    >
                      <PlusIcon className="size-3.5" />
                      {t('addMessage')}
                    </Button>
                  )}
                </div>

                <p className="text-xs text-muted-foreground">
                  {t('turnsHint')}
                </p>

                {turns.length === 0 && (
                  <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-8 text-center bg-muted/10">
                    <p className="text-sm text-muted-foreground">
                      {t('turnsEmpty')}
                    </p>
                  </div>
                )}

                <div className="space-y-2">
                  {turns.map((turn, index) => {
                    const RoleIcon = ROLE_ICON[turn.role] ?? UserIcon;
                    const colorClass = ROLE_COLORS[turn.role] ?? 'border-l-gray-400 bg-muted/10';

                    return (
                      <div
                        key={index}
                        className={cn(
                          'rounded-lg border border-l-4 p-3 space-y-2 transition-colors',
                          colorClass,
                        )}
                      >
                        {/* Turn header: role selector + actions */}
                        <div className="flex items-center justify-between gap-2">
                          <div className="flex items-center gap-2">
                            <RoleIcon className="size-4 text-muted-foreground shrink-0" />
                            <select
                              value={turn.role}
                              onChange={(e) => updateTurnRole(index, e.target.value)}
                              disabled={isReadOnly || isSubmitting}
                              className="h-7 rounded-md border border-input bg-background px-2 text-xs font-medium text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
                            >
                              {ROLES.map((r) => (
                                <option key={r} value={r}>
                                  {t(`role${r.charAt(0).toUpperCase()}${r.slice(1)}` as 'roleUser' | 'roleAssistant' | 'roleSystem')}
                                </option>
                              ))}
                            </select>
                            <span className="text-xs text-muted-foreground font-mono">
                              #{index + 1}
                            </span>
                          </div>

                          {!isReadOnly && (
                            <div className="flex items-center gap-0.5">
                              <button
                                type="button"
                                onClick={() => moveTurn(index, 'up')}
                                disabled={index === 0 || isSubmitting}
                                className="rounded p-1 text-muted-foreground transition-colors hover:text-foreground hover:bg-muted disabled:opacity-30 disabled:hover:bg-transparent"
                                title={t('moveUp')}
                              >
                                <ArrowUpIcon className="size-3.5" />
                              </button>
                              <button
                                type="button"
                                onClick={() => moveTurn(index, 'down')}
                                disabled={index === turns.length - 1 || isSubmitting}
                                className="rounded p-1 text-muted-foreground transition-colors hover:text-foreground hover:bg-muted disabled:opacity-30 disabled:hover:bg-transparent"
                                title={t('moveDown')}
                              >
                                <ArrowDownIcon className="size-3.5" />
                              </button>
                              <button
                                type="button"
                                onClick={() => removeTurn(index)}
                                disabled={isSubmitting}
                                className="rounded p-1 text-muted-foreground transition-colors hover:text-destructive hover:bg-destructive/10"
                                title={t('removeMessage')}
                              >
                                <TrashSimpleIcon className="size-3.5" />
                              </button>
                            </div>
                          )}
                        </div>

                        {/* Content textarea */}
                        <textarea
                          value={turn.content}
                          onChange={(e) => updateTurnContent(index, e.target.value)}
                          disabled={isReadOnly || isSubmitting}
                          placeholder={t('messageContentPlaceholder')}
                          rows={2}
                          className={cn(
                            textareaClassName,
                            'min-h-[60px] text-sm',
                          )}
                        />
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Ground Truth */}
            <div className="space-y-2">
              <label
                htmlFor="tc-ground-truth"
                className="text-sm font-medium leading-none text-foreground"
              >
                {t('fieldGroundTruth')}
              </label>
              <textarea
                id="tc-ground-truth"
                disabled={isSubmitting || isReadOnly}
                placeholder={t('fieldGroundTruthPlaceholder')}
                className={cn(
                  textareaClassName,
                  errors.groundTruth &&
                    'border-destructive focus-visible:ring-destructive',
                )}
                {...register('groundTruth')}
              />
              {errors.groundTruth && (
                <p className="text-sm text-destructive">
                  {errors.groundTruth.message}
                </p>
              )}
            </div>

            {/* Precondition */}
            <div className="space-y-2">
              <label
                htmlFor="tc-precondition"
                className="text-sm font-medium leading-none text-foreground"
              >
                {t('fieldPrecondition')}
              </label>
              <input
                id="tc-precondition"
                type="text"
                disabled={isSubmitting || isReadOnly}
                placeholder={t('fieldPreconditionPlaceholder')}
                className={cn(
                  inputClassName,
                  errors.precondition &&
                    'border-destructive focus-visible:ring-destructive',
                )}
                {...register('precondition')}
              />
              {errors.precondition && (
                <p className="text-sm text-destructive">
                  {errors.precondition.message}
                </p>
              )}
            </div>

            {/* Metadata */}
            <div className="space-y-2">
              <label
                htmlFor="tc-metadata"
                className="text-sm font-medium leading-none text-foreground"
              >
                {t('fieldMetadata')}
              </label>
              <textarea
                id="tc-metadata"
                disabled={isSubmitting || isReadOnly}
                placeholder={t('fieldMetadataPlaceholder')}
                className={cn(
                  textareaClassName,
                  'min-h-[80px] font-mono text-xs',
                  errors.metadata &&
                    'border-destructive focus-visible:ring-destructive',
                )}
                {...register('metadata')}
              />
              <p className="text-xs text-muted-foreground">
                {t('fieldMetadataHint')}
              </p>
              {errors.metadata && (
                <p className="text-sm text-destructive">
                  {errors.metadata.message}
                </p>
              )}
            </div>
          </div>

          {/* Footer */}
          {!isReadOnly && (
            <div className="flex items-center justify-between border-t px-6 py-4">
              <div>
                {isEditMode && (
                  <Button
                    type="button"
                    variant="destructive"
                    size="sm"
                    disabled={isSubmitting}
                    onClick={() => setConfirmDeleteOpen(true)}
                  >
                    <TrashSimpleIcon className="mr-1.5 size-4" />
                    {tCommon('delete')}
                  </Button>
                )}
              </div>
              <div className="flex gap-3">
                <Button
                  type="button"
                  variant="outline"
                  disabled={isSubmitting}
                  onClick={onClose}
                >
                  {tCommon('cancel')}
                </Button>
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting
                    ? tCommon('saving')
                    : isEditMode
                      ? tCommon('save')
                      : t('createTestCase')}
                </Button>
              </div>
            </div>
          )}
        </form>
      </div>

      {/* Delete confirmation */}
      <ConfirmDialog
        open={confirmDeleteOpen}
        onOpenChange={setConfirmDeleteOpen}
        title={t('deleteTitle')}
        description={t('deleteDescription')}
        confirmLabel={tCommon('delete')}
        cancelLabel={tCommon('cancel')}
        variant="destructive"
        onConfirm={handleDelete}
        loading={deleting}
      />
    </>
  );
}
