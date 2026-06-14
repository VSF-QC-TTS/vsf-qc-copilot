'use client';

import type { Column } from '@tanstack/react-table';
import { ArrowDownIcon, ArrowUpIcon, ArrowsDownUpIcon } from '@phosphor-icons/react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';

interface DataTableColumnHeaderProps<TData, TValue> {
  column: Column<TData, TValue>;
  title: string;
  className?: string;
}

export function DataTableColumnHeader<TData, TValue>({
  column,
  title,
  className,
}: DataTableColumnHeaderProps<TData, TValue>) {
  if (!column.getCanSort()) {
    return <span className={cn(className)}>{title}</span>;
  }

  const sorted = column.getIsSorted();

  const handleToggleSort = () => {
    if (sorted === false) {
      column.toggleSorting(false); // set asc
    } else if (sorted === 'asc') {
      column.toggleSorting(true); // set desc
    } else {
      column.clearSorting(); // clear
    }
  };

  return (
    <Button
      variant="ghost"
      size="sm"
      className={cn('-ml-3 h-8 data-[state=open]:bg-accent', className)}
      onClick={handleToggleSort}
    >
      <span>{title}</span>
      {sorted === 'asc' ? (
        <ArrowUpIcon className="ml-1 size-4" />
      ) : sorted === 'desc' ? (
        <ArrowDownIcon className="ml-1 size-4" />
      ) : (
        <ArrowsDownUpIcon className="ml-1 size-4 text-muted-foreground/50" />
      )}
    </Button>
  );
}
