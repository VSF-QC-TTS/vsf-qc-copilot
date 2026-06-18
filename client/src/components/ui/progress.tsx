'use client';

import { cn } from '@/lib/utils';

interface ProgressProps {
  value: number | null;  // 0-100, null = indeterminate
  className?: string;
  label?: string;  // aria-label
}

export function Progress({ value, className, label = 'Progress' }: ProgressProps) {
  const isIndeterminate = value === null || value === undefined;
  const clampedValue = isIndeterminate ? 0 : Math.min(100, Math.max(0, value));

  return (
    <div className={cn('relative w-full overflow-hidden rounded-full bg-muted h-2.5', className)}>
      <div
        role="progressbar"
        aria-valuenow={isIndeterminate ? undefined : clampedValue}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={label}
        className={cn(
          'h-full rounded-full bg-primary transition-all duration-500 ease-out',
          isIndeterminate && 'w-1/3 animate-[progress-indeterminate_1.5s_ease-in-out_infinite]',
        )}
        style={isIndeterminate ? undefined : { width: `${clampedValue}%` }}
      />
    </div>
  );
}
