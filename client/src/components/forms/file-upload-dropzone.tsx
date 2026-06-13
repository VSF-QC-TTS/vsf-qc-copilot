"use client";

import * as React from "react";
import { UploadSimple } from "@phosphor-icons/react";

import { cn } from "@/lib/utils";

/* ------------------------------------------------------------------ */
/*  Defaults                                                           */
/* ------------------------------------------------------------------ */

const DEFAULT_ACCEPT: Record<string, string[]> = {
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": [".xlsx"],
  "text/csv": [".csv"],
};

const DEFAULT_MAX_SIZE = 5_242_880; // 5 MB

/* ------------------------------------------------------------------ */
/*  Helpers                                                            */
/* ------------------------------------------------------------------ */

function humanSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1_048_576) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1_048_576).toFixed(1)} MB`;
}

/** Check if a File matches the accept map. */
function isAccepted(
  file: File,
  accept: Record<string, string[]>,
): boolean {
  const entries = Object.entries(accept);
  if (entries.length === 0) return true;

  for (const [mime, exts] of entries) {
    if (file.type === mime) return true;
    const name = file.name.toLowerCase();
    if (exts.some((ext) => name.endsWith(ext.toLowerCase()))) return true;
  }
  return false;
}

/** Flatten accept map to the `accept` attribute value for <input>. */
function toInputAccept(accept: Record<string, string[]>): string {
  const parts: string[] = [];
  for (const [mime, exts] of Object.entries(accept)) {
    parts.push(mime, ...exts);
  }
  return parts.join(",");
}

/* ------------------------------------------------------------------ */
/*  Component                                                          */
/* ------------------------------------------------------------------ */

export interface FileUploadDropzoneProps {
  /** MIME → extensions map. */
  accept?: Record<string, string[]>;
  /** Max file size in bytes. @default 5_242_880 */
  maxSizeBytes?: number;
  /** Called when a valid file is selected. */
  onFileSelect: (file: File) => void;
  disabled?: boolean;
  /** Label shown inside the zone. */
  label?: string;
  /** Helper description shown below the label. */
  description?: string;
  className?: string;
}

export function FileUploadDropzone({
  accept = DEFAULT_ACCEPT,
  maxSizeBytes = DEFAULT_MAX_SIZE,
  onFileSelect,
  disabled = false,
  label = "Drop file here or click to browse",
  description,
  className,
}: FileUploadDropzoneProps) {
  const inputRef = React.useRef<HTMLInputElement>(null);
  const [dragOver, setDragOver] = React.useState(false);
  const [selectedName, setSelectedName] = React.useState<string | null>(null);
  const [error, setError] = React.useState<string | null>(null);

  /* ---- Validate & propagate ---- */
  const handleFile = React.useCallback(
    (file: File) => {
      setError(null);
      setSelectedName(null);

      if (!isAccepted(file, accept)) {
        const allowed = Object.values(accept).flat().join(", ");
        setError(`Invalid file type. Accepted: ${allowed}`);
        return;
      }

      if (file.size > maxSizeBytes) {
        setError(`File too large. Max size: ${humanSize(maxSizeBytes)}`);
        return;
      }

      setSelectedName(file.name);
      onFileSelect(file);
    },
    [accept, maxSizeBytes, onFileSelect],
  );

  /* ---- Drag handlers ---- */
  const onDragOver = React.useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      if (!disabled) setDragOver(true);
    },
    [disabled],
  );

  const onDragLeave = React.useCallback(() => setDragOver(false), []);

  const onDrop = React.useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      setDragOver(false);
      if (disabled) return;

      const file = e.dataTransfer.files[0];
      if (file) handleFile(file);
    },
    [disabled, handleFile],
  );

  /* ---- Click / input change ---- */
  const onInputChange = React.useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0];
      if (file) handleFile(file);
      // Reset so the same file can be re-selected.
      e.target.value = "";
    },
    [handleFile],
  );

  const extensions = Object.values(accept).flat().join(", ");
  const defaultDesc =
    description ??
    `Accepted: ${extensions} (max ${humanSize(maxSizeBytes)})`;

  return (
    <div data-slot="file-upload-dropzone" className={cn("space-y-1.5", className)}>
      <button
        type="button"
        disabled={disabled}
        onClick={() => inputRef.current?.click()}
        onDragOver={onDragOver}
        onDragLeave={onDragLeave}
        onDrop={onDrop}
        className={cn(
          "flex w-full flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed px-6 py-10 text-center transition-colors",
          "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
          dragOver
            ? "border-primary bg-primary/5"
            : "border-input hover:border-primary/50",
          disabled && "pointer-events-none opacity-50",
        )}
      >
        <UploadSimple
          size={32}
          weight="duotone"
          className="text-muted-foreground"
        />

        <div>
          <p className="text-sm font-medium text-foreground">{label}</p>
          <p className="mt-0.5 text-xs text-muted-foreground">{defaultDesc}</p>
        </div>

        {selectedName && !error && (
          <p className="text-xs font-medium text-primary">{selectedName}</p>
        )}
      </button>

      {/* Hidden native input */}
      <input
        ref={inputRef}
        type="file"
        className="sr-only"
        accept={toInputAccept(accept)}
        onChange={onInputChange}
        disabled={disabled}
        tabIndex={-1}
        aria-hidden="true"
      />

      {/* Inline error */}
      {error && (
        <p className="text-xs text-destructive" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
