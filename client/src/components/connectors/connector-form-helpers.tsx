'use client';

import * as React from 'react';
import { MagicWand } from '@phosphor-icons/react';
import { useTranslations } from 'next-intl';

import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

export type ConnectorAuthType = 'NONE' | 'BEARER' | 'API_KEY' | 'BASIC';

export type ParsedCurlConnector = {
  method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  baseUrl: string;
  path: string;
  headers: Record<string, string>;
  queryParams: Record<string, string>;
  bodyType: 'NONE' | 'RAW_JSON' | 'RAW_TEXT';
  bodyTemplate: string;
  responseSelector?: string;
  authType: ConnectorAuthType;
  authConfig: Record<string, string>;
  secretValues: Record<string, string>;
};

export function buildConnectorUrl(baseUrl: string, path: string | undefined): string {
  const trimmedBase = baseUrl.trim().replace(/\/+$/, '');
  const trimmedPath = (path ?? '').trim();
  if (!trimmedPath) return trimmedBase;
  return `${trimmedBase}/${trimmedPath.replace(/^\/+/, '')}`;
}

export function buildConnectorBodyPayload(
  bodyType: string | undefined,
  bodyTemplate: string | undefined,
): { bodyTemplate?: unknown; bodyTemplateText?: string } {
  const raw = (bodyTemplate ?? '').trim();
  if (!bodyType || bodyType === 'NONE' || !raw) {
    return { bodyTemplate: undefined, bodyTemplateText: undefined };
  }

  if (bodyType === 'RAW_JSON') {
    try {
      return { bodyTemplate: JSON.parse(raw), bodyTemplateText: undefined };
    } catch {
      return { bodyTemplate: undefined, bodyTemplateText: raw };
    }
  }

  return { bodyTemplate: undefined, bodyTemplateText: raw };
}

type AuthBuildInput = {
  headers: Record<string, string>;
  queryParams: Record<string, string>;
  pathParams: Record<string, string>;
  authType: ConnectorAuthType | undefined;
  authConfig: Record<string, string>;
  secretValues: Record<string, string>;
};

function compactRecord(record: Record<string, string>): Record<string, string> {
  return Object.fromEntries(
    Object.entries(record)
      .map(([key, value]) => [key.trim(), value.trim()] as const)
      .filter(([key, value]) => key.length > 0 && value.length > 0),
  );
}

function encodeBasicCredentials(username: string, password: string): string {
  if (typeof window === 'undefined') {
    return `${username}:${password}`;
  }
  return window.btoa(`${username}:${password}`);
}

function withRecordValue(
  record: Record<string, string>,
  key: string,
  value: string,
): Record<string, string> {
  const next = { ...record };
  if (value.trim()) {
    next[key] = value;
  } else {
    delete next[key];
  }
  return next;
}

export function buildConnectorAuthPayload({
  headers,
  queryParams,
  pathParams,
  authType,
  authConfig,
  secretValues,
}: AuthBuildInput): {
  headers: Record<string, string>;
  queryParams: Record<string, string>;
  pathParams: Record<string, string>;
  authConfig?: Record<string, string>;
  secretValues?: Record<string, string>;
} {
  const nextHeaders = compactRecord(headers);
  const nextQueryParams = compactRecord(queryParams);
  const nextPathParams = compactRecord(pathParams);
  const nextAuthConfig = compactRecord(authConfig);
  const nextSecrets: Record<string, string> = {};

  if (authType === 'BEARER') {
    const token = secretValues.BEARER_TOKEN?.trim();
    if (token) {
      nextHeaders.Authorization = `Bearer ${token}`;
      nextSecrets.BEARER_TOKEN = token;
    }
  }

  if (authType === 'API_KEY') {
    const name = nextAuthConfig.API_KEY_NAME || 'X-API-Key';
    const location = nextAuthConfig.API_KEY_LOCATION === 'QUERY' ? 'QUERY' : 'HEADER';
    const value = secretValues.API_KEY_VALUE?.trim();

    nextAuthConfig.API_KEY_NAME = name;
    nextAuthConfig.API_KEY_LOCATION = location;

    if (value) {
      if (location === 'QUERY') {
        nextQueryParams[name] = value;
      } else {
        nextHeaders[name] = value;
      }
      nextSecrets.API_KEY_VALUE = value;
    }
  }

  if (authType === 'BASIC') {
    const existingCredential = secretValues.BASIC_CREDENTIALS?.trim();
    const username = secretValues.BASIC_USERNAME?.trim();
    const password = secretValues.BASIC_PASSWORD?.trim();
    const credential =
      existingCredential || (username && password ? encodeBasicCredentials(username, password) : '');

    if (credential) {
      nextHeaders.Authorization = `Basic ${credential}`;
      nextSecrets.BASIC_CREDENTIALS = credential;
    }
  }

  return {
    headers: nextHeaders,
    queryParams: nextQueryParams,
    pathParams: nextPathParams,
    authConfig: authType === 'API_KEY' ? nextAuthConfig : undefined,
    secretValues: Object.keys(nextSecrets).length > 0 ? nextSecrets : undefined,
  };
}

function splitHeader(raw: string): [string, string] | null {
  const idx = raw.indexOf(':');
  if (idx <= 0) return null;
  return [raw.slice(0, idx).trim(), raw.slice(idx + 1).trim()];
}

function tokenizeCurl(input: string): string[] {
  const tokens: string[] = [];
  let current = '';
  let quote: '"' | "'" | null = null;
  let escaped = false;

  for (const char of input) {
    if (escaped) {
      current += char;
      escaped = false;
      continue;
    }

    if (char === '\\' && quote !== "'") {
      escaped = true;
      continue;
    }

    if (quote) {
      if (char === quote) {
        quote = null;
      } else {
        current += char;
      }
      continue;
    }

    if (char === '"' || char === "'") {
      quote = char;
      continue;
    }

    if (/\s/.test(char)) {
      if (current) {
        tokens.push(current);
        current = '';
      }
      continue;
    }

    current += char;
  }

  if (current) tokens.push(current);
  return tokens;
}

function bodyTypeFrom(data: string, headers: Record<string, string>): ParsedCurlConnector['bodyType'] {
  if (!data) return 'NONE';
  const contentType = Object.entries(headers).find(
    ([key]) => key.toLowerCase() === 'content-type',
  )?.[1];

  if (contentType?.toLowerCase().includes('application/json')) return 'RAW_JSON';
  try {
    JSON.parse(data);
    return 'RAW_JSON';
  } catch {
    return 'RAW_TEXT';
  }
}

function parseAuth(
  headers: Record<string, string>,
  basicUser?: string,
): Pick<ParsedCurlConnector, 'authType' | 'authConfig' | 'secretValues' | 'headers'> {
  const restHeaders = { ...headers };
  const authHeader = Object.entries(headers).find(
    ([key]) => key.toLowerCase() === 'authorization',
  );

  if (authHeader) {
    delete restHeaders[authHeader[0]];
    const value = authHeader[1];
    const bearer = value.match(/^Bearer\s+(.+)$/i);
    if (bearer?.[1]) {
      return {
        authType: 'BEARER',
        authConfig: {},
        secretValues: { BEARER_TOKEN: bearer[1].trim() },
        headers: restHeaders,
      };
    }

    const basic = value.match(/^Basic\s+(.+)$/i);
    if (basic?.[1]) {
      return {
        authType: 'BASIC',
        authConfig: {},
        secretValues: { BASIC_CREDENTIALS: basic[1].trim() },
        headers: restHeaders,
      };
    }
  }

  if (basicUser) {
    const [username, password = ''] = basicUser.split(':');
    return {
      authType: 'BASIC',
      authConfig: {},
      secretValues: { BASIC_USERNAME: username, BASIC_PASSWORD: password },
      headers: restHeaders,
    };
  }

  const apiKeyHeader = Object.entries(headers).find(([key]) =>
    ['x-api-key', 'api-key', 'apikey'].includes(key.toLowerCase()),
  );

  if (apiKeyHeader) {
    delete restHeaders[apiKeyHeader[0]];
    return {
      authType: 'API_KEY',
      authConfig: {
        API_KEY_LOCATION: 'HEADER',
        API_KEY_NAME: apiKeyHeader[0],
      },
      secretValues: { API_KEY_VALUE: apiKeyHeader[1] },
      headers: restHeaders,
    };
  }

  return {
    authType: 'NONE',
    authConfig: {},
    secretValues: {},
    headers: restHeaders,
  };
}

function responseSelectorForUrl(url: URL): string | undefined {
  if (
    url.hostname === 'generativelanguage.googleapis.com' &&
    url.pathname.includes(':generateContent')
  ) {
    return '$.candidates[0].content.parts[0].text';
  }

  return undefined;
}

export function parseCurlCommand(input: string): ParsedCurlConnector {
  const tokens = tokenizeCurl(input.trim());
  if (tokens.length === 0 || tokens[0] !== 'curl') {
    throw new Error('Expected a curl command.');
  }

  let method: ParsedCurlConnector['method'] | null = null;
  let url = '';
  let data = '';
  let basicUser: string | undefined;
  const headers: Record<string, string> = {};

  for (let i = 1; i < tokens.length; i += 1) {
    const token = tokens[i];
    const next = tokens[i + 1];

    if ((token === '-X' || token === '--request') && next) {
      method = next.toUpperCase() as ParsedCurlConnector['method'];
      i += 1;
      continue;
    }

    if ((token === '-H' || token === '--header') && next) {
      const header = splitHeader(next);
      if (header) headers[header[0]] = header[1];
      i += 1;
      continue;
    }

    if (
      ['-d', '--data', '--data-raw', '--data-binary', '--data-urlencode'].includes(token) &&
      next
    ) {
      data = data ? `${data}&${next}` : next;
      if (!method) method = 'POST';
      i += 1;
      continue;
    }

    if ((token === '-u' || token === '--user') && next) {
      basicUser = next;
      i += 1;
      continue;
    }

    if (!token.startsWith('-') && /^https?:\/\//i.test(token)) {
      url = token;
    }
  }

  if (!url) {
    throw new Error('Could not find a request URL.');
  }

  const parsedUrl = new URL(url);
  const queryParams: Record<string, string> = {};
  parsedUrl.searchParams.forEach((value, key) => {
    queryParams[key] = value;
  });

  const auth = parseAuth(headers, basicUser);

  return {
    method: method ?? 'GET',
    baseUrl: parsedUrl.origin,
    path: parsedUrl.pathname === '/' ? '' : parsedUrl.pathname,
    headers: auth.headers,
    queryParams,
    bodyType: bodyTypeFrom(data, headers),
    bodyTemplate: data,
    responseSelector: responseSelectorForUrl(parsedUrl),
    authType: auth.authType,
    authConfig: auth.authConfig,
    secretValues: auth.secretValues,
  };
}

interface CurlImportPanelProps {
  disabled?: boolean;
  onApply: (parsed: ParsedCurlConnector) => void;
}

export function CurlImportPanel({ disabled = false, onApply }: CurlImportPanelProps) {
  const t = useTranslations('connectors');
  const [raw, setRaw] = React.useState('');
  const [error, setError] = React.useState<string | null>(null);

  function handleApply() {
    setError(null);
    try {
      onApply(parseCurlCommand(raw));
    } catch {
      setError(t('curl.invalid'));
    }
  }

  return (
    <section className="space-y-3 rounded-lg border bg-card p-4">
      <div className="space-y-1">
        <h2 className="text-base font-semibold tracking-tight">{t('curl.title')}</h2>
        <p className="text-sm text-muted-foreground">{t('curl.description')}</p>
      </div>
      <textarea
        value={raw}
        disabled={disabled}
        onChange={(event) => setRaw(event.target.value)}
        rows={4}
        placeholder={t('curl.placeholder')}
        className="flex min-h-[96px] w-full resize-y rounded-md border border-input bg-background px-3 py-2 font-mono text-xs ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
      />
      {error && <p className="text-sm text-destructive">{error}</p>}
      <Button type="button" variant="outline" disabled={disabled || !raw.trim()} onClick={handleApply}>
        <MagicWand weight="bold" />
        {t('curl.apply')}
      </Button>
    </section>
  );
}

interface AuthSettingsFieldsProps {
  authType: ConnectorAuthType | undefined;
  authConfig: Record<string, string>;
  secretValues: Record<string, string>;
  onAuthConfigChange: (next: Record<string, string>) => void;
  onSecretValuesChange: (next: Record<string, string>) => void;
  inputClassName: string;
  selectClassName: string;
  disabled?: boolean;
}

export function AuthSettingsFields({
  authType,
  authConfig,
  secretValues,
  onAuthConfigChange,
  onSecretValuesChange,
  inputClassName,
  selectClassName,
  disabled = false,
}: AuthSettingsFieldsProps) {
  const t = useTranslations('connectors');

  if (!authType || authType === 'NONE') {
    return <p className="text-sm text-muted-foreground">{t('auth.noneHelp')}</p>;
  }

  if (authType === 'BEARER') {
    return (
      <LabeledSecretInput
        id="auth-bearer-token"
        label={t('auth.bearerToken')}
        value={secretValues.BEARER_TOKEN ?? ''}
        onChange={(value) =>
          onSecretValuesChange(withRecordValue(secretValues, 'BEARER_TOKEN', value))
        }
        inputClassName={inputClassName}
        disabled={disabled}
      />
    );
  }

  if (authType === 'API_KEY') {
    return (
      <div className="grid gap-4 sm:grid-cols-3">
        <div className="space-y-2">
          <label htmlFor="auth-api-key-location" className="text-sm font-medium leading-none">
            {t('auth.apiKeyLocation')}
          </label>
          <select
            id="auth-api-key-location"
            disabled={disabled}
            value={authConfig.API_KEY_LOCATION ?? 'HEADER'}
            onChange={(event) =>
              onAuthConfigChange(
                withRecordValue(authConfig, 'API_KEY_LOCATION', event.target.value),
              )
            }
            className={selectClassName}
          >
            <option value="HEADER">{t('auth.apiKeyHeader')}</option>
            <option value="QUERY">{t('auth.apiKeyQuery')}</option>
          </select>
        </div>
        <div className="space-y-2">
          <label htmlFor="auth-api-key-name" className="text-sm font-medium leading-none">
            {t('auth.apiKeyName')}
          </label>
          <input
            id="auth-api-key-name"
            type="text"
            disabled={disabled}
            value={authConfig.API_KEY_NAME ?? 'X-API-Key'}
            onChange={(event) =>
              onAuthConfigChange(withRecordValue(authConfig, 'API_KEY_NAME', event.target.value))
            }
            className={inputClassName}
          />
        </div>
        <LabeledSecretInput
          id="auth-api-key-value"
          label={t('auth.apiKeyValue')}
          value={secretValues.API_KEY_VALUE ?? ''}
          onChange={(value) =>
            onSecretValuesChange(withRecordValue(secretValues, 'API_KEY_VALUE', value))
          }
          inputClassName={inputClassName}
          disabled={disabled}
        />
      </div>
    );
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <LabeledSecretInput
        id="auth-basic-username"
        label={t('auth.basicUsername')}
        value={secretValues.BASIC_USERNAME ?? ''}
        onChange={(value) =>
          onSecretValuesChange(withRecordValue(secretValues, 'BASIC_USERNAME', value))
        }
        inputClassName={inputClassName}
        disabled={disabled}
        type="text"
      />
      <LabeledSecretInput
        id="auth-basic-password"
        label={t('auth.basicPassword')}
        value={secretValues.BASIC_PASSWORD ?? ''}
        onChange={(value) =>
          onSecretValuesChange(withRecordValue(secretValues, 'BASIC_PASSWORD', value))
        }
        inputClassName={inputClassName}
        disabled={disabled}
      />
    </div>
  );
}

function LabeledSecretInput({
  id,
  label,
  value,
  onChange,
  inputClassName,
  disabled,
  type = 'password',
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  inputClassName: string;
  disabled: boolean;
  type?: 'password' | 'text';
}) {
  return (
    <div className="space-y-2">
      <label htmlFor={id} className="text-sm font-medium leading-none">
        {label}
      </label>
      <input
        id={id}
        type={type}
        disabled={disabled}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className={cn(inputClassName, 'font-mono')}
      />
    </div>
  );
}
