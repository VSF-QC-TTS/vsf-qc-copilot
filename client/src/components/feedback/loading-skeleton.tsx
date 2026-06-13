import * as React from "react";

import { cn } from "@/lib/utils";

/* ------------------------------------------------------------------ */
/*  Base skeleton                                                      */
/* ------------------------------------------------------------------ */

export interface SkeletonProps extends React.ComponentProps<"div"> {}

export function Skeleton({ className, ...props }: SkeletonProps) {
  return (
    <div
      data-slot="skeleton"
      className={cn("animate-pulse rounded-md bg-muted", className)}
      {...props}
    />
  );
}

/* ------------------------------------------------------------------ */
/*  SkeletonText                                                       */
/* ------------------------------------------------------------------ */

export interface SkeletonTextProps {
  /** Tailwind width class, e.g. "w-3/4". @default "w-full" */
  width?: string;
  className?: string;
}

export function SkeletonText({ width = "w-full", className }: SkeletonTextProps) {
  return <Skeleton className={cn("h-4", width, className)} />;
}

/* ------------------------------------------------------------------ */
/*  SkeletonRect                                                       */
/* ------------------------------------------------------------------ */

export interface SkeletonRectProps {
  /** Tailwind width class. @default "w-full" */
  width?: string;
  /** Tailwind height class. @default "h-24" */
  height?: string;
  className?: string;
}

export function SkeletonRect({
  width = "w-full",
  height = "h-24",
  className,
}: SkeletonRectProps) {
  return <Skeleton className={cn(width, height, className)} />;
}

/* ------------------------------------------------------------------ */
/*  SkeletonCircle                                                     */
/* ------------------------------------------------------------------ */

export interface SkeletonCircleProps {
  /** Tailwind size class. @default "size-10" */
  size?: string;
  className?: string;
}

export function SkeletonCircle({ size = "size-10", className }: SkeletonCircleProps) {
  return <Skeleton className={cn("rounded-full", size, className)} />;
}

/* ------------------------------------------------------------------ */
/*  SkeletonTableRows                                                  */
/* ------------------------------------------------------------------ */

export interface SkeletonTableRowsProps {
  /** Number of rows to render. @default 5 */
  count?: number;
  /** Number of columns per row. @default 4 */
  columns?: number;
  className?: string;
}

export function SkeletonTableRows({
  count = 5,
  columns = 4,
  className,
}: SkeletonTableRowsProps) {
  return (
    <>
      {Array.from({ length: count }, (_, rowIdx) => (
        <tr key={rowIdx} className={cn("animate-pulse", className)}>
          {Array.from({ length: columns }, (_, colIdx) => (
            <td key={colIdx} className="px-4 py-3">
              <Skeleton
                className={cn(
                  "h-4",
                  colIdx === 0 ? "w-2/3" : "w-full",
                )}
              />
            </td>
          ))}
        </tr>
      ))}
    </>
  );
}
