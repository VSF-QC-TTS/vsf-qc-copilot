'use client';

import { useTranslations } from 'next-intl';
import {
  CaretDoubleLeft,
  CaretDoubleRight,
  CaretLeft,
  CaretRight,
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
  pageSize,
  totalItems,
  totalPages,
  onPageChange,
}: DataTablePaginationProps) {
  const t = useTranslations('table');
  const rangeStart = totalItems === 0 ? 0 : pageIndex * pageSize + 1;
  const rangeEnd = Math.min((pageIndex + 1) * pageSize, totalItems);

  const isFirstPage = pageIndex === 0;
  const isLastPage = pageIndex >= totalPages - 1;

  return (
    <div className="flex flex-col items-center justify-between gap-4 px-2 py-4 sm:flex-row">
      <p className="text-sm text-muted-foreground">
        {t('showing', { from: rangeStart, to: rangeEnd, total: totalItems })}
      </p>

      <div className="flex items-center gap-4">
        <div className="flex items-center gap-1">
          <Button
            variant="outline"
            size="icon"
            className="size-8"
            onClick={() => onPageChange(0)}
            disabled={isFirstPage}
            aria-label={t('firstPage')}
          >
            <CaretDoubleLeft className="size-4" />
          </Button>
          <Button
            variant="outline"
            size="icon"
            className="size-8"
            onClick={() => onPageChange(pageIndex - 1)}
            disabled={isFirstPage}
            aria-label={t('previousPage')}
          >
            <CaretLeft className="size-4" />
          </Button>

          <span className="min-w-[5ch] text-center text-sm text-muted-foreground">
            {totalPages === 0
              ? '0 / 0'
              : t('page', { current: pageIndex + 1, total: totalPages })}
          </span>

          <Button
            variant="outline"
            size="icon"
            className="size-8"
            onClick={() => onPageChange(pageIndex + 1)}
            disabled={isLastPage}
            aria-label={t('nextPage')}
          >
            <CaretRight className="size-4" />
          </Button>
          <Button
            variant="outline"
            size="icon"
            className="size-8"
            onClick={() => onPageChange(totalPages - 1)}
            disabled={isLastPage}
            aria-label={t('lastPage')}
          >
            <CaretDoubleRight className="size-4" />
          </Button>
        </div>
      </div>
    </div>
  );
}
