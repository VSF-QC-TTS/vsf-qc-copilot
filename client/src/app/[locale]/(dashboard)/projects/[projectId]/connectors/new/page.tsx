'use client';

import * as React from 'react';
import { useParams } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslations } from 'next-intl';
import { useQueryClient } from '@tanstack/react-query';
import { InfoIcon, CaretDownIcon, CaretUpIcon } from '@phosphor-icons/react';
import { motion, AnimatePresence } from 'motion/react';

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
  collapsible = false,
  defaultOpen = true,
}: {
  title: string;
  children: React.ReactNode;
  collapsible?: boolean;
  defaultOpen?: boolean;
}) {
  const [isOpen, setIsOpen] = React.useState(defaultOpen);

  return (
    <section className={cn("rounded-xl border border-border/50 bg-card shadow-sm transition-all overflow-hidden", !collapsible && "space-y-5 p-6 hover:shadow-md")}>
      {collapsible ? (
        <>
          <div 
            className="flex items-center justify-between p-6 cursor-pointer select-none hover:bg-muted/50"
            onClick={() => setIsOpen(!isOpen)}
          >
            <h2 className="text-base font-semibold tracking-tight text-foreground">{title}</h2>
            <div className="text-muted-foreground flex items-center justify-center rounded-md hover:bg-muted p-1">
              {isOpen ? <CaretUpIcon weight="bold" /> : <CaretDownIcon weight="bold" />}
            </div>
          </div>
          <AnimatePresence initial={false}>
            {isOpen && (
              <motion.div
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: 'auto', opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                transition={{ duration: 0.2 }}
              >
                <div className="px-6 pb-6 pt-0 space-y-5">
                  {children}
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </>
      ) : (
        <>
          <h2 className="text-base font-semibold tracking-tight text-foreground">{title}</h2>
          {children}
        </>
      )}
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
        queryKey: ['target-api-connectors', projectId],
      });
      await queryClient.invalidateQueries({
        queryKey: ['project-readiness', projectId],
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
      <form noValidate onSubmit={handleSubmit(onSubmit)} className="space-y-6">
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

        <FormSection title={t('sections.advanced')} collapsible defaultOpen={false}>
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
