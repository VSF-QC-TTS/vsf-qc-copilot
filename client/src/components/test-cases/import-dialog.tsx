'use client';

import * as React from 'react';
import { useTranslations } from 'next-intl';
import { useQueryClient } from '@tanstack/react-query';
import { UploadSimple, File, X, CheckCircle, Warning } from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { useAuthStore } from '@/lib/store/auth-store';

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? '';
const ACCEPTED_TYPES = [
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'text/csv',
  'application/vnd.ms-excel',
];
const ACCEPTED_EXTENSIONS = '.xlsx,.csv';
const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type ImportResult = {
  importedCount: number;
  skippedCount: number;
  errors?: { row: number; message: string }[];
};

interface ImportDialogProps {
  datasetId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function ImportDialog({
  datasetId,
  open,
  onOpenChange,
}: ImportDialogProps) {
  const t = useTranslations('testCases');
  const tCommon = useTranslations('common');
  const queryClient = useQueryClient();

  const [file, setFile] = React.useState<File | null>(null);
  const [dragging, setDragging] = React.useState(false);
  const [uploading, setUploading] = React.useState(false);
  const [result, setResult] = React.useState<ImportResult | null>(null);
  const [error, setError] = React.useState<string | null>(null);

  const fileInputRef = React.useRef<HTMLInputElement>(null);

  /* ---- Close helper ---- */
  const handleClose = React.useCallback(
    (nextOpen: boolean) => {
      if (!nextOpen) {
        setFile(null);
        setResult(null);
        setError(null);
        setDragging(false);
      }
      onOpenChange(nextOpen);
    },
    [onOpenChange],
  );

  /* ---- Escape key ---- */
  React.useEffect(() => {
    if (!open) return;

    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        e.stopPropagation();
        handleClose(false);
      }
    }

    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [open, handleClose]);

  /* ---- Lock body scroll ---- */
  React.useEffect(() => {
    if (!open) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prev;
    };
  }, [open]);

  /* ---- File validation ---- */
  function validateFile(f: File): string | null {
    if (!ACCEPTED_TYPES.includes(f.type) && !f.name.match(/\.(xlsx|csv)$/i)) {
      return t('importInvalidType');
    }
    if (f.size > MAX_FILE_SIZE) {
      return t('importFileTooLarge');
    }
    return null;
  }

  function handleFileSelect(f: File) {
    const validationError = validateFile(f);
    if (validationError) {
      setError(validationError);
      setFile(null);
      return;
    }
    setError(null);
    setResult(null);
    setFile(f);
  }

  /* ---- Drag & drop ---- */
  function handleDragOver(e: React.DragEvent) {
    e.preventDefault();
    e.stopPropagation();
    setDragging(true);
  }

  function handleDragLeave(e: React.DragEvent) {
    e.preventDefault();
    e.stopPropagation();
    setDragging(false);
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault();
    e.stopPropagation();
    setDragging(false);

    const droppedFile = e.dataTransfer.files[0];
    if (droppedFile) {
      handleFileSelect(droppedFile);
    }
  }

  /* ---- Upload ---- */
  async function handleUpload() {
    if (!file) return;
    setUploading(true);
    setError(null);

    try {
      const token = useAuthStore.getState().accessToken;
      const formData = new FormData();
      formData.append('file', file);

      const res = await fetch(
        `${BASE_URL}/api/v1/datasets/${datasetId}/test-cases/import`,
        {
          method: 'POST',
          headers: { Authorization: `Bearer ${token}` },
          credentials: 'include',
          body: formData,
        },
      );

      if (!res.ok) {
        const body = await res.json().catch(() => null);
        throw new Error(body?.message ?? res.statusText);
      }

      const data: ImportResult = await res.json();
      setResult(data);

      await queryClient.invalidateQueries({
        queryKey: ['test-cases', datasetId],
      });
    } catch (err: unknown) {
      setError(
        err instanceof Error ? err.message : t('importUploadError'),
      );
    } finally {
      setUploading(false);
    }
  }

  if (!open) return null;

  return (
    <div
      data-slot="import-dialog"
      className="fixed inset-0 z-50 flex items-center justify-center"
    >
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={() => !uploading && handleClose(false)}
        aria-hidden="true"
      />

      {/* Card */}
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="import-dialog-title"
        className={cn(
          'relative z-10 w-full max-w-md rounded-lg border bg-card p-6 shadow-lg',
          'animate-in fade-in-0 zoom-in-95',
        )}
      >
        <h2
          id="import-dialog-title"
          className="text-lg font-semibold text-card-foreground"
        >
          {t('importTitle')}
        </h2>

        <div className="mt-4 space-y-4">
          {/* Error banner */}
          {error && (
            <div className="rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
              {error}
            </div>
          )}

          {/* Result banner */}
          {result && (
            <div className="space-y-2">
              <div className="flex items-center gap-2 rounded-md border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800 dark:border-emerald-800 dark:bg-emerald-950 dark:text-emerald-300">
                <CheckCircle className="size-4 shrink-0" />
                {t('importSuccess', {
                  imported: result.importedCount,
                  skipped: result.skippedCount,
                })}
              </div>

              {/* Row-level errors */}
              {result.errors && result.errors.length > 0 && (
                <div className="max-h-40 overflow-y-auto rounded-md border bg-muted/50 p-3">
                  <p className="mb-1 text-xs font-medium text-muted-foreground">
                    {t('importRowErrors')}
                  </p>
                  <ul className="space-y-1 text-xs text-destructive">
                    {result.errors.map((err, i) => (
                      <li key={i} className="flex items-start gap-1.5">
                        <Warning className="mt-0.5 size-3 shrink-0" />
                        {t('importRowError', {
                          row: err.row,
                          message: err.message,
                        })}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}

          {/* Dropzone (hide after result) */}
          {!result && (
            <>
              <div
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                onClick={() => fileInputRef.current?.click()}
                className={cn(
                  'flex cursor-pointer flex-col items-center justify-center gap-2 rounded-md border-2 border-dashed px-4 py-8 transition-colors',
                  dragging
                    ? 'border-primary bg-primary/5'
                    : 'border-muted-foreground/25 hover:border-primary/50',
                )}
              >
                <UploadSimple className="size-8 text-muted-foreground" />
                <p className="text-sm text-muted-foreground">
                  {t('importDropzone')}
                </p>
                <p className="text-xs text-muted-foreground/70">
                  {t('importAcceptedFormats')}
                </p>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept={ACCEPTED_EXTENSIONS}
                  className="hidden"
                  onChange={(e) => {
                    const f = e.target.files?.[0];
                    if (f) handleFileSelect(f);
                  }}
                />
              </div>

              {/* Selected file */}
              {file && (
                <div className="flex items-center gap-2 rounded-md border bg-muted/50 px-3 py-2">
                  <File className="size-4 text-muted-foreground" />
                  <span className="flex-1 truncate text-sm">{file.name}</span>
                  <span className="text-xs text-muted-foreground">
                    {(file.size / 1024).toFixed(1)} KB
                  </span>
                  <button
                    type="button"
                    onClick={() => setFile(null)}
                    className="rounded p-0.5 text-muted-foreground hover:text-foreground"
                  >
                    <X className="size-3.5" />
                  </button>
                </div>
              )}
            </>
          )}

          {/* Actions */}
          <div className="flex justify-end gap-3 pt-2">
            <Button
              type="button"
              variant="outline"
              disabled={uploading}
              onClick={() => handleClose(false)}
            >
              {result ? tCommon('close') : tCommon('cancel')}
            </Button>
            {!result && (
              <Button
                type="button"
                disabled={!file || uploading}
                onClick={handleUpload}
              >
                {uploading ? t('importUploading') : t('importUpload')}
              </Button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
