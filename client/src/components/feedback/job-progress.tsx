"use client";

import { cn } from "@/lib/utils";
import {
  CircleNotch,
  CheckCircle,
  XCircle,
  Clock,
  Prohibit,
} from "@phosphor-icons/react";
import type { JobStatus } from "@/lib/api/types";

type JobProgressProps = {
  status: JobStatus | null;
  progress?: number | null;
  errorMessage?: string | null;
  className?: string;
  /** Labels for each state — pass i18n translated strings */
  labels?: {
    pending?: string;
    running?: string;
    completed?: string;
    failed?: string;
    cancelled?: string;
  };
};

const defaultLabels = {
  pending: "Waiting to start...",
  running: "Processing...",
  completed: "Completed",
  failed: "Failed",
  cancelled: "Cancelled",
};

/**
 * Shared job progress display — shows status icon, text, optional progress bar,
 * and error message for terminal failure states.
 */
export function JobProgress({
  status,
  progress,
  errorMessage,
  className,
  labels: userLabels,
}: JobProgressProps) {
  const labels = { ...defaultLabels, ...userLabels };

  if (!status) return null;

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <div className="flex items-center gap-2">
        <StatusIcon status={status} />
        <span className="text-sm font-medium">{getLabel(status, labels)}</span>
      </div>

      {/* Progress bar for running state */}
      {status === "RUNNING" && progress != null && (
        <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
          <div
            className="h-full rounded-full bg-primary transition-all duration-500 ease-out"
            style={{ width: `${Math.min(Math.max(progress, 0), 100)}%` }}
          />
        </div>
      )}

      {/* Error message for failed state */}
      {(status === "FAILED" || status === "CANCELLED") && errorMessage && (
        <p className="text-xs text-destructive">{errorMessage}</p>
      )}
    </div>
  );
}

function StatusIcon({ status }: { status: JobStatus }) {
  switch (status) {
    case "PENDING":
      return <Clock size={18} className="text-muted-foreground" />;
    case "RUNNING":
      return (
        <CircleNotch size={18} className="animate-spin text-primary" />
      );
    case "COMPLETED":
      return (
        <CheckCircle size={18} weight="fill" className="text-emerald-500" />
      );
    case "FAILED":
      return <XCircle size={18} weight="fill" className="text-destructive" />;
    case "CANCELLED":
      return <Prohibit size={18} className="text-muted-foreground" />;
    default:
      return <Clock size={18} className="text-muted-foreground" />;
  }
}

function getLabel(
  status: JobStatus,
  labels: typeof defaultLabels
): string {
  switch (status) {
    case "PENDING":
      return labels.pending;
    case "RUNNING":
      return labels.running;
    case "COMPLETED":
      return labels.completed;
    case "FAILED":
      return labels.failed;
    case "CANCELLED":
      return labels.cancelled;
    default:
      return status;
  }
}
