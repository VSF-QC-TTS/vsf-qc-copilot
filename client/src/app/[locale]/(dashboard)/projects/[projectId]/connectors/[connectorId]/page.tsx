'use client';

import * as React from 'react';
import { useParams } from 'next/navigation';
import { useForm, Controller, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslations } from 'next-intl';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  PencilSimple,
  FloppyDisk,
  Play,
  SpinnerGap,
  X,
} from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { PageShell } from '@/components/layout/page-shell';
import { StatusBadge } from '@/components/ui/status-badge';
import { KeyValueEditor } from '@/components/connectors/key-value-editor';
import {
  AuthSettingsFields,
  CurlImportPanel,
  buildConnectorBodyPayload,
  buildConnectorAuthPayload,
  buildConnectorUrl,
  type ParsedCurlConnector,
} from '@/components/connectors/connector-form-helpers';
import { Skeleton, SkeletonText } from '@/components/feedback/loading-skeleton';
import { apiClient } from '@/lib/api/client';
import { isApiError } from '@/lib/utils/error-messages';
import {
  createConnectorSchema,
  testRunSchema,
  type CreateConnectorFormValues,
  type TestRunFormValues,
  HTTP_METHODS,
  AUTH_TYPES,
  BODY_TYPES,
  RESPONSE_FORMATS,
} from '@/lib/validations/connector';

// ---------------------------------------------------------------------------
// Shared styles
// ---------------------------------------------------------------------------

const inputClassName =
  'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

const textareaClassName =
  'flex min-h-[80px] w-full resize-none rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

const selectClassName =
  'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

const checkboxClassName =
  'size-4 rounded border border-input bg-background text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2';

// ---------------------------------------------------------------------------
// Connector response type (detail)
// ---------------------------------------------------------------------------

type ConnectorDetail = {
  publicId: string;
  name: string;
  description: string | null;
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
  responseBody: string | null;
  extractedAnswer: string | null;
  error: string | null;
};

type JsonRecord = Record<string, unknown>;

function toStringRecord(record: Record<string, unknown> | null): Record<string, string> {
  if (!record) return {};
  return Object.fromEntries(
    Object.entries(record).map(([key, value]) => [
      key,
      typeof value === 'string' ? value : JSON.stringify(value),
    ]),
  );
}

function toEditableBodyTemplate(
  bodyTemplate: unknown,
  bodyTemplateText: string | null,
): string {
  if (bodyTemplateText) return bodyTemplateText;
  if (!bodyTemplate) return '';
  if (typeof bodyTemplate === 'string') return bodyTemplate;
  return JSON.stringify(bodyTemplate, null, 2);
}

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
    <div className="space-y-1">
      <span className="text-sm font-medium text-muted-foreground">
        {label}
      </span>
      <p className={cn('text-sm', mono && 'font-mono')}>
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
  // Extra fields for editing
  const [headers, setHeaders] = React.useState<Record<string, string>>({});
  const [queryParamsKv, setQueryParamsKv] = React.useState<Record<string, string>>({});
  const [pathParamsKv, setPathParamsKv] = React.useState<Record<string, string>>({});
  const [authConfigKv, setAuthConfigKv] = React.useState<Record<string, string>>({});
  const [secretValuesKv, setSecretValuesKv] = React.useState<Record<string, string>>({});
  const [editorRevision, setEditorRevision] = React.useState(0);

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

  // Populate KV editors when connector loads
  React.useEffect(() => {
    if (!connector) return;

    let cancelled = false;
    queueMicrotask(() => {
      if (cancelled) return;
      setHeaders(toStringRecord(connector.headers));
      setQueryParamsKv(toStringRecord(connector.queryParams));
      setPathParamsKv(toStringRecord(connector.pathParams));
      setAuthConfigKv(toStringRecord(connector.authConfig));
    });

    return () => {
      cancelled = true;
    };
  }, [connector]);

  // ---------------------------------------------------------------------------
  // Edit form
  // ---------------------------------------------------------------------------
  const {
    register,
    handleSubmit,
    reset,
    control,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<CreateConnectorFormValues>({
    resolver: zodResolver(createConnectorSchema),
    values: connector
      ? {
          name: connector.name,
          description: connector.description ?? '',
          protocol: connector.protocol ?? '',
          method: connector.method as CreateConnectorFormValues['method'],
          baseUrl: connector.baseUrl,
          path: connector.path ?? '',
          bodyType: (connector.bodyType as CreateConnectorFormValues['bodyType']) ?? 'NONE',
          bodyTemplate: toEditableBodyTemplate(
            connector.bodyTemplate,
            connector.bodyTemplateText,
          ),
          bodyTemplateText: connector.bodyTemplateText ?? '',
          responseFormat:
            (connector.responseFormat as CreateConnectorFormValues['responseFormat']) ?? 'JSON',
          responseSelector: connector.responseSelector ?? '',
          authType:
            (connector.authType as CreateConnectorFormValues['authType']) ?? 'NONE',
          timeoutSeconds: connector.timeoutSeconds,
          retryCount: connector.retryCount,
          active: connector.active,
          isStreaming: connector.isStreaming,
        }
      : undefined,
  });

  const authType = useWatch({ control, name: 'authType' });
  const bodyType = useWatch({ control, name: 'bodyType' });

  const handleCancelEdit = () => {
    reset();
    if (connector) {
      setHeaders(toStringRecord(connector.headers));
      setQueryParamsKv(toStringRecord(connector.queryParams));
      setPathParamsKv(toStringRecord(connector.pathParams));
      setAuthConfigKv(toStringRecord(connector.authConfig));
      setSecretValuesKv({});
      setEditorRevision((revision) => revision + 1);
    }
    setEditing(false);
  };

  async function onSaveEdit(values: CreateConnectorFormValues) {
    const authPayload = buildConnectorAuthPayload({
      headers,
      queryParams: queryParamsKv,
      pathParams: pathParamsKv,
      authType,
      authConfig: authConfigKv,
      secretValues: secretValuesKv,
    });

    const payload = {
      ...values,
      protocol: values.protocol || 'HTTP',
      url: buildConnectorUrl(values.baseUrl, values.path),
      ...buildConnectorBodyPayload(values.bodyType, values.bodyTemplate),
      ...authPayload,
    };

    try {
      await apiClient.patch(
        '/api/v1/target-api-connectors/' + connectorId,
        payload,
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

  const handleApplyCurl = (parsed: ParsedCurlConnector) => {
    setValue('method', parsed.method);
    setValue('baseUrl', parsed.baseUrl);
    setValue('path', parsed.path);
    setValue('bodyType', parsed.bodyType);
    setValue('bodyTemplate', parsed.bodyTemplate);
    if (parsed.responseSelector) {
      setValue('responseSelector', parsed.responseSelector);
    }
    setValue('authType', parsed.authType);
    setHeaders(parsed.headers);
    setQueryParamsKv(parsed.queryParams);
    setAuthConfigKv(parsed.authConfig);
    setSecretValuesKv(parsed.secretValues);
    setEditorRevision((revision) => revision + 1);
  };

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
              <PencilSimple weight="bold" />
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
          <CurlImportPanel disabled={isSubmitting} onApply={handleApplyCurl} />

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

              <div className="space-y-2">
                <label
                  htmlFor="edit-method"
                  className="text-sm font-medium leading-none text-foreground"
                >
                  {t('fields.method')}
                </label>
                <select
                  id="edit-method"
                  disabled={isSubmitting}
                  className={selectClassName}
                  {...register('method')}
                >
                  {HTTP_METHODS.map((m) => (
                    <option key={m} value={m}>
                      {m}
                    </option>
                  ))}
                </select>
              </div>

              <div className="space-y-2">
                <label
                  htmlFor="edit-baseUrl"
                  className="text-sm font-medium leading-none text-foreground"
                >
                  {t('fields.baseUrl')}
                </label>
                <input
                  id="edit-baseUrl"
                  type="url"
                  disabled={isSubmitting}
                  className={cn(
                    inputClassName,
                    errors.baseUrl &&
                      'border-destructive focus-visible:ring-destructive',
                  )}
                  {...register('baseUrl')}
                />
                {errors.baseUrl && (
                  <p className="text-sm text-destructive">
                    {errors.baseUrl.message}
                  </p>
                )}
              </div>

              <div className="space-y-2 sm:col-span-2">
                <label
                  htmlFor="edit-path"
                  className="text-sm font-medium leading-none text-foreground"
                >
                  {t('fields.path')}
                </label>
                <input
                  id="edit-path"
                  type="text"
                  disabled={isSubmitting}
                  className={inputClassName}
                  {...register('path')}
                />
              </div>
            </div>
          </FormSection>

          {/* Request Config */}
          <FormSection title={t('sections.requestConfig')}>
            <KeyValueEditor
              key={`headers-${editorRevision}`}
              label={t('fields.headers')}
              value={headers}
              onChange={setHeaders}
              disabled={isSubmitting}
            />
            <KeyValueEditor
              key={`query-${editorRevision}`}
              label={t('fields.queryParams')}
              value={queryParamsKv}
              onChange={setQueryParamsKv}
              disabled={isSubmitting}
            />
            <KeyValueEditor
              key={`path-${editorRevision}`}
              label={t('fields.pathParams')}
              value={pathParamsKv}
              onChange={setPathParamsKv}
              disabled={isSubmitting}
            />
          </FormSection>

          {/* Body Template */}
          <FormSection title={t('sections.bodyTemplate')}>
            <div className="space-y-2">
              <label
                htmlFor="edit-bodyType"
                className="text-sm font-medium leading-none text-foreground"
              >
                {t('fields.bodyType')}
              </label>
              <select
                id="edit-bodyType"
                disabled={isSubmitting}
                className={selectClassName}
                {...register('bodyType')}
              >
                {BODY_TYPES.map((bt) => (
                  <option key={bt} value={bt}>
                    {bt}
                  </option>
                ))}
              </select>
            </div>
            {bodyType !== 'NONE' && (
              <div className="space-y-2">
                <label
                  htmlFor="edit-bodyTemplate"
                  className="text-sm font-medium leading-none text-foreground"
                >
                  {t('fields.bodyTemplate')}
                </label>
                <textarea
                  id="edit-bodyTemplate"
                  disabled={isSubmitting}
                  rows={6}
                  className={cn(textareaClassName, 'font-mono text-xs')}
                  {...register('bodyTemplate')}
                />
                <p className="text-xs text-muted-foreground">
                  {t('bodyTemplateHelp')}
                </p>
              </div>
            )}
          </FormSection>

          {/* Authentication */}
          <FormSection title={t('sections.authentication')}>
            <div className="space-y-2">
              <label
                htmlFor="edit-authType"
                className="text-sm font-medium leading-none text-foreground"
              >
                {t('fields.authType')}
              </label>
              <select
                id="edit-authType"
                disabled={isSubmitting}
                className={selectClassName}
                {...register('authType')}
              >
                {AUTH_TYPES.map((at) => (
                  <option key={at} value={at}>
                    {at}
                  </option>
                ))}
              </select>
            </div>

            {authType !== 'NONE' && connector.secretRefs && connector.secretRefs.length > 0 && (
              <div className="space-y-1">
                <span className="text-sm font-medium text-muted-foreground">
                  {t('fields.existingSecrets')}
                </span>
                <div className="flex flex-wrap gap-2">
                  {connector.secretRefs.map((ref) => (
                    <span
                      key={ref.secretKey}
                      className="inline-flex items-center rounded-full bg-zinc-100 px-2.5 py-1 text-xs font-medium text-zinc-600 dark:bg-zinc-800 dark:text-zinc-400"
                    >
                      {ref.secretKey}: {ref.maskedValue}
                    </span>
                  ))}
                </div>
              </div>
            )}

            <AuthSettingsFields
              authType={authType}
              authConfig={authConfigKv}
              secretValues={secretValuesKv}
              onAuthConfigChange={setAuthConfigKv}
              onSecretValuesChange={setSecretValuesKv}
              inputClassName={inputClassName}
              selectClassName={selectClassName}
              disabled={isSubmitting}
            />
          </FormSection>

          {/* Advanced */}
          <FormSection title={t('sections.advanced')}>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <label
                  htmlFor="edit-responseFormat"
                  className="text-sm font-medium leading-none text-foreground"
                >
                  {t('fields.responseFormat')}
                </label>
                <select
                  id="edit-responseFormat"
                  disabled={isSubmitting}
                  className={selectClassName}
                  {...register('responseFormat')}
                >
                  {RESPONSE_FORMATS.map((rf) => (
                    <option key={rf} value={rf}>
                      {rf}
                    </option>
                  ))}
                </select>
              </div>

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

              <div className="flex items-center gap-2 sm:col-span-2">
                <Controller
                  control={control}
                  name="active"
                  render={({ field }) => (
                    <input
                      id="edit-active"
                      type="checkbox"
                      disabled={isSubmitting}
                      checked={field.value}
                      onChange={field.onChange}
                      className={checkboxClassName}
                    />
                  )}
                />
                <label
                  htmlFor="edit-active"
                  className="text-sm font-medium leading-none text-foreground"
                >
                  {t('fields.active')}
                </label>
              </div>

              <div className="flex items-center gap-2 sm:col-span-2">
                <Controller
                  control={control}
                  name="isStreaming"
                  render={({ field }) => (
                    <input
                      id="edit-streaming"
                      type="checkbox"
                      disabled={isSubmitting}
                      checked={field.value}
                      onChange={field.onChange}
                      className={checkboxClassName}
                    />
                  )}
                />
                <label
                  htmlFor="edit-streaming"
                  className="text-sm font-medium leading-none text-foreground"
                >
                  {t('fields.isStreaming')}
                </label>
              </div>
            </div>
          </FormSection>

          {/* Edit actions */}
          <div className="flex justify-end gap-3">
            <Button
              type="button"
              variant="outline"
              disabled={isSubmitting}
              onClick={handleCancelEdit}
            >
              <X weight="bold" />
              {tCommon('cancel')}
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              <FloppyDisk weight="bold" />
              {isSubmitting ? t('saving') : tCommon('save')}
            </Button>
          </div>
        </form>
      ) : (
        /* ---- READ MODE ---- */
        <div className="space-y-6">
          {/* Basic Info */}
          <FormSection title={t('sections.basicInfo')}>
            <div className="grid gap-4 sm:grid-cols-2">
              <ReadOnlyField
                label={t('fields.name')}
                value={connector.name}
              />
              <ReadOnlyField
                label={t('fields.method')}
                value={connector.method}
              />
              <ReadOnlyField
                label={t('fields.baseUrl')}
                value={connector.baseUrl}
                mono
              />
              <ReadOnlyField
                label={t('fields.path')}
                value={connector.path}
                mono
              />
              <div className="sm:col-span-2">
                <ReadOnlyField
                  label={t('fields.description')}
                  value={connector.description}
                />
              </div>
              <ReadOnlyField
                label={t('columns.status')}
                value={
                  <StatusBadge
                    status={connector.active ? 'ACTIVE' : 'INACTIVE'}
                    size="sm"
                  />
                }
              />
              <ReadOnlyField
                label={t('fields.protocol')}
                value={connector.protocol}
              />
              <ReadOnlyField
                label={t('fields.isStreaming')}
                value={connector.isStreaming ? tCommon('yes') : tCommon('no')}
              />
              <ReadOnlyField
                label={t('columns.createdAt')}
                value={new Date(connector.createdAt).toLocaleString()}
              />
              <ReadOnlyField
                label={t('fields.updatedAt')}
                value={new Date(connector.updatedAt).toLocaleString()}
              />
            </div>
          </FormSection>

          {/* Request Config (read-only) */}
          {(connector.headers || connector.queryParams || connector.pathParams) && (
            <FormSection title={t('sections.requestConfig')}>
              {connector.headers &&
                Object.keys(connector.headers).length > 0 && (
                  <div className="space-y-1">
                    <span className="text-sm font-medium text-muted-foreground">
                      {t('fields.headers')}
                    </span>
                    <pre className="overflow-auto rounded-md bg-muted p-3 text-xs">
                      {JSON.stringify(connector.headers, null, 2)}
                    </pre>
                  </div>
                )}
              {connector.queryParams &&
                Object.keys(connector.queryParams).length > 0 && (
                  <div className="space-y-1">
                    <span className="text-sm font-medium text-muted-foreground">
                      {t('fields.queryParams')}
                    </span>
                    <pre className="overflow-auto rounded-md bg-muted p-3 text-xs">
                      {JSON.stringify(connector.queryParams, null, 2)}
                    </pre>
                  </div>
                )}
              {connector.pathParams &&
                Object.keys(connector.pathParams).length > 0 && (
                  <div className="space-y-1">
                    <span className="text-sm font-medium text-muted-foreground">
                      {t('fields.pathParams')}
                    </span>
                    <pre className="overflow-auto rounded-md bg-muted p-3 text-xs">
                      {JSON.stringify(connector.pathParams, null, 2)}
                    </pre>
                  </div>
                )}
            </FormSection>
          )}

          <FormSection title={t('sections.bodyTemplate')}>
            <div className="grid gap-4 sm:grid-cols-2">
              <ReadOnlyField
                label={t('fields.bodyType')}
                value={connector.bodyType}
              />
              <ReadOnlyField
                label={t('fields.responseFormat')}
                value={connector.responseFormat}
              />
            </div>
            <div className="space-y-1">
              <span className="text-sm font-medium text-muted-foreground">
                {t('fields.bodyTemplate')}
              </span>
              <JsonBlock value={connector.bodyTemplate ?? connector.bodyTemplateText} />
            </div>
          </FormSection>

          <FormSection title={t('sections.authentication')}>
            <div className="grid gap-4 sm:grid-cols-2">
              <ReadOnlyField
                label={t('fields.authType')}
                value={connector.authType}
              />
              <div className="space-y-1">
                <span className="text-sm font-medium text-muted-foreground">
                  {t('fields.existingSecrets')}
                </span>
                <JsonBlock value={connector.secretRefs} />
              </div>
            </div>
            <div className="space-y-1">
              <span className="text-sm font-medium text-muted-foreground">
                {t('fields.authConfig')}
              </span>
              <JsonBlock value={connector.authConfig} />
            </div>
          </FormSection>

          {/* Advanced (read-only) */}
          <FormSection title={t('sections.advanced')}>
            <div className="grid gap-4 sm:grid-cols-2">
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
            </div>
          </FormSection>

          <FormSection title={t('sections.apiPayload')}>
            <JsonBlock value={connector} />
          </FormSection>
        </div>
      )}

      {/* ================================================================ */}
      {/* Test Run (04.03) — always visible, below detail/edit */}
      {/* ================================================================ */}
      <FormSection title={t('testRun.title')}>
        <form
          onSubmit={testForm.handleSubmit(onTestRun)}
          className="space-y-4"
        >
          {/* Question */}
          <div className="space-y-2">
            <label
              htmlFor="test-question"
              className="text-sm font-medium leading-none text-foreground"
            >
              {t('testRun.question')}
            </label>
            <textarea
              id="test-question"
              disabled={testRunning}
              rows={3}
              placeholder={t('testRun.questionPlaceholder')}
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

          {/* Precondition */}
          <div className="space-y-2">
            <label
              htmlFor="test-precondition"
              className="text-sm font-medium leading-none text-foreground"
            >
              {t('testRun.precondition')}
            </label>
            <input
              id="test-precondition"
              type="text"
              disabled={testRunning}
              placeholder={t('testRun.preconditionPlaceholder')}
              className={inputClassName}
              {...testForm.register('precondition')}
            />
          </div>

          {/* Metadata */}
          <div className="space-y-2">
            <label
              htmlFor="test-metadata"
              className="text-sm font-medium leading-none text-foreground"
            >
              {t('testRun.metadata')}
            </label>
            <textarea
              id="test-metadata"
              disabled={testRunning}
              rows={2}
              placeholder={t('testRun.metadataPlaceholder')}
              className={textareaClassName}
              {...testForm.register('metadata')}
            />
          </div>

          {/* Run button */}
          <Button type="submit" disabled={testRunning}>
            {testRunning ? (
              <SpinnerGap className="animate-spin" weight="bold" />
            ) : (
              <Play weight="bold" />
            )}
            {testRunning ? t('testRun.running') : t('testRun.run')}
          </Button>
        </form>

        {/* Test run results */}
        {testError && (
          <div className="rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
            {testError}
          </div>
        )}

        {testResult && (
          <div className="space-y-4 rounded-lg border bg-muted/30 p-4">
            <div className="flex items-center gap-4">
              <StatusBadge status={testResult.status} size="sm" />
              <span className="text-sm text-muted-foreground">
                {t('testRun.latency')}: {testResult.latencyMs}ms
              </span>
            </div>

            {/* Extracted answer */}
            {testResult.extractedAnswer && (
              <div className="space-y-1">
                <span className="text-sm font-medium text-foreground">
                  {t('testRun.extractedAnswer')}
                </span>
                <div className="rounded-md border border-emerald-300 bg-emerald-50 p-3 text-sm dark:border-emerald-800 dark:bg-emerald-950">
                  {testResult.extractedAnswer}
                </div>
              </div>
            )}

            {/* Response body */}
            {testResult.responseBody && (
              <div className="space-y-1">
                <span className="text-sm font-medium text-foreground">
                  {t('testRun.responseBody')}
                </span>
                <pre className="max-h-80 overflow-auto rounded-md bg-muted p-3 text-xs font-mono">
                  {testResult.responseBody}
                </pre>
              </div>
            )}

            {/* Error from test run */}
            {testResult.error && (
              <div className="rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
                {testResult.error}
              </div>
            )}

            <div className="space-y-1">
              <span className="text-sm font-medium text-foreground">
                {t('testRun.fullPayload')}
              </span>
              <JsonBlock value={testResult} />
            </div>
          </div>
        )}
      </FormSection>
    </PageShell>
  );
}
