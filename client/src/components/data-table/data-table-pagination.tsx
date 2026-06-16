'use client';

import { useTranslations } from 'next-intl';
import {
  CaretDoubleLeftIcon,
  CaretDoubleRightIcon,
  CaretLeftIcon,
  CaretRightIcon,
} from '@phosphor-icons/react';

import { Button } from '@/components/ui/button';

interface DataTablePaginationProps {
  pageIndex: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  onPageSizeChange?: (size: number) => void;
}

export function DataTablePagination({
  pageIndex,
  totalPages,
  onPageChange,
}: DataTablePaginationProps) {
  const t = useTranslations('table');

  const isFirstPage = pageIndex === 0;
  const isLastPage = pageIndex >= totalPages - 1;

  return (
    <div className="flex items-center justify-center px-2 py-3 border-t border-border/40">
      {/* Unified pill control group */}
      <div className="flex items-center rounded-lg border bg-muted/20 p-0.5 dark:bg-muted/5 shadow-xs">
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8 rounded-md hover:bg-background hover:text-foreground active:scale-95 transition-all"
          onClick={() => onPageChange(0)}
          disabled={isFirstPage}
          aria-label={t('firstPage')}
        >
          <CaretDoubleLeftIcon className="size-3.5" />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8 rounded-md hover:bg-background hover:text-foreground active:scale-95 transition-all"
          onClick={() => onPageChange(pageIndex - 1)}
          disabled={isFirstPage}
          aria-label={t('previousPage')}
        >
          <CaretLeftIcon className="size-3.5" />
        </Button>

        {/* Separator line */}
        <div className="h-4 w-px bg-border mx-1" />

        {/* Page status */}
        <span className="px-3 text-xs font-semibold text-foreground/90 select-none min-w-[6ch] text-center">
          {totalPages === 0 ? '0 / 0' : `${pageIndex + 1} / ${totalPages}`}
        </span>

        {/* Separator line */}
        <div className="h-4 w-px bg-border mx-1" />

        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8 rounded-md hover:bg-background hover:text-foreground active:scale-95 transition-all"
          onClick={() => onPageChange(pageIndex + 1)}
          disabled={isLastPage}
          aria-label={t('nextPage')}
        >
          <CaretRightIcon className="size-3.5" />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8 rounded-md hover:bg-background hover:text-foreground active:scale-95 transition-all"
          onClick={() => onPageChange(totalPages - 1)}
          disabled={isLastPage}
          aria-label={t('lastPage')}
        >
          <CaretDoubleRightIcon className="size-3.5" />
        </Button>
      </div>
    </div>
  );
}
