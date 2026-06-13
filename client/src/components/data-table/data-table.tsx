'use client';

import { useTranslations } from 'next-intl';
import {
  type ColumnDef,
  type CellContext,
  type HeaderContext,
  type SortingState,
  flexRender,
} from '@tanstack/react-table';

import { cn } from '@/lib/utils';
import { SkeletonTableRows } from '@/components/feedback/loading-skeleton';
import { EmptyState } from '@/components/feedback/empty-state';

interface DataTableProps<TData, TValue> {
  columns: ColumnDef<TData, TValue>[];
  data: TData[];
  totalItems: number;
  pageIndex: number;
  pageSize: number;
  onPaginationChange: (page: number, size: number) => void;
  sorting?: SortingState;
  onSortingChange?: (sorting: SortingState) => void;
  loading?: boolean;
  emptyMessage?: string;
  emptyAction?: React.ReactNode;
  onRowClick?: (row: TData) => void;
}

function getColumnKey<TData, TValue>(
  column: ColumnDef<TData, TValue>,
  index: number,
): string {
  if ('id' in column && column.id) {
    return column.id;
  }
  if ('accessorKey' in column && typeof column.accessorKey === 'string') {
    return column.accessorKey;
  }
  return `column-${index}`;
}

function getColumnSize<TData, TValue>(
  column: ColumnDef<TData, TValue>,
): number | undefined {
  return 'size' in column && typeof column.size === 'number'
    ? column.size
    : undefined;
}

function getColumnValue<TData, TValue>(
  row: TData,
  column: ColumnDef<TData, TValue>,
): unknown {
  if (!('accessorKey' in column) || typeof column.accessorKey !== 'string') {
    return undefined;
  }

  return (row as Record<string, unknown>)[column.accessorKey];
}

function getRowKey<TData>(row: TData, index: number): string {
  if (row && typeof row === 'object') {
    const record = row as Record<string, unknown>;
    const stableId = record.publicId ?? record.id;
    if (typeof stableId === 'string' || typeof stableId === 'number') {
      return String(stableId);
    }
  }

  return `row-${index}`;
}

function makeCellContext<TData, TValue>(
  row: TData,
  rowIndex: number,
  column: ColumnDef<TData, TValue>,
  columnIndex: number,
): CellContext<TData, TValue> {
  const columnId = getColumnKey(column, columnIndex);
  const value = getColumnValue(row, column);

  return {
    cell: {
      id: `${rowIndex}_${columnId}`,
      column: { id: columnId, columnDef: column, getSize: () => getColumnSize(column) ?? 150 },
      row: { id: String(rowIndex), original: row, getIsSelected: () => false },
      getValue: () => value,
      renderValue: () => value,
    },
    column: { id: columnId, columnDef: column, getSize: () => getColumnSize(column) ?? 150 },
    getValue: () => value,
    renderValue: () => value,
    row: { id: String(rowIndex), original: row, getIsSelected: () => false },
    table: {},
  } as unknown as CellContext<TData, TValue>;
}

function makeHeaderContext<TData, TValue>(
  column: ColumnDef<TData, TValue>,
  columnIndex: number,
): HeaderContext<TData, TValue> {
  const columnId = getColumnKey(column, columnIndex);

  return {
    column: { id: columnId, columnDef: column, getSize: () => getColumnSize(column) ?? 150 },
    header: { id: columnId, column: { id: columnId, columnDef: column } },
    table: {},
  } as unknown as HeaderContext<TData, TValue>;
}

export function DataTable<TData, TValue>({
  columns,
  data,
  pageSize,
  loading = false,
  emptyMessage,
  emptyAction,
  onRowClick,
}: DataTableProps<TData, TValue>) {
  const t = useTranslations('table');

  return (
    <div className="overflow-x-auto rounded-md border">
      <table className="w-full caption-bottom text-sm">
        <thead className="border-b bg-muted/50">
          <tr>
            {columns.map((column, columnIndex) => {
              const columnSize = getColumnSize(column);
              const columnKey = getColumnKey(column, columnIndex);

              return (
                <th
                  key={columnKey}
                  className="h-10 px-4 text-left align-middle font-medium text-muted-foreground"
                  style={{ width: columnSize && columnSize !== 150 ? columnSize : undefined }}
                >
                  {flexRender(
                    column.header,
                    makeHeaderContext(column, columnIndex),
                  )}
                </th>
              );
            })}
          </tr>
        </thead>
        <tbody className="divide-y">
          {loading ? (
            <SkeletonTableRows
              columns={columns.length}
              count={pageSize}
            />
          ) : data.length > 0 ? (
            data.map((row, rowIndex) => {
              const handleRowClick = onRowClick
                ? () => onRowClick(row)
                : undefined;

              return (
                <tr
                  key={getRowKey(row, rowIndex)}
                  className={cn(
                    'transition-colors hover:bg-muted/50',
                    onRowClick && 'cursor-pointer'
                  )}
                  onClick={handleRowClick}
                >
                  {columns.map((column, columnIndex) => {
                    const columnKey = getColumnKey(column, columnIndex);
                    const value = getColumnValue(row, column);

                    return (
                      <td key={columnKey} className="px-4 py-3 align-middle">
                        {column.cell
                          ? flexRender(
                              column.cell,
                              makeCellContext(row, rowIndex, column, columnIndex),
                            )
                          : String(value ?? '')}
                      </td>
                    );
                  })}
                </tr>
              );
            })
          ) : (
            <tr>
              <td colSpan={columns.length} className="h-48">
                <EmptyState title={emptyMessage ?? t('noData')} action={emptyAction} />
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
