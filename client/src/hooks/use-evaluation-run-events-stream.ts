'use client';

import * as React from 'react';

import { apiBaseUrl } from '@/lib/api/client';
import { useAuthStore } from '@/lib/store/auth-store';
import type { JobEventResponse } from '@/lib/api/types';

type StreamStatus = 'idle' | 'connecting' | 'open' | 'reconnecting' | 'closed' | 'error';

type UseEvaluationRunEventsStreamOptions = {
  enabled?: boolean;
  onTerminal?: (event: JobEventResponse) => void;
};

type ParsedSseEvent = {
  event: string;
  id: string | null;
  data: string;
};

const TERMINAL_EVENT_TYPES = new Set(['COMPLETED', 'FAILED', 'CANCELLED']);
const MAX_RECONNECT_ATTEMPTS = 2;
const RECONNECT_DELAY_MS = 1_500;

export function useEvaluationRunEventsStream(
  runPublicId: string | null,
  options: UseEvaluationRunEventsStreamOptions = {},
) {
  const { enabled = true, onTerminal } = options;
  const accessToken = useAuthStore((state) => state.accessToken);
  const [eventState, setEventState] = React.useState<{
    runPublicId: string | null;
    items: JobEventResponse[];
  }>({ runPublicId: null, items: [] });
  const [status, setStatus] = React.useState<StreamStatus>('idle');
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    if (!enabled || !runPublicId) {
      return;
    }

    if (!accessToken) {
      return;
    }

    const abortController = new AbortController();
    let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
    let reconnectAttempts = 0;
    let stopped = false;

    const appendEvent = (event: JobEventResponse) => {
      setEventState((current) => {
        const currentItems =
          current.runPublicId === runPublicId ? current.items : [];
        if (currentItems.some((item) => item.publicId === event.publicId)) {
          return current;
        }
        return { runPublicId, items: [...currentItems, event] };
      });
      if (TERMINAL_EVENT_TYPES.has(event.eventType)) {
        onTerminal?.(event);
      }
    };

    const connect = async () => {
      setStatus(reconnectAttempts > 0 ? 'reconnecting' : 'connecting');
      setError(null);

      try {
        const response = await fetch(
          `${apiBaseUrl}/api/v1/evaluation-runs/${runPublicId}/events/stream`,
          {
            headers: {
              Accept: 'text/event-stream',
              Authorization: `Bearer ${accessToken}`,
            },
            credentials: 'include',
            signal: abortController.signal,
          },
        );

        if (!response.ok) {
          throw new Error(`Event stream failed with HTTP ${response.status}`);
        }
        if (!response.body) {
          throw new Error('Event stream response has no body.');
        }

        setStatus('open');
        await readEventStream(response.body, appendEvent);

        if (!stopped) {
          setStatus('closed');
        }
      } catch (streamError) {
        if (abortController.signal.aborted || stopped) {
          return;
        }

        const message =
          streamError instanceof Error
            ? streamError.message
            : 'Event stream disconnected.';

        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
          reconnectAttempts += 1;
          setStatus('reconnecting');
          setError(message);
          reconnectTimer = setTimeout(() => {
            void connect();
          }, RECONNECT_DELAY_MS);
          return;
        }

        setStatus('error');
        setError(message);
      }
    };

    void connect();

    return () => {
      stopped = true;
      if (reconnectTimer) {
        clearTimeout(reconnectTimer);
      }
      abortController.abort();
    };
  }, [accessToken, enabled, onTerminal, runPublicId]);

  const resolvedStatus: StreamStatus =
    !enabled || !runPublicId ? 'idle' : !accessToken ? 'error' : status;
  const resolvedError =
    enabled && runPublicId && !accessToken ? 'Missing access token.' : error;
  const events =
    eventState.runPublicId === runPublicId ? eventState.items : [];

  return {
    events,
    status: resolvedStatus,
    error: resolvedError,
    isStreaming:
      resolvedStatus === 'connecting' ||
      resolvedStatus === 'open' ||
      resolvedStatus === 'reconnecting',
    isFallbackRecommended: resolvedStatus === 'error',
  };
}

async function readEventStream(
  body: ReadableStream<Uint8Array>,
  onEvent: (event: JobEventResponse) => void,
) {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) {
      break;
    }

    buffer += decoder.decode(value, { stream: true });
    const chunks = buffer.split(/\r?\n\r?\n/);
    buffer = chunks.pop() ?? '';

    for (const chunk of chunks) {
      const parsed = parseSseChunk(chunk);
      if (parsed?.event !== 'job-event' || !parsed.data) {
        continue;
      }

      const event = parseJobEvent(parsed.data);
      if (event) {
        onEvent(event);
      }
    }
  }
}

function parseSseChunk(chunk: string): ParsedSseEvent | null {
  const lines = chunk.split(/\r?\n/);
  const dataLines: string[] = [];
  let event = 'message';
  let id: string | null = null;

  for (const line of lines) {
    if (!line || line.startsWith(':')) {
      continue;
    }
    if (line.startsWith('event:')) {
      event = line.slice('event:'.length).trim();
      continue;
    }
    if (line.startsWith('id:')) {
      id = line.slice('id:'.length).trim();
      continue;
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice('data:'.length).trimStart());
    }
  }

  if (dataLines.length === 0) {
    return null;
  }
  return { event, id, data: dataLines.join('\n') };
}

function parseJobEvent(raw: string): JobEventResponse | null {
  try {
    const value: unknown = JSON.parse(raw);
    if (!isJobEventResponse(value)) {
      return null;
    }
    return value;
  } catch {
    return null;
  }
}

function isJobEventResponse(value: unknown): value is JobEventResponse {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const record = value as Record<string, unknown>;
  return (
    typeof record.publicId === 'string' &&
    typeof record.eventType === 'string' &&
    (typeof record.payloadJson === 'string' || record.payloadJson === null) &&
    typeof record.createdAt === 'string'
  );
}
