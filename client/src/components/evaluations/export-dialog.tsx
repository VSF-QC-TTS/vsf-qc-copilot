'use client';

import * as React from 'react';
import { useTranslations } from 'next-intl';
import { useMutation } from '@tanstack/react-query';
import { DownloadSimpleIcon, XIcon } from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { apiClient } from '@/lib/api/client';
import { useJobProgress } from '@/hooks/use-job-progress';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type FileType = 'EXCEL' | 'JSON';

type StartExportResponse = {
  exportPublicId: string;
  jobPublicId: string;
  status: string;
  message: string;
};

type ExportDetailResponse = {
  publicId: string;
  status: string;
  fileType: string;
  downloadUrl: string | null;
  errorMessage: string | null;
  createdAt: string;
};

interface ExportDialogProps {
  runPublicId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function ExportDialog({
  runPublicId,
  open,
  onOpenChange,
}: ExportDialogProps) {
  const t = useTranslations('exports');
  const cancelRef = React.useRef<HTMLButtonElement>(null);

  const [fileType, setFileType] = React.useState<FileType>('EXCEL');
  const [jobPublicId, setJobPublicId] = React.useState<string | null>(null);
  const [exportPublicId, setExportPublicId] = React.useState<string | null>(
    null,
  );
  const [downloadUrl, setDownloadUrl] = React.useState<string | null>(null);
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);

  // Start export mutation
  const startExportMutation = useMutation({
    mutationFn: (ft: FileType) =>
      apiClient.post<StartExportResponse>(
        `/api/v1/evaluation-runs/${runPublicId}/exports`,
        { fileType: ft },
      ),
    onSuccess: (res) => {
      setExportPublicId(res.exportPublicId);
      setJobPublicId(res.jobPublicId);
      setErrorMessage(null);
    },
    onError: (err: unknown) => {
      const msg =
        err instanceof Object && 'message' in err
          ? (err as { message: string }).message
          : 'Export failed';
      setErrorMessage(msg);
    },
  });

  // Poll job progress
  const { isPolling, isCompleted, isFailed, job } = useJobProgress(
    jobPublicId,
    {
      onCompleted: async () => {
        if (!exportPublicId) return;
        try {
          const detail = await apiClient.get<ExportDetailResponse>(
            `/api/v1/exports/${exportPublicId}`,
          );
          if (detail.downloadUrl) {
            setDownloadUrl(detail.downloadUrl);
          } else {
            // Fallback to file endpoint
            setDownloadUrl(`/api/v1/exports/${exportPublicId}/file`);
          }
        } catch {
          setErrorMessage('Failed to fetch export details');
        }
      },
      onFailed: (failedJob) => {
        setErrorMessage(
          failedJob.errorMessage ?? 'Export job failed',
        );
      },
    },
  );

  const isSubmitting = startExportMutation.isPending || isPolling;

  // Reset on close
  React.useEffect(() => {
    if (open) return;

    let cancelled = false;
    queueMicrotask(() => {
      if (cancelled) return;

      setFileType('EXCEL');
      setJobPublicId(null);
      setExportPublicId(null);
      setDownloadUrl(null);
      setErrorMessage(null);
      startExportMutation.reset();
    });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  // Focus cancel on open
  React.useEffect(() => {
    if (open) {
      const raf = requestAnimationFrame(() => cancelRef.current?.focus());
      return () => cancelAnimationFrame(raf);
    }
  }, [open]);

  // Escape key
  React.useEffect(() => {
    if (!open) return;
    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        e.stopPropagation();
        onOpenChange(false);
      }
    }
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [open, onOpenChange]);

  // Lock body scroll
  React.useEffect(() => {
    if (!open) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prev;
    };
  }, [open]);

  const handleStartExport = () => {
    setErrorMessage(null);
    setDownloadUrl(null);
    startExportMutation.mutate(fileType);
  };

  if (!open) return null;

  return (
    <div
      data-slot="export-dialog"
      className="fixed inset-0 z-50 flex items-center justify-center"
    >
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={() => !isSubmitting && onOpenChange(false)}
        aria-hidden="true"
      />

      {/* Card */}
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="export-dialog-title"
        className={cn(
          'relative z-10 w-full max-w-md rounded-lg border bg-card p-6 shadow-lg',
          'animate-in fade-in-0 zoom-in-95',
        )}
      >
        {/* Header */}
        <div className="flex items-center justify-between">
          <h2
            id="export-dialog-title"
            className="text-lg font-semibold text-card-foreground"
          >
            {t('exportDialog')}
          </h2>
          <Button
            variant="ghost"
            size="icon"
            className="size-8"
            onClick={() => onOpenChange(false)}
            disabled={isSubmitting}
          >
            <XIcon className="size-4" />
          </Button>
        </div>

        {/* Error */}
        {errorMessage && (
          <div className="mt-3 rounded-md bg-red-50 p-3 text-sm text-red-800 dark:bg-red-950 dark:text-red-300">
            {errorMessage}
          </div>
        )}

        {/* Download ready */}
        {isCompleted && downloadUrl && (
          <div className="mt-4 space-y-3">
            <p className="text-sm text-green-700 dark:text-green-400 font-medium">
              {t('exportReady')}
            </p>
            <a
              href={downloadUrl}
              download
              className={cn(
                'inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground',
                'hover:bg-primary/90 transition-colors',
              )}
            >
              <DownloadSimpleIcon className="size-4" weight="bold" />
              {t('download')}
            </a>
          </div>
        )}

        {/* Failed state */}
        {isFailed && !errorMessage && (
          <div className="mt-4">
            <p className="text-sm text-red-600 dark:text-red-400 font-medium">
              {t('exportFailed')}
            </p>
            {job?.errorMessage && (
              <p className="mt-1 text-xs text-muted-foreground">
                {job.errorMessage}
              </p>
            )}
          </div>
        )}

        {/* Polling state */}
        {isPolling && (
          <div className="mt-4 space-y-2">
            <p className="text-sm text-muted-foreground animate-pulse">
              {t('exporting')}
            </p>
            {job?.progress !== null && job?.progress !== undefined && (
              <div className="h-2 w-full rounded-full bg-muted overflow-hidden">
                <div
                  className="h-full rounded-full bg-primary transition-all"
                  style={{ width: `${job.progress}%` }}
                />
              </div>
            )}
          </div>
        )}

        {/* Initial form */}
        {!jobPublicId && !isCompleted && !isFailed && (
          <div className="mt-4 space-y-4">
            {/* File type selection */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium">{t('fileType')}</label>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setFileType('EXCEL')}
                  className={cn(
                    'flex-1 rounded-md border px-3 py-2 text-sm transition-colors',
                    fileType === 'EXCEL'
                      ? 'border-primary bg-primary/10 text-primary font-medium'
                      : 'border-input bg-background text-muted-foreground hover:bg-muted',
                  )}
                >
                  {t('excel')}
                </button>
                <button
                  type="button"
                  onClick={() => setFileType('JSON')}
                  className={cn(
                    'flex-1 rounded-md border px-3 py-2 text-sm transition-colors',
                    fileType === 'JSON'
                      ? 'border-primary bg-primary/10 text-primary font-medium'
                      : 'border-input bg-background text-muted-foreground hover:bg-muted',
                  )}
                >
                  {t('json')}
                </button>
              </div>
            </div>

            {/* Actions */}
            <div className="flex items-center justify-end gap-3 pt-2">
              <Button
                ref={cancelRef}
                type="button"
                variant="outline"
                disabled={startExportMutation.isPending}
                onClick={() => onOpenChange(false)}
              >
                {t('title')}
              </Button>
              <Button
                type="button"
                disabled={startExportMutation.isPending}
                onClick={handleStartExport}
              >
                {startExportMutation.isPending && (
                  <svg
                    className="mr-1.5 size-4 animate-spin"
                    viewBox="0 0 24 24"
                    fill="none"
                    aria-hidden="true"
                  >
                    <circle
                      className="opacity-25"
                      cx="12"
                      cy="12"
                      r="10"
                      stroke="currentColor"
                      strokeWidth="4"
                    />
                    <path
                      className="opacity-75"
                      fill="currentColor"
                      d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
                    />
                  </svg>
                )}
                {t('startExport')}
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
