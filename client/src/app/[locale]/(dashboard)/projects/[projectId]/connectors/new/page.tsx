'use client';

import * as React from 'react';
import { useParams } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslations } from 'next-intl';
import { useQueryClient } from '@tanstack/react-query';
import { InfoIcon } from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { PageShell } from '@/components/layout/page-shell';
import { apiClient } from '@/lib/api/client';
import {
  createConnectorFromCurlSchema,
  type CreateConnectorFromCurlFormValues,
} from '@/lib/validations/connector';
import { useRouter } from '@/i18n/navigation';

// ---------------------------------------------------------------------------
// Shared styles
// ---------------------------------------------------------------------------

const inputClassName =
  'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

const textareaClassName =
  'flex min-h-[80px] w-full resize-none rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

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
    <section className="space-y-5 rounded-xl border border-border/50 bg-card p-6 shadow-sm transition-all hover:shadow-md">
      <h2 className="text-base font-semibold tracking-tight text-foreground">{title}</h2>
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
    <PageShell
      title={t('createConnector')}
      backHref={`/projects/${projectId}/connectors`}
      backLabel={tCommon('back')}
    >
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
            <div className="space-y-3 sm:col-span-2">
              <div className="flex items-center gap-2">
                <label htmlFor="qc-rawCurl" className="text-sm font-medium leading-none text-foreground">
                  {t('rawCurl')}
                </label>
              </div>
              <p className="text-[13px] text-muted-foreground flex items-start gap-2 rounded-md border bg-blue-50/50 p-3 dark:bg-blue-950/20 dark:border-blue-900/30">
                <InfoIcon className="mt-0.5 size-4 shrink-0 text-blue-500" weight="fill" />
                <span>{t('curl.helpPlaceholder')}</span>
              </p>
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
    </PageShell>
  );
}
