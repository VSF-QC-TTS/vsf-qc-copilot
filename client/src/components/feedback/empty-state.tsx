import * as React from "react";
import { MagnifyingGlassIcon } from "@phosphor-icons/react/dist/ssr";

import { cn } from "@/lib/utils";

export interface EmptyStateProps {
  /** Primary heading shown below the icon. */
  title: string;
  /** Optional secondary text below the title. */
  description?: string;
  /** Override the default search icon. */
  icon?: React.ReactNode;
  /** Optional CTA rendered below the description (e.g. a Button). */
  action?: React.ReactNode;
  className?: string;
}

export function EmptyState({
  title,
  description,
  icon,
  action,
  className,
}: EmptyStateProps) {
  return (
    <div
      data-slot="empty-state"
      className={cn(
        "flex flex-col items-center justify-center gap-3 py-12 text-center",
        className,
      )}
    >
      <div className="text-muted-foreground/60">
        {icon ?? <MagnifyingGlassIcon size={48} weight="duotone" />}
      </div>

      <div className="space-y-1">
        <h3 className="text-base font-semibold text-foreground">{title}</h3>
        {description && (
          <p className="text-sm text-muted-foreground max-w-sm">{description}</p>
        )}
      </div>

      {action && <div className="mt-2">{action}</div>}
    </div>
  );
}
