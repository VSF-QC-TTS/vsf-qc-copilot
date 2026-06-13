import * as React from "react";

import { cn } from "@/lib/utils";

/* ------------------------------------------------------------------ */
/*  Status → visual-category mapping                                   */
/* ------------------------------------------------------------------ */

type StatusCategory = "success" | "destructive" | "warning" | "info" | "neutral";

const STATUS_MAP: Record<string, StatusCategory> = {
  // Green – success
  PASS: "success",
  COMPLETED: "success",
  ACTIVE: "success",
  APPROVED: "success",
  PUBLISHED: "success",

  // Red – destructive
  FAIL: "destructive",
  FAILED: "destructive",
  ERROR: "destructive",

  // Yellow / amber – warning
  WARNING: "warning",
  NEED_FIX: "warning",
  PENDING: "warning",
  PENDING_EMAIL_VERIFICATION: "warning",

  // Blue – info
  RUNNING: "info",
  DRAFT: "info",

  // Gray – neutral
  NOT_REVIEWED: "neutral",
  INACTIVE: "neutral",
  ARCHIVED: "neutral",
  CANCELLED: "neutral",
  IGNORED: "neutral",
  DISABLED: "neutral",
};

const CATEGORY_CLASSES: Record<StatusCategory, string> = {
  success:
    "bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300",
  destructive:
    "bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300",
  warning:
    "bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300",
  info:
    "bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300",
  neutral:
    "bg-zinc-100 text-zinc-600 dark:bg-zinc-800 dark:text-zinc-400",
};

/* ------------------------------------------------------------------ */
/*  Helpers                                                            */
/* ------------------------------------------------------------------ */

/** Convert RAW_SNAKE to Title Case for display. */
function formatLabel(raw: string): string {
  return raw
    .replace(/_/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

/* ------------------------------------------------------------------ */
/*  Component                                                          */
/* ------------------------------------------------------------------ */

export interface StatusBadgeProps {
  /** The raw status string from the backend (e.g. "PASS", "RUNNING"). */
  status: string;
  /** Badge size. @default "md" */
  size?: "sm" | "md";
  className?: string;
}

export function StatusBadge({
  status,
  size = "md",
  className,
}: StatusBadgeProps) {
  const normalized = status.toUpperCase().trim();
  const category = STATUS_MAP[normalized] ?? "neutral";
  const isRunning = normalized === "RUNNING";

  return (
    <span
      data-slot="status-badge"
      {...(isRunning ? { role: "status", "aria-live": "polite" } : {})}
      className={cn(
        "inline-flex items-center rounded-full font-medium select-none",
        size === "sm" ? "text-xs px-2 py-0.5" : "text-xs px-2.5 py-1",
        CATEGORY_CLASSES[category],
        isRunning && "animate-pulse",
        className,
      )}
    >
      {formatLabel(status)}
    </span>
  );
}
