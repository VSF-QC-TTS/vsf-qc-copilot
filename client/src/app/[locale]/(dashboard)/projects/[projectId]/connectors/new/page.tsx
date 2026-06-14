'use client';

import * as React from 'react';
import { useParams } from 'next/navigation';
import { useForm, Controller, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslations } from 'next-intl';
import { useQueryClient } from '@tanstack/react-query';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { PageShell } from '@/components/layout/page-shell';
import { KeyValueEditor } from '@/components/connectors/key-value-editor';
import {
  AuthSettingsFields,
  buildConnectorBodyPayload,
  buildConnectorAuthPayload,
  buildConnectorUrl,
} from '@/components/connectors/connector-form-helpers';
import { apiClient } from '@/lib/api/client';
import {
  createConnectorSchema,
  createConnectorFromCurlSchema,
  type CreateConnectorFormValues,
  type CreateConnectorFromCurlFormValues,
  HTTP_METHODS,
  AUTH_TYPES,
  BODY_TYPES,
  RESPONSE_FORMATS,
} from '@/lib/validations/connector';
import { useRouter } from '@/i18n/navigation';

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
// Quick Create Form
// ---------------------------------------------------------------------------

function QuickCreateForm({ projectId }: { projectId: string }) {
  const t = useTranslations('connectors');
  const tCommon = useTranslations('common');
  const router = useRouter();
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CreateConnectorFromCurlFormValues>({
    resolver: zodResolver(createConnectorFromCurlSchema),
    defaultValues: {
      name: '',
      description: '',
      rawCurl: '',
      responseSelector: '',
      timeoutSeconds: 60,
      retryCount: 1,
    },
  });

  const handleBack = () => {
    router.push(`/projects/${projectId}/connectors`);
  };

  async function onSubmit(values: CreateConnectorFromCurlFormValues) {
    try {
      await apiClient.post(
        `/api/v1/projects/${projectId}/target-api-connectors/from-curl`,
        values,
      );
      await queryClient.invalidateQueries({
        queryKey: ['connectors', projectId],
      });
      router.push(`/projects/${projectId}/connectors`);
    } catch {
      // apiClient emits toast
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <FormSection title={t('sections.basicInfo')}>
        <div className="grid gap-4 sm:grid-cols-2">
          {/* Name */}
          <div className="space-y-2 sm:col-span-2">
            <label htmlFor="qc-name" className="text-sm font-medium leading-none text-foreground">
              {t('fields.name')}
            </label>
            <input
              id="qc-name"
              type="text"
              autoFocus
              disabled={isSubmitting}
              placeholder={t('placeholders.name')}
              className={cn(inputClassName, errors.name && 'border-destructive')}
              {...register('name')}
            />
            {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
          </div>

          {/* Raw cURL */}
          <div className="space-y-2 sm:col-span-2">
            <label htmlFor="qc-rawCurl" className="text-sm font-medium leading-none text-foreground">
              {t('rawCurl')}
            </label>
            <textarea
              id="qc-rawCurl"
              disabled={isSubmitting}
              rows={5}
              placeholder={t('rawCurlPlaceholder')}
              className={cn(textareaClassName, 'font-mono text-xs', errors.rawCurl && 'border-destructive')}
              {...register('rawCurl')}
            />
            {errors.rawCurl && <p className="text-sm text-destructive">{errors.rawCurl.message}</p>}
          </div>

          {/* Description */}
          <div className="space-y-2 sm:col-span-2">
            <label htmlFor="qc-description" className="text-sm font-medium leading-none text-foreground">
              {t('fields.description')}
            </label>
            <textarea
              id="qc-description"
              disabled={isSubmitting}
              placeholder={t('placeholders.description')}
              className={cn(textareaClassName, errors.description && 'border-destructive')}
              {...register('description')}
            />
          </div>
        </div>
      </FormSection>

      <FormSection title={t('sections.advanced')}>
        <div className="grid gap-4 sm:grid-cols-2">
          {/* Response Selector */}
          <div className="space-y-2">
            <label htmlFor="qc-responseSelector" className="text-sm font-medium leading-none text-foreground">
              {t('fields.responseSelector')}
            </label>
            <input
              id="qc-responseSelector"
              type="text"
              disabled={isSubmitting}
              placeholder="$.candidates[0].content.parts[0].text"
              className={inputClassName}
              {...register('responseSelector')}
            />
            <p className="text-xs text-muted-foreground">{t('responseSelectorHelp')}</p>
          </div>

          {/* Timeout */}
          <div className="space-y-2">
            <label htmlFor="qc-timeoutSeconds" className="text-sm font-medium leading-none text-foreground">
              {t('fields.timeoutSeconds')}
            </label>
            <input
              id="qc-timeoutSeconds"
              type="number"
              min={1}
              disabled={isSubmitting}
              className={cn(inputClassName, errors.timeoutSeconds && 'border-destructive')}
              {...register('timeoutSeconds')}
            />
          </div>

          {/* Retry Count */}
          <div className="space-y-2">
            <label htmlFor="qc-retryCount" className="text-sm font-medium leading-none text-foreground">
              {t('fields.retryCount')}
            </label>
            <input
              id="qc-retryCount"
              type="number"
              min={0}
              disabled={isSubmitting}
              className={cn(inputClassName, errors.retryCount && 'border-destructive')}
              {...register('retryCount')}
            />
          </div>
        </div>
      </FormSection>

      <div className="flex justify-end gap-3">
        <Button type="button" variant="outline" disabled={isSubmitting} onClick={handleBack}>
          {tCommon('cancel')}
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? t('creating') : t('createConnector')}
        </Button>
      </div>
    </form>
  );
}

// ---------------------------------------------------------------------------
// Manual Create Form
// ---------------------------------------------------------------------------

function ManualCreateForm({ projectId }: { projectId: string }) {
  const t = useTranslations('connectors');
  const tCommon = useTranslations('common');
  const router = useRouter();
  const queryClient = useQueryClient();

  const [headers, setHeaders] = React.useState<Record<string, string>>({});
  const [queryParams, setQueryParams] = React.useState<Record<string, string>>({});
  const [pathParams, setPathParams] = React.useState<Record<string, string>>({});
  const [authConfig, setAuthConfig] = React.useState<Record<string, string>>({});
  const [secretValues, setSecretValues] = React.useState<Record<string, string>>({});
  const [editorRevision] = React.useState(0);

  const {
    register,
    handleSubmit,
    control,
    formState: { errors, isSubmitting },
  } = useForm<CreateConnectorFormValues>({
    resolver: zodResolver(createConnectorSchema),
    defaultValues: {
      name: '',
      description: '',
      protocol: '',
      method: 'POST',
      baseUrl: '',
      path: '',
      bodyType: 'RAW_JSON',
      bodyTemplate: '',
      bodyTemplateText: '',
      responseFormat: 'JSON',
      responseSelector: '',
      authType: 'NONE',
      timeoutSeconds: 30,
      retryCount: 0,
      active: true,
      isStreaming: false,
    },
  });

  const authType = useWatch({ control, name: 'authType' });
  const bodyType = useWatch({ control, name: 'bodyType' });

  async function onSubmit(values: CreateConnectorFormValues) {
    const authPayload = buildConnectorAuthPayload({
      headers,
      queryParams,
      pathParams,
      authType,
      authConfig,
      secretValues,
    });

    const payload = {
      ...values,
      protocol: values.protocol || 'HTTP',
      url: buildConnectorUrl(values.baseUrl, values.path),
      ...buildConnectorBodyPayload(values.bodyType, values.bodyTemplate),
      ...authPayload,
    };

    try {
      await apiClient.post(
        `/api/v1/projects/${projectId}/target-api-connectors`,
        payload,
      );
      await queryClient.invalidateQueries({
        queryKey: ['connectors', projectId],
      });
      router.push(`/projects/${projectId}/connectors`);
    } catch {
      // apiClient emits toast
    }
  }

  const handleBack = () => {
    router.push(`/projects/${projectId}/connectors`);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <FormSection title={t('sections.basicInfo')}>
        <div className="grid gap-4 sm:grid-cols-2">
          {/* Name */}
          <div className="space-y-2 sm:col-span-2">
            <label htmlFor="connector-name" className="text-sm font-medium leading-none text-foreground">
              {t('fields.name')}
            </label>
            <input
              id="connector-name"
              type="text"
              autoFocus
              disabled={isSubmitting}
              placeholder={t('placeholders.name')}
              className={cn(inputClassName, errors.name && 'border-destructive focus-visible:ring-destructive')}
              {...register('name')}
            />
            {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
          </div>

          {/* Description */}
          <div className="space-y-2 sm:col-span-2">
            <label htmlFor="connector-description" className="text-sm font-medium leading-none text-foreground">
              {t('fields.description')}
            </label>
            <textarea
              id="connector-description"
              disabled={isSubmitting}
              placeholder={t('placeholders.description')}
              className={cn(textareaClassName, errors.description && 'border-destructive focus-visible:ring-destructive')}
              {...register('description')}
            />
          </div>

          {/* Method */}
          <div className="space-y-2">
            <label htmlFor="connector-method" className="text-sm font-medium leading-none text-foreground">
              {t('fields.method')}
            </label>
            <select
              id="connector-method"
              disabled={isSubmitting}
              className={selectClassName}
              {...register('method')}
            >
              {HTTP_METHODS.map((m) => (
                <option key={m} value={m}>{m}</option>
              ))}
            </select>
            {errors.method && <p className="text-sm text-destructive">{errors.method.message}</p>}
          </div>

          {/* Base URL */}
          <div className="space-y-2">
            <label htmlFor="connector-baseUrl" className="text-sm font-medium leading-none text-foreground">
              {t('fields.baseUrl')}
            </label>
            <input
              id="connector-baseUrl"
              type="url"
              disabled={isSubmitting}
              placeholder="https://api.example.com"
              className={cn(inputClassName, errors.baseUrl && 'border-destructive focus-visible:ring-destructive')}
              {...register('baseUrl')}
            />
            {errors.baseUrl && <p className="text-sm text-destructive">{errors.baseUrl.message}</p>}
          </div>

          {/* Path */}
          <div className="space-y-2 sm:col-span-2">
            <label htmlFor="connector-path" className="text-sm font-medium leading-none text-foreground">
              {t('fields.path')}
            </label>
            <input
              id="connector-path"
              type="text"
              disabled={isSubmitting}
              placeholder="/v1/chat/completions"
              className={inputClassName}
              {...register('path')}
            />
          </div>
        </div>
      </FormSection>

      <FormSection title={t('sections.requestConfig')}>
        <KeyValueEditor
          key={`headers-${editorRevision}`}
          label={t('fields.headers')}
          value={headers}
          onChange={setHeaders}
          disabled={isSubmitting}
          keyPlaceholder="Content-Type"
          valuePlaceholder="application/json"
        />
        <KeyValueEditor
          key={`query-${editorRevision}`}
          label={t('fields.queryParams')}
          value={queryParams}
          onChange={setQueryParams}
          disabled={isSubmitting}
        />
        <KeyValueEditor
          key={`path-${editorRevision}`}
          label={t('fields.pathParams')}
          value={pathParams}
          onChange={setPathParams}
          disabled={isSubmitting}
        />
      </FormSection>

      <FormSection title={t('sections.bodyTemplate')}>
        <div className="space-y-2">
          <label htmlFor="connector-bodyType" className="text-sm font-medium leading-none text-foreground">
            {t('fields.bodyType')}
          </label>
          <select id="connector-bodyType" disabled={isSubmitting} className={selectClassName} {...register('bodyType')}>
            {BODY_TYPES.map((bt) => (
              <option key={bt} value={bt}>{bt}</option>
            ))}
          </select>
        </div>
        {bodyType !== 'NONE' && (
          <div className="space-y-2">
            <label htmlFor="connector-bodyTemplate" className="text-sm font-medium leading-none text-foreground">
              {t('fields.bodyTemplate')}
            </label>
            <textarea
              id="connector-bodyTemplate"
              disabled={isSubmitting}
              rows={6}
              placeholder={t('placeholders.bodyTemplate')}
              className={cn(textareaClassName, 'font-mono text-xs')}
              {...register('bodyTemplate')}
            />
            <p className="text-xs text-muted-foreground">{t('bodyTemplateHelp')}</p>
          </div>
        )}
      </FormSection>

      <FormSection title={t('sections.authentication')}>
        <div className="space-y-2">
          <label htmlFor="connector-authType" className="text-sm font-medium leading-none text-foreground">
            {t('fields.authType')}
          </label>
          <select id="connector-authType" disabled={isSubmitting} className={selectClassName} {...register('authType')}>
            {AUTH_TYPES.map((at) => (
              <option key={at} value={at}>{at}</option>
            ))}
          </select>
        </div>
        <AuthSettingsFields
          authType={authType}
          authConfig={authConfig}
          secretValues={secretValues}
          onAuthConfigChange={setAuthConfig}
          onSecretValuesChange={setSecretValues}
          inputClassName={inputClassName}
          selectClassName={selectClassName}
          disabled={isSubmitting}
        />
      </FormSection>

      <FormSection title={t('sections.advanced')}>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-2">
            <label htmlFor="connector-responseFormat" className="text-sm font-medium leading-none text-foreground">
              {t('fields.responseFormat')}
            </label>
            <select id="connector-responseFormat" disabled={isSubmitting} className={selectClassName} {...register('responseFormat')}>
              {RESPONSE_FORMATS.map((rf) => (
                <option key={rf} value={rf}>{rf}</option>
              ))}
            </select>
          </div>
          <div className="space-y-2">
            <label htmlFor="connector-responseSelector" className="text-sm font-medium leading-none text-foreground">
              {t('fields.responseSelector')}
            </label>
            <input
              id="connector-responseSelector"
              type="text"
              disabled={isSubmitting}
              placeholder="$.candidates[0].content.parts[0].text"
              className={inputClassName}
              {...register('responseSelector')}
            />
            <p className="text-xs text-muted-foreground">{t('responseSelectorHelp')}</p>
            {errors.responseSelector && <p className="text-sm text-destructive">{errors.responseSelector.message}</p>}
          </div>
          <div className="space-y-2">
            <label htmlFor="connector-timeout" className="text-sm font-medium leading-none text-foreground">
              {t('fields.timeoutSeconds')}
            </label>
            <input
              id="connector-timeout"
              type="number"
              min={1}
              max={300}
              disabled={isSubmitting}
              className={cn(inputClassName, errors.timeoutSeconds && 'border-destructive')}
              {...register('timeoutSeconds')}
            />
            {errors.timeoutSeconds && <p className="text-sm text-destructive">{errors.timeoutSeconds.message}</p>}
          </div>
          <div className="space-y-2">
            <label htmlFor="connector-retry" className="text-sm font-medium leading-none text-foreground">
              {t('fields.retryCount')}
            </label>
            <input
              id="connector-retry"
              type="number"
              min={0}
              max={5}
              disabled={isSubmitting}
              className={cn(inputClassName, errors.retryCount && 'border-destructive')}
              {...register('retryCount')}
            />
            {errors.retryCount && <p className="text-sm text-destructive">{errors.retryCount.message}</p>}
          </div>
          <div className="flex items-center gap-2 sm:col-span-2">
            <Controller
              control={control}
              name="active"
              render={({ field }) => (
                <input
                  id="connector-active"
                  type="checkbox"
                  disabled={isSubmitting}
                  checked={field.value}
                  onChange={field.onChange}
                  className={checkboxClassName}
                />
              )}
            />
            <label htmlFor="connector-active" className="text-sm font-medium leading-none text-foreground">
              {t('fields.active')}
            </label>
          </div>
          <div className="flex items-center gap-2 sm:col-span-2">
            <Controller
              control={control}
              name="isStreaming"
              render={({ field }) => (
                <input
                  id="connector-streaming"
                  type="checkbox"
                  disabled={isSubmitting}
                  checked={field.value}
                  onChange={field.onChange}
                  className={checkboxClassName}
                />
              )}
            />
            <label htmlFor="connector-streaming" className="text-sm font-medium leading-none text-foreground">
              {t('fields.isStreaming')}
            </label>
          </div>
        </div>
      </FormSection>

      <div className="flex justify-end gap-3">
        <Button type="button" variant="outline" disabled={isSubmitting} onClick={handleBack}>
          {tCommon('cancel')}
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? t('creating') : t('createConnector')}
        </Button>
      </div>
    </form>
  );
}

// ---------------------------------------------------------------------------
// Page component
// ---------------------------------------------------------------------------

export default function CreateConnectorPage() {
  const t = useTranslations('connectors');
  const tCommon = useTranslations('common');
  const params = useParams();
  const projectId = params.projectId as string;
  const [mode, setMode] = React.useState<'quick' | 'manual'>('quick');

  return (
    <PageShell
      title={t('createConnector')}
      backHref={`/projects/${projectId}/connectors`}
      backLabel={tCommon('back')}
    >
      <div className="mb-6 flex space-x-1 rounded-lg bg-muted p-1 sm:w-fit">
        <button
          type="button"
          onClick={() => setMode('quick')}
          className={cn(
            'px-4 py-2 text-sm font-medium transition-all rounded-md',
            mode === 'quick' ? 'bg-background shadow-sm text-foreground' : 'text-muted-foreground hover:text-foreground'
          )}
        >
          {t('quickCreate')}
        </button>
        <button
          type="button"
          onClick={() => setMode('manual')}
          className={cn(
            'px-4 py-2 text-sm font-medium transition-all rounded-md',
            mode === 'manual' ? 'bg-background shadow-sm text-foreground' : 'text-muted-foreground hover:text-foreground'
          )}
        >
          {t('manualSetup')}
        </button>
      </div>

      {mode === 'quick' ? (
        <QuickCreateForm projectId={projectId} />
      ) : (
        <ManualCreateForm projectId={projectId} />
      )}
    </PageShell>
  );
}
