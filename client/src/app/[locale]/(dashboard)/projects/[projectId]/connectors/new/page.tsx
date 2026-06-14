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
  CurlImportPanel,
  buildConnectorBodyPayload,
  buildConnectorAuthPayload,
  buildConnectorUrl,
  type ParsedCurlConnector,
} from '@/components/connectors/connector-form-helpers';
import { apiClient } from '@/lib/api/client';
import {
  createConnectorSchema,
  type CreateConnectorFormValues,
  HTTP_METHODS,
  AUTH_TYPES,
  BODY_TYPES,
  RESPONSE_FORMATS,
} from '@/lib/validations/connector';
import { useRouter } from '@/i18n/navigation';

// ---------------------------------------------------------------------------
// Shared styles (matches project form patterns)
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
// Page component
// ---------------------------------------------------------------------------

export default function CreateConnectorPage() {
  const t = useTranslations('connectors');
  const tCommon = useTranslations('common');
  const router = useRouter();
  const queryClient = useQueryClient();
  const params = useParams();
  const projectId = params.projectId as string;

  // Extra fields not in Zod schema (sent alongside)
  const [headers, setHeaders] = React.useState<Record<string, string>>({});
  const [queryParams, setQueryParams] = React.useState<Record<string, string>>({});
  const [pathParams, setPathParams] = React.useState<Record<string, string>>({});
  const [authConfig, setAuthConfig] = React.useState<Record<string, string>>({});
  const [secretValues, setSecretValues] = React.useState<Record<string, string>>({});
  const [editorRevision, setEditorRevision] = React.useState(0);

  const {
    register,
    handleSubmit,
    control,
    setValue,
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

  // Submit
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
      // apiClient emits a localized toast for normalized backend errors.
    }
  }

  const handleBack = () => {
    router.push(`/projects/${projectId}/connectors`);
  };

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
    setQueryParams(parsed.queryParams);
    setAuthConfig(parsed.authConfig);
    setSecretValues(parsed.secretValues);
    setEditorRevision((revision) => revision + 1);
  };

  return (
    <PageShell
      title={t('createConnector')}
      backHref={`/projects/${projectId}/connectors`}
      backLabel={tCommon('back')}
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <CurlImportPanel disabled={isSubmitting} onApply={handleApplyCurl} />

        {/* ================================================================ */}
        {/* Basic Info */}
        {/* ================================================================ */}
        <FormSection title={t('sections.basicInfo')}>
          <div className="grid gap-4 sm:grid-cols-2">
            {/* Name */}
            <div className="space-y-2 sm:col-span-2">
              <label
                htmlFor="connector-name"
                className="text-sm font-medium leading-none text-foreground"
              >
                {t('fields.name')}
              </label>
              <input
                id="connector-name"
                type="text"
                autoFocus
                disabled={isSubmitting}
                placeholder={t('placeholders.name')}
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

            {/* Description */}
            <div className="space-y-2 sm:col-span-2">
              <label
                htmlFor="connector-description"
                className="text-sm font-medium leading-none text-foreground"
              >
                {t('fields.description')}
              </label>
              <textarea
                id="connector-description"
                disabled={isSubmitting}
                placeholder={t('placeholders.description')}
                className={cn(
                  textareaClassName,
                  errors.description &&
                    'border-destructive focus-visible:ring-destructive',
                )}
                {...register('description')}
              />
            </div>

            {/* Method */}
            <div className="space-y-2">
              <label
                htmlFor="connector-method"
                className="text-sm font-medium leading-none text-foreground"
              >
                {t('fields.method')}
              </label>
              <select
                id="connector-method"
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
              {errors.method && (
                <p className="text-sm text-destructive">
                  {errors.method.message}
                </p>
              )}
            </div>

            {/* Base URL */}
            <div className="space-y-2">
              <label
                htmlFor="connector-baseUrl"
                className="text-sm font-medium leading-none text-foreground"
              >
                {t('fields.baseUrl')}
              </label>
              <input
                id="connector-baseUrl"
                type="url"
                disabled={isSubmitting}
                placeholder="https://api.example.com"
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

            {/* Path */}
            <div className="space-y-2 sm:col-span-2">
              <label
                htmlFor="connector-path"
                className="text-sm font-medium leading-none text-foreground"
              >
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

        {/* ================================================================ */}
        {/* Request Config */}
        {/* ================================================================ */}
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

        {/* ================================================================ */}
        {/* Body Template */}
        {/* ================================================================ */}
        <FormSection title={t('sections.bodyTemplate')}>
          {/* Body Type */}
          <div className="space-y-2">
            <label
              htmlFor="connector-bodyType"
              className="text-sm font-medium leading-none text-foreground"
            >
              {t('fields.bodyType')}
            </label>
            <select
              id="connector-bodyType"
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

          {/* Body Template */}
          {bodyType !== 'NONE' && (
            <div className="space-y-2">
              <label
                htmlFor="connector-bodyTemplate"
                className="text-sm font-medium leading-none text-foreground"
              >
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
              <p className="text-xs text-muted-foreground">
                {t('bodyTemplateHelp')}
              </p>
            </div>
          )}
        </FormSection>

        {/* ================================================================ */}
        {/* Authentication */}
        {/* ================================================================ */}
        <FormSection title={t('sections.authentication')}>
          {/* Auth Type */}
          <div className="space-y-2">
            <label
              htmlFor="connector-authType"
              className="text-sm font-medium leading-none text-foreground"
            >
              {t('fields.authType')}
            </label>
            <select
              id="connector-authType"
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

        {/* ================================================================ */}
        {/* Advanced */}
        {/* ================================================================ */}
        <FormSection title={t('sections.advanced')}>
          <div className="grid gap-4 sm:grid-cols-2">
            {/* Response Format */}
            <div className="space-y-2">
              <label
                htmlFor="connector-responseFormat"
                className="text-sm font-medium leading-none text-foreground"
              >
                {t('fields.responseFormat')}
              </label>
              <select
                id="connector-responseFormat"
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

            {/* Response Selector */}
            <div className="space-y-2">
              <label
                htmlFor="connector-responseSelector"
                className="text-sm font-medium leading-none text-foreground"
              >
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
              <p className="text-xs text-muted-foreground">
                {t('responseSelectorHelp')}
              </p>
              {errors.responseSelector && (
                <p className="text-sm text-destructive">
                  {errors.responseSelector.message}
                </p>
              )}
            </div>

            {/* Timeout */}
            <div className="space-y-2">
              <label
                htmlFor="connector-timeout"
                className="text-sm font-medium leading-none text-foreground"
              >
                {t('fields.timeoutSeconds')}
              </label>
              <input
                id="connector-timeout"
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
              {errors.timeoutSeconds && (
                <p className="text-sm text-destructive">
                  {errors.timeoutSeconds.message}
                </p>
              )}
            </div>

            {/* Retry Count */}
            <div className="space-y-2">
              <label
                htmlFor="connector-retry"
                className="text-sm font-medium leading-none text-foreground"
              >
                {t('fields.retryCount')}
              </label>
              <input
                id="connector-retry"
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
              {errors.retryCount && (
                <p className="text-sm text-destructive">
                  {errors.retryCount.message}
                </p>
              )}
            </div>

            {/* Active checkbox */}
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
              <label
                htmlFor="connector-active"
                className="text-sm font-medium leading-none text-foreground"
              >
                {t('fields.active')}
              </label>
            </div>

            {/* isStreaming checkbox */}
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
              <label
                htmlFor="connector-streaming"
                className="text-sm font-medium leading-none text-foreground"
              >
                {t('fields.isStreaming')}
              </label>
            </div>
          </div>
        </FormSection>

        {/* ================================================================ */}
        {/* Actions */}
        {/* ================================================================ */}
        <div className="flex justify-end gap-3">
          <Button
            type="button"
            variant="outline"
            disabled={isSubmitting}
            onClick={handleBack}
          >
            {tCommon('cancel')}
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? t('creating') : t('createConnector')}
          </Button>
        </div>
      </form>
    </PageShell>
  );
}
