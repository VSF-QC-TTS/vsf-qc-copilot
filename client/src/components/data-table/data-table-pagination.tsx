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
  onPageSizeChange: (size: number) => void;
}

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100] as const;

export function DataTablePagination({
  pageIndex,
  pageSize,
  totalItems,
  totalPages,
  onPageChange,
  onPageSizeChange,
}: DataTablePaginationProps) {
  const t = useTranslations('table');
  const rangeStart = totalItems === 0 ? 0 : pageIndex * pageSize + 1;
  const rangeEnd = Math.min((pageIndex + 1) * pageSize, totalItems);

  const isFirstPage = pageIndex === 0;
  const isLastPage = pageIndex >= totalPages - 1;

  const handlePageSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    onPageSizeChange(Number(e.target.value));
  };

  return (
    <div className="flex flex-col items-center justify-between gap-4 px-2 py-4 sm:flex-row">
      <p className="text-sm text-muted-foreground">
        {t('showing', { from: rangeStart, to: rangeEnd, total: totalItems })}
      </p>

      <div className="flex items-center gap-4">
        <div className="flex items-center gap-2">
          <label
            htmlFor="page-size-select"
            className="text-sm text-muted-foreground"
          >
            {t('rowsPerPage')}
          </label>
          <select
            id="page-size-select"
            value={pageSize}
            onChange={handlePageSizeChange}
            className="h-8 rounded-md border border-input bg-background px-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            {PAGE_SIZE_OPTIONS.map((size) => (
              <option key={size} value={size}>
                {size}
              </option>
            ))}
          </select>
        </div>

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
