'use client';

import * as React from 'react';
import { useParams } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslations } from 'next-intl';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  PencilSimpleIcon,
  FloppyDiskIcon,
  PlayIcon,
  SpinnerGapIcon,
  XIcon,
  InfoIcon,
} from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { PageShell } from '@/components/layout/page-shell';
import { StatusBadge } from '@/components/ui/status-badge';
import { Skeleton, SkeletonText } from '@/components/feedback/loading-skeleton';
import { apiClient } from '@/lib/api/client';
import { isApiError } from '@/lib/utils/error-messages';
import {
  createConnectorFromCurlSchema,
  testRunSchema,
  type CreateConnectorFromCurlFormValues,
  type TestRunFormValues,
} from '@/lib/validations/connector';

// ---------------------------------------------------------------------------
// Shared styles
// ---------------------------------------------------------------------------

const inputClassName =
  'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

const textareaClassName =
  'flex min-h-[80px] w-full resize-none rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';


// ---------------------------------------------------------------------------
// Connector response type (detail)
// ---------------------------------------------------------------------------

type ConnectorDetail = {
  publicId: string;
  name: string;
  description: string | null;
  rawCurl: string | null;
  protocol: string | null;
  method: string;
  baseUrl: string;
  path: string | null;
  headers: Record<string, string | number | boolean> | null;
  queryParams: Record<string, string | number | boolean> | null;
  pathParams: Record<string, string | number | boolean> | null;
  bodyType: string | null;
  bodyTemplate: unknown;
  bodyTemplateText: string | null;
  responseFormat: string | null;
  responseSelector: string | null;
  authType: string | null;
  authConfig: Record<string, unknown> | null;
  secretRefs: { secretKey: string; maskedValue: string }[] | null;
  timeoutSeconds: number;
  retryCount: number;
  active: boolean;
  isStreaming: boolean;
  createdAt: string;
  updatedAt: string;
};

type TestRunResult = {
  status: string;
  latencyMs: number;
  rawResponse: Record<string, unknown> | null;
  extractedAnswer: string | null;
  error: string | null;
};

type JsonRecord = Record<string, unknown>;

// ---------------------------------------------------------------------------
// Section wrapper
// ---------------------------------------------------------------------------

function FormSection({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <section className="space-y-4 rounded-lg border bg-card p-6">
      <h2 className="text-lg font-semibold tracking-tight">{title}</h2>
      {children}
    </section>
  );
}

// ---------------------------------------------------------------------------
// Read-only field
// ---------------------------------------------------------------------------

function ReadOnlyField({
  label,
  value,
  mono = false,
}: {
  label: string;
  value: React.ReactNode;
  mono?: boolean;
}) {
  const tCommon = useTranslations('common');

  return (
    <div className="space-y-1 overflow-hidden">
      <span className="text-sm font-medium text-muted-foreground block truncate">
        {label}
      </span>
      <p className={cn('text-sm break-all', mono && 'font-mono')}>
        {value || tCommon('notAvailable')}
      </p>
    </div>
  );
}

function JsonBlock({ value }: { value: unknown }) {
  const tCommon = useTranslations('common');
  const isEmptyObject =
    value &&
    typeof value === 'object' &&
    !Array.isArray(value) &&
    Object.keys(value as JsonRecord).length === 0;

  if (
    value === null ||
    value === undefined ||
    value === '' ||
    (Array.isArray(value) && value.length === 0) ||
    isEmptyObject
  ) {
    return <p className="text-sm text-muted-foreground">{tCommon('notAvailable')}</p>;
  }

  const rendered =
    typeof value === 'string' ? value : JSON.stringify(value, null, 2);

  return (
    <pre className="max-h-96 overflow-auto rounded-md bg-muted p-3 font-mono text-xs">
      {rendered}
    </pre>
  );
}

// ---------------------------------------------------------------------------
// Detail skeleton
// ---------------------------------------------------------------------------

function ConnectorDetailSkeleton() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div className="space-y-2">
          <Skeleton className="h-7 w-48" />
          <Skeleton className="h-4 w-32" />
        </div>
        <Skeleton className="h-9 w-24" />
      </div>
      <div className="rounded-lg border bg-card p-6 space-y-4">
        <SkeletonText width="w-3/4" />
        <div className="grid gap-4 sm:grid-cols-2">
          <SkeletonText width="w-1/2" />
          <SkeletonText width="w-1/2" />
        </div>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Page component
// ---------------------------------------------------------------------------

export default function ConnectorDetailPage() {
  const t = useTranslations('connectors');
  const tCommon = useTranslations('common');
  const tErrors = useTranslations('errors');
  const queryClient = useQueryClient();
  const params = useParams();
  const projectId = params.projectId as string;
  const connectorId = params.connectorId as string;

  // State
  const [editing, setEditing] = React.useState(false);

  // Test run state
  const [testRunning, setTestRunning] = React.useState(false);
  const [testResult, setTestResult] = React.useState<TestRunResult | null>(null);
  const [testError, setTestError] = React.useState<string | null>(null);

  // ---------------------------------------------------------------------------
  // Fetch connector
  // ---------------------------------------------------------------------------
  const {
    data: connector,
    isLoading,
  } = useQuery({
    queryKey: ['connector', connectorId],
    queryFn: () =>
      apiClient.get<ConnectorDetail>(
        '/api/v1/target-api-connectors/' + connectorId,
      ),
  });

  // ---------------------------------------------------------------------------
  // Edit form
  // ---------------------------------------------------------------------------
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateConnectorFromCurlFormValues>({
    resolver: zodResolver(createConnectorFromCurlSchema),
    values: connector
      ? {
          name: connector.name,
          description: connector.description ?? '',
          rawCurl: connector.rawCurl ?? '',
          responseSelector: connector.responseSelector ?? '',
          timeoutSeconds: connector.timeoutSeconds,
          retryCount: connector.retryCount,
        }
      : undefined,
  });

  const handleCancelEdit = () => {
    reset();
    setEditing(false);
  };

  async function onSaveEdit(values: CreateConnectorFromCurlFormValues) {
    try {
      await apiClient.put(
        `/api/v1/target-api-connectors/${connectorId}/from-curl`,
        values,
      );
      await queryClient.invalidateQueries({
        queryKey: ['connector', connectorId],
      });
      await queryClient.invalidateQueries({
        queryKey: ['connectors', projectId],
      });
      setEditing(false);
    } catch {
      // apiClient emits a localized toast for normalized backend errors.
    }
  }

  // ---------------------------------------------------------------------------
  // Test Run form
  // ---------------------------------------------------------------------------
  const testForm = useForm<TestRunFormValues>({
    resolver: zodResolver(testRunSchema),
    defaultValues: {
      question: '',
      precondition: '',
      metadata: '',
    },
  });

  async function onTestRun(values: TestRunFormValues) {
    setTestRunning(true);
    setTestResult(null);
    setTestError(null);

    try {
      const result = await apiClient.post<TestRunResult>(
        '/api/v1/target-api-connectors/' + connectorId + '/test-runs',
        {
          question: values.question,
          precondition: values.precondition || undefined,
          metadata: values.metadata || undefined,
        },
      );
      setTestResult(result);
    } catch (error: unknown) {
      if (
        typeof error === 'object' &&
        error !== null &&
        'message' in error
      ) {
        setTestError(isApiError(error) ? error.message : tErrors('network'));
      } else {
        setTestError(tErrors('network'));
      }
    } finally {
      setTestRunning(false);
    }
  }

  // ---------------------------------------------------------------------------
  // Loading
  // ---------------------------------------------------------------------------
  if (isLoading) {
    return (
      <PageShell
        title={t('connectorDetail')}
        backHref={`/projects/${projectId}/connectors`}
        backLabel={tCommon('back')}
      >
        <ConnectorDetailSkeleton />
      </PageShell>
    );
  }

  if (!connector) {
    return null;
  }

  // ---------------------------------------------------------------------------
  // Render
  // ---------------------------------------------------------------------------
  return (
    <PageShell
      title={connector.name}
      description={t('connectorDetail')}
      backHref={`/projects/${projectId}/connectors`}
      backLabel={tCommon('back')}
      actions={
        <div className="flex items-center gap-2">
          {!editing && (
            <Button onClick={() => setEditing(true)}>
              <PencilSimpleIcon weight="bold" />
              {tCommon('edit')}
            </Button>
          )}
        </div>
      }
    >
      {/* ================================================================ */}
      {/* Read mode / Edit mode */}
      {/* ================================================================ */}
      {editing ? (
        /* ---- EDIT FORM ---- */
        <form onSubmit={handleSubmit(onSaveEdit)} className="space-y-6">
          {/* Basic Info */}
          <FormSection title={t('sections.basicInfo')}>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2 sm:col-span-2">
                <label
                  htmlFor="edit-name"
                  className="text-sm font-medium leading-none text-foreground"
                >
                  {t('fields.name')}
                </label>
                <input
                  id="edit-name"
                  type="text"
                  disabled={isSubmitting}
                  className={cn(
                    inputClassName,
                    errors.name &&
                      'border-destructive focus-visible:ring-destructive',
                  )}
                  {...register('name')}
                />
                {errors.name && (
                  <p className="text-sm text-destructive">
                    {errors.name.message}
                  </p>
                )}
              </div>

              <div className="space-y-2 sm:col-span-2">
                <div className="flex items-center gap-2">
                  <label
                    htmlFor="edit-rawCurl"
                    className="text-sm font-medium leading-none text-foreground"
                  >
                    {t('rawCurl')}
                  </label>
                </div>
                <p className="text-[13px] text-muted-foreground flex items-start gap-2 rounded-md border bg-blue-50/50 p-3 dark:bg-blue-950/20 dark:border-blue-900/30">
                  <InfoIcon className="mt-0.5 size-4 shrink-0 text-blue-500" weight="fill" />
                  <span>{t('curl.helpPlaceholder')}</span>
                </p>
                <textarea
                  id="edit-rawCurl"
                  disabled={isSubmitting}
                  rows={5}
                  className={cn(
                    textareaClassName,
                    'font-mono text-xs',
                    errors.rawCurl &&
                      'border-destructive focus-visible:ring-destructive',
                  )}
                  {...register('rawCurl')}
                />
                {errors.rawCurl && (
                  <p className="text-sm text-destructive">
                    {errors.rawCurl.message}
                  </p>
                )}
              </div>

              <div className="space-y-2 sm:col-span-2">
                <label
                  htmlFor="edit-description"
                  className="text-sm font-medium leading-none text-foreground"
                >
                  {t('fields.description')}
                </label>
                <textarea
                  id="edit-description"
                  disabled={isSubmitting}
                  className={textareaClassName}
                  {...register('description')}
                />
              </div>
            </div>
          </FormSection>

          {/* Advanced */}
          <FormSection title={t('sections.advanced')}>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <label
                  htmlFor="edit-responseSelector"
                  className="text-sm font-medium leading-none text-foreground"
                >
                  {t('fields.responseSelector')}
                </label>
                <input
                  id="edit-responseSelector"
                  type="text"
                  disabled={isSubmitting}
                  placeholder="$.candidates[0].content.parts[0].text"
                  className={inputClassName}
                  {...register('responseSelector')}
                />
                <p className="text-xs text-muted-foreground">
                  {t('responseSelectorHelp')}
                </p>
                {errors.responseSelector && (
                  <p className="text-sm text-destructive">
                    {errors.responseSelector.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <label
                  htmlFor="edit-timeout"
                  className="text-sm font-medium leading-none text-foreground"
                >
                  {t('fields.timeoutSeconds')}
                </label>
                <input
                  id="edit-timeout"
                  type="number"
                  min={1}
                  max={300}
                  disabled={isSubmitting}
                  className={cn(
                    inputClassName,
                    errors.timeoutSeconds &&
                      'border-destructive focus-visible:ring-destructive',
                  )}
                  {...register('timeoutSeconds')}
                />
              </div>

              <div className="space-y-2">
                <label
                  htmlFor="edit-retry"
                  className="text-sm font-medium leading-none text-foreground"
                >
                  {t('fields.retryCount')}
                </label>
                <input
                  id="edit-retry"
                  type="number"
                  min={0}
                  max={5}
                  disabled={isSubmitting}
                  className={cn(
                    inputClassName,
                    errors.retryCount &&
                      'border-destructive focus-visible:ring-destructive',
                  )}
                  {...register('retryCount')}
                />
              </div>

            </div>
          </FormSection>

          <div className="flex justify-end gap-3 pt-4">
            <Button
              type="button"
              variant="outline"
              disabled={isSubmitting}
              onClick={handleCancelEdit}
            >
              {tCommon('cancel')}
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              <FloppyDiskIcon weight="bold" />
              {tCommon('save')}
            </Button>
          </div>
        </form>
      ) : (
        /* ---- READ MODE ---- */
        <div className="space-y-6">
          <div className="grid gap-6 md:grid-cols-2">
            <FormSection title={t('sections.basicInfo')}>
              <div className="grid gap-4">
                <ReadOnlyField label={t('fields.name')} value={connector.name} />
                <ReadOnlyField
                  label={t('fields.description')}
                  value={connector.description}
                />
                <ReadOnlyField
                  label={t('fields.status')}
                  value={
                    <StatusBadge
                      status={connector.active ? 'ACTIVE' : 'INACTIVE'}
                    />
                  }
                />
                <div className="grid gap-4 sm:grid-cols-2">
                  <ReadOnlyField
                    label={t('fields.method')}
                    value={connector.method}
                    mono
                  />
                  <ReadOnlyField
                    label={t('fields.protocol')}
                    value={connector.protocol}
                    mono
                  />
                  <div className="sm:col-span-2">
                    <ReadOnlyField
                      label={t('fields.url')}
                      value={connector.baseUrl + (connector.path ?? '')}
                      mono
                    />
                  </div>
                </div>
              </div>
            </FormSection>

            <FormSection title={t('sections.authentication')}>
              <div className="grid gap-4">
                <ReadOnlyField
                  label={t('fields.authType')}
                  value={connector.authType}
                  mono
                />
                {connector.secretRefs && connector.secretRefs.length > 0 && (
                  <div className="space-y-1">
                    <span className="text-sm font-medium text-muted-foreground">
                      {t('fields.secrets')}
                    </span>
                    <div className="flex flex-col gap-2">
                      {connector.secretRefs.map((ref) => (
                        <div
                          key={ref.secretKey}
                          className="flex items-center gap-2 font-mono text-xs"
                        >
                          <span className="font-semibold text-foreground">
                            {ref.secretKey}
                          </span>
                          <span className="text-muted-foreground">
                            {ref.maskedValue}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
                {connector.authConfig && Object.keys(connector.authConfig).length > 0 && (
                  <div className="space-y-1">
                    <span className="text-sm font-medium text-muted-foreground">
                      {t('fields.authConfig')}
                    </span>
                    <JsonBlock value={connector.authConfig} />
                  </div>
                )}
              </div>
            </FormSection>
          </div>

          <div className="grid gap-6 md:grid-cols-2">
            <FormSection title={t('sections.requestConfig')}>
              <div className="grid gap-4">
                <div className="space-y-1">
                  <span className="text-sm font-medium text-muted-foreground">
                    {t('fields.headers')}
                  </span>
                  <JsonBlock value={connector.headers} />
                </div>
                <div className="space-y-1">
                  <span className="text-sm font-medium text-muted-foreground">
                    {t('fields.queryParams')}
                  </span>
                  <JsonBlock value={connector.queryParams} />
                </div>
              </div>
            </FormSection>

            <FormSection title={t('sections.bodyTemplate')}>
              <div className="grid gap-4">
                <ReadOnlyField
                  label={t('fields.bodyType')}
                  value={connector.bodyType}
                  mono
                />
                <div className="space-y-1">
                  <span className="text-sm font-medium text-muted-foreground">
                    {t('fields.bodyTemplate')}
                  </span>
                  {connector.bodyType === 'RAW_JSON' ? (
                    <JsonBlock value={connector.bodyTemplate} />
                  ) : connector.bodyTemplateText ? (
                    <pre className="max-h-96 overflow-auto rounded-md bg-muted p-3 font-mono text-xs">
                      {connector.bodyTemplateText}
                    </pre>
                  ) : (
                    <p className="text-sm text-muted-foreground">
                      {tCommon('notAvailable')}
                    </p>
                  )}
                </div>
              </div>
            </FormSection>
          </div>

          <FormSection title={t('sections.advanced')}>
            <div className="grid gap-4 sm:grid-cols-3">
              <ReadOnlyField
                label={t('fields.responseFormat')}
                value={connector.responseFormat}
                mono
              />
              <ReadOnlyField
                label={t('fields.responseSelector')}
                value={connector.responseSelector}
                mono
              />
              <ReadOnlyField
                label={t('fields.timeoutSeconds')}
                value={`${connector.timeoutSeconds}s`}
              />
              <ReadOnlyField
                label={t('fields.retryCount')}
                value={connector.retryCount}
              />
              <ReadOnlyField
                label={t('fields.isStreaming')}
                value={connector.isStreaming ? 'Yes' : 'No'}
              />
            </div>
          </FormSection>
        </div>
      )}

      {/* ================================================================ */}
      {/* TEST RUN SECTION */}
      {/* ================================================================ */}
      <div className="mt-8">
        <h3 className="mb-4 text-lg font-semibold tracking-tight">
          {t('testRun.title')}
        </h3>
        <p className="mb-6 text-sm text-muted-foreground">
          {t('testRun.description')}
        </p>

        <div className="grid gap-8 lg:grid-cols-2">
          {/* Form Side */}
          <div className="rounded-lg border bg-card p-6">
            <form
              onSubmit={testForm.handleSubmit(onTestRun)}
              className="space-y-4"
            >
              <div className="space-y-2">
                <label
                  htmlFor="tr-question"
                  className="text-sm font-medium leading-none text-foreground"
                >
                  {t('testRun.question')}
                </label>
                <textarea
                  id="tr-question"
                  disabled={testRunning}
                  placeholder={t('testRun.questionPlaceholder')}
                  rows={3}
                  className={cn(
                    textareaClassName,
                    testForm.formState.errors.question &&
                      'border-destructive focus-visible:ring-destructive',
                  )}
                  {...testForm.register('question')}
                />
                {testForm.formState.errors.question && (
                  <p className="text-sm text-destructive">
                    {testForm.formState.errors.question.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <label
                  htmlFor="tr-precondition"
                  className="text-sm font-medium leading-none text-foreground"
                >
                  {t('testRun.precondition')}
                </label>
                <textarea
                  id="tr-precondition"
                  disabled={testRunning}
                  placeholder={t('testRun.preconditionPlaceholder')}
                  rows={2}
                  className={textareaClassName}
                  {...testForm.register('precondition')}
                />
              </div>

              <div className="space-y-2">
                <label
                  htmlFor="tr-metadata"
                  className="text-sm font-medium leading-none text-foreground"
                >
                  {t('testRun.metadata')}
                </label>
                <textarea
                  id="tr-metadata"
                  disabled={testRunning}
                  placeholder={t('testRun.metadataPlaceholder')}
                  rows={2}
                  className={textareaClassName}
                  {...testForm.register('metadata')}
                />
              </div>

              <div className="pt-2">
                <Button type="submit" disabled={testRunning} className="w-full">
                  {testRunning ? (
                    <SpinnerGapIcon className="animate-spin" weight="bold" />
                  ) : (
                    <PlayIcon weight="fill" />
                  )}
                  {testRunning ? t('testRun.running') : t('testRun.runButton')}
                </Button>
              </div>
            </form>
          </div>

          {/* Result Side */}
          <div className="rounded-lg border bg-muted/50 p-6">
            {testRunning ? (
              <div className="flex h-full min-h-[200px] flex-col items-center justify-center space-y-4 text-muted-foreground">
                <SpinnerGapIcon className="size-8 animate-spin" />
                <p className="text-sm">{t('testRun.running')}</p>
              </div>
            ) : testError ? (
              <div className="space-y-4">
                <div className="flex items-center gap-2 text-destructive">
                  <XIcon className="size-5" weight="bold" />
                  <h4 className="font-medium">{t('testRun.error')}</h4>
                </div>
                <pre className="whitespace-pre-wrap rounded-md bg-destructive/10 p-4 font-mono text-sm text-destructive">
                  {testError}
                </pre>
              </div>
            ) : testResult ? (
              <div className="space-y-6">
                <div className="flex items-center justify-between">
                  <h4 className="font-medium">
                    {t('testRun.result')}
                  </h4>
                  <StatusBadge
                    status={testResult.error ? 'FAILED' : 'COMPLETED'}
                  />
                </div>

                <div className="space-y-4">
                  <div className="space-y-1.5">
                    <span className="text-xs font-medium text-muted-foreground">
                      {t('testRun.latency')}
                    </span>
                    <p className="text-sm font-medium">
                      {testResult.latencyMs}ms
                    </p>
                  </div>

                  {testResult.error && (
                    <div className="space-y-1.5">
                      <span className="text-xs font-medium text-muted-foreground">
                        {t('testRun.errorDetails')}
                      </span>
                      <pre className="whitespace-pre-wrap rounded-md border border-destructive/20 bg-destructive/10 p-3 font-mono text-xs text-destructive">
                        {testResult.error}
                      </pre>
                    </div>
                  )}

                  <div className="space-y-1.5">
                    <span className="text-xs font-medium text-muted-foreground">
                      {t('testRun.extractedAnswer')}
                    </span>
                    {testResult.extractedAnswer ? (
                      <div className="rounded-md border bg-background p-3 text-sm">
                        {testResult.extractedAnswer}
                      </div>
                    ) : (
                      <p className="text-sm italic text-muted-foreground">
                        {t('testRun.noAnswer')}
                      </p>
                    )}
                  </div>

                  <div className="space-y-1.5">
                    <span className="text-xs font-medium text-muted-foreground">
                      {t('testRun.rawResponse')}
                    </span>
                    <pre className="max-h-96 overflow-auto rounded-md border bg-background p-3 font-mono text-xs">
                      {typeof testResult.rawResponse === 'string'
                        ? testResult.rawResponse
                        : JSON.stringify(testResult.rawResponse, null, 2)}
                    </pre>
                  </div>
                </div>
              </div>
            ) : (
              <div className="flex h-full min-h-[200px] items-center justify-center">
                <p className="text-center text-sm text-muted-foreground">
                  {t('testRun.emptyState')}
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </PageShell>
  );
}
