import * as React from "react";
import { useTranslations } from "next-intl";

import { cn } from "@/lib/utils";
import { Skeleton } from "@/components/feedback/loading-skeleton";

export interface MetricCardProps {
  /** Metric label (e.g. "Total Defects"). */
  label: string;
  /** The metric value. `null` renders a dash. */
  value: string | number | null;
  /** Secondary helper text below the value. */
  description?: string;
  /** Optional icon rendered top-left. */
  icon?: React.ReactNode;
  /** Show skeleton placeholder instead of the value. */
  loading?: boolean;
  /** Optional trend indicator. */
  trend?: { value: number; label: string };
  className?: string;
}

export function MetricCard({
  label,
  value,
  description,
  icon,
  loading = false,
  trend,
  className,
}: MetricCardProps) {
  const tCommon = useTranslations("common");
  const isPositive = trend && trend.value >= 0;

  return (
    <div
      data-slot="metric-card"
      className={cn(
        "rounded-lg border bg-card p-5 shadow-sm",
        className,
      )}
    >
      {/* Header: icon + label */}
      <div className="flex items-center gap-2">
        {icon && (
          <span className="text-muted-foreground [&_svg]:size-4">{icon}</span>
        )}
        <span className="text-sm font-medium text-muted-foreground">
          {label}
        </span>
      </div>

      {/* Value */}
      <div className="mt-2">
        {loading ? (
          <Skeleton className="h-8 w-24" />
        ) : (
          <p className="text-2xl font-bold tracking-tight text-card-foreground">
            {value ?? tCommon("notAvailable")}
          </p>
        )}
      </div>

      {/* Trend + description */}
      <div className="mt-1.5 flex items-center gap-2">
        {trend && !loading && (
          <span
            className={cn(
              "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium",
              isPositive
                ? "bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300"
                : "bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300",
            )}
          >
            {isPositive ? "↑" : "↓"} {Math.abs(trend.value)}%{" "}
            <span className="ml-1">{trend.label}</span>
          </span>
        )}

        {description && !loading && (
          <span className="text-xs text-muted-foreground">{description}</span>
        )}
      </div>
    </div>
  );
}
