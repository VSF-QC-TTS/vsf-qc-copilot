'use client';

import * as React from 'react';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { type ColumnDef } from '@tanstack/react-table';
import { PlusIcon } from '@phosphor-icons/react';

import { Button } from '@/components/ui/button';
import { DataTable } from '@/components/data-table/data-table';
import { DataTablePagination } from '@/components/data-table/data-table-pagination';
import { PageShell } from '@/components/layout/page-shell';
import { StatusBadge } from '@/components/ui/status-badge';
import { apiClient } from '@/lib/api/client';
import type {
  JudgeModelResponse,
  JudgeProvider,
  PageResponse,
} from '@/lib/api/types';

const PAGE_SIZE = 10;
const PROVIDERS: JudgeProvider[] = [
  'GEMINI',
  'OPENAI',
  'ANTHROPIC',
  'DEEPSEEK',
  'CUSTOM',
];
const DEFAULT_MODELS: Record<JudgeProvider, string> = {
  GEMINI: 'gemini-2.5-flash',
  OPENAI: 'gpt-4.1-mini',
  ANTHROPIC: 'claude-3-5-haiku-latest',
  DEEPSEEK: 'deepseek-chat',
  CUSTOM: '',
};

const inputClassName =
  'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

const textareaClassName =
  'flex min-h-[80px] w-full resize-none rounded-md border border-input bg-background px-3 py-2 text-sm font-mono ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

type CreateJudgeModelPayload = {
  name: string;
  provider: JudgeProvider;
  modelName: string;
  baseUrl: string;
  apiKey: string;
  configJson: string;
  active: boolean;
};

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

function initialPayload(): CreateJudgeModelPayload {
  return {
    name: '',
    provider: 'GEMINI',
    modelName: DEFAULT_MODELS.GEMINI,
    baseUrl: '',
    apiKey: '',
    configJson: '',
    active: true,
  };
}

export default function JudgeModelsPage() {
  const t = useTranslations('judgeModels');
  const tCommon = useTranslations('common');
  const params = useParams();
  const queryClient = useQueryClient();
  const projectId = params.projectId as string;

  const [page, setPage] = React.useState(0);
  const [form, setForm] = React.useState<CreateJudgeModelPayload>(() =>
    initialPayload(),
  );
  const [advancedOpen, setAdvancedOpen] = React.useState(false);
  const [serverError, setServerError] = React.useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['judge-models', projectId, { page, size: PAGE_SIZE }],
    queryFn: () =>
      apiClient.get<PageResponse<JudgeModelResponse>>(
        `/api/v1/projects/${projectId}/judge-models?page=${page}&size=${PAGE_SIZE}`,
      ),
  });

  const createMutation = useMutation({
    mutationFn: (payload: CreateJudgeModelPayload) =>
      apiClient.post<JudgeModelResponse>(
        `/api/v1/projects/${projectId}/judge-models`,
        payload,
      ),
    onSuccess: async () => {
      setForm(initialPayload());
      setServerError(null);
      await queryClient.invalidateQueries({
        queryKey: ['judge-models', projectId],
      });
      await queryClient.invalidateQueries({
        queryKey: ['judge-models-active', projectId],
      });
    },
    onError: (error: unknown) => {
      const message =
        typeof error === 'object' && error !== null && 'message' in error
          ? String((error as { message: unknown }).message)
          : t('saveFailed');
      setServerError(message);
    },
  });

  const judgeModels = data?.items ?? [];
  const totalItems = data?.totalItems ?? 0;
  const totalPages = data?.totalPages ?? 0;

  const columns = React.useMemo<ColumnDef<JudgeModelResponse, unknown>[]>(
    () => [
      {
        accessorKey: 'name',
        header: t('columns.name'),
        cell: ({ row }) => (
          <span className="font-medium">{row.original.name}</span>
        ),
      },
      {
        accessorKey: 'provider',
        header: t('columns.provider'),
        size: 120,
      },
      {
        accessorKey: 'modelName',
        header: t('columns.model'),
        cell: ({ row }) => (
          <span className="font-mono text-xs text-muted-foreground">
            {row.original.modelName}
          </span>
        ),
      },
      {
        accessorKey: 'apiKeyMasked',
        header: t('columns.apiKey'),
        size: 140,
        cell: ({ row }) => (
          <span className="font-mono text-xs text-muted-foreground">
            {row.original.apiKeyMasked}
          </span>
        ),
      },
      {
        accessorKey: 'active',
        header: t('columns.status'),
        size: 120,
        cell: ({ row }) => (
          <StatusBadge
            status={row.original.active ? 'ACTIVE' : 'INACTIVE'}
            size="sm"
          />
        ),
      },
      {
        accessorKey: 'createdAt',
        header: t('columns.createdAt'),
        size: 140,
        cell: ({ row }) => (
          <span className="text-muted-foreground">
            {formatDate(row.original.createdAt)}
          </span>
        ),
      },
    ],
    [t],
  );

  function updateField<K extends keyof CreateJudgeModelPayload>(
    key: K,
    value: CreateJudgeModelPayload[K],
  ) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function handleProviderChange(provider: JudgeProvider) {
    setForm((current) => ({
      ...current,
      provider,
      modelName: DEFAULT_MODELS[provider] || current.modelName,
      baseUrl: provider === 'CUSTOM' ? current.baseUrl : '',
    }));
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setServerError(null);
    createMutation.mutate(form);
  }

  return (
    <PageShell
      title={t('title')}
      description={t('description')}
      backHref={`/projects/${projectId}`}
      backLabel={tCommon('back')}
    >
      <form
        onSubmit={handleSubmit}
        className="rounded-lg border bg-card p-4"
      >
        <div className="flex items-center gap-2">
          <PlusIcon weight="bold" />
          <h2 className="text-base font-semibold">{t('createJudgeModel')}</h2>
        </div>

        {serverError && (
          <div className="mt-3 rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
            {serverError}
          </div>
        )}

        <div className="mt-4 grid gap-4 lg:grid-cols-2">
          <div className="space-y-2">
            <label htmlFor="judge-name" className="text-sm font-medium">
              {t('fields.name')}
            </label>
            <input
              id="judge-name"
              required
              value={form.name}
              onChange={(event) => updateField('name', event.target.value)}
              className={inputClassName}
            />
          </div>

          <div className="space-y-2">
            <label htmlFor="judge-provider" className="text-sm font-medium">
              {t('fields.provider')}
            </label>
            <select
              id="judge-provider"
              value={form.provider}
              onChange={(event) =>
                handleProviderChange(event.target.value as JudgeProvider)
              }
              className={inputClassName}
            >
              {PROVIDERS.map((provider) => (
                <option key={provider} value={provider}>
                  {provider}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-2">
            <label htmlFor="judge-model-name" className="text-sm font-medium">
              {t('fields.modelName')}
            </label>
            <input
              id="judge-model-name"
              required
              value={form.modelName}
              onChange={(event) => updateField('modelName', event.target.value)}
              className={inputClassName}
            />
          </div>

          <div className="space-y-2">
            <label htmlFor="judge-api-key" className="text-sm font-medium">
              {t('fields.apiKey')}
            </label>
            <input
              id="judge-api-key"
              required
              type="password"
              value={form.apiKey}
              onChange={(event) => updateField('apiKey', event.target.value)}
              className={inputClassName}
            />
          </div>

          <label className="flex items-center gap-2 self-end pb-2 text-sm font-medium">
            <input
              type="checkbox"
              checked={form.active}
              onChange={(event) => updateField('active', event.target.checked)}
              className="size-4 rounded border-input"
            />
            {t('fields.active')}
          </label>

          <div className="lg:col-span-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setAdvancedOpen((current) => !current)}
            >
              {advancedOpen ? t('hideAdvanced') : t('showAdvanced')}
            </Button>
          </div>

          {advancedOpen && (
            <div className="grid gap-4 lg:col-span-2 lg:grid-cols-2">
              <div className="space-y-2">
                <label htmlFor="judge-base-url" className="text-sm font-medium">
                  {t('fields.baseUrl')}
                </label>
                <input
                  id="judge-base-url"
                  value={form.baseUrl}
                  onChange={(event) => updateField('baseUrl', event.target.value)}
                  className={inputClassName}
                  placeholder="https://api.openai.com/v1"
                />
              </div>

              <div className="space-y-2">
                <label htmlFor="judge-config-json" className="text-sm font-medium">
                  {t('fields.configJson')}
                </label>
                <textarea
                  id="judge-config-json"
                  value={form.configJson}
                  onChange={(event) =>
                    updateField('configJson', event.target.value)
                  }
                  className={textareaClassName}
                  placeholder='{"temperature": 0}'
                />
              </div>
            </div>
          )}
        </div>

        <div className="mt-4 flex justify-end">
          <Button type="submit" disabled={createMutation.isPending}>
            {createMutation.isPending ? tCommon('loading') : tCommon('create')}
          </Button>
        </div>
      </form>

      <DataTable
        columns={columns}
        data={judgeModels}
        totalItems={totalItems}
        pageIndex={page}
        pageSize={PAGE_SIZE}
        onPaginationChange={setPage}
        loading={isLoading}
        emptyMessage={t('noJudgeModels')}
      />

      {totalItems > 0 && (
        <DataTablePagination
          pageIndex={page}
          pageSize={PAGE_SIZE}
          totalItems={totalItems}
          totalPages={totalPages}
          onPageChange={setPage}
        />
      )}
    </PageShell>
  );
}
