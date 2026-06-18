'use client';

import { useMemo } from 'react';
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
  enableRowSelection?: boolean;
  selectedRows?: Set<string>;
  onSelectionChange?: (selected: Set<string>) => void;
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
  isSelected: boolean,
): CellContext<TData, TValue> {
  const columnId = getColumnKey(column, columnIndex);
  const value = getColumnValue(row, column);

  return {
    cell: {
      id: `${rowIndex}_${columnId}`,
      column: { id: columnId, columnDef: column, getSize: () => getColumnSize(column) ?? 150 },
      row: { id: String(rowIndex), original: row, getIsSelected: () => isSelected },
      getValue: () => value,
      renderValue: () => value,
    },
    column: { id: columnId, columnDef: column, getSize: () => getColumnSize(column) ?? 150 },
    getValue: () => value,
    renderValue: () => value,
    row: { id: String(rowIndex), original: row, getIsSelected: () => isSelected },
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
  enableRowSelection = false,
  selectedRows,
  onSelectionChange,
}: DataTableProps<TData, TValue>) {
  const t = useTranslations('table');

  const effectiveColumns = useMemo(() => {
    if (!enableRowSelection) return columns;

    const selectColumn: ColumnDef<TData, TValue> = {
      id: '_select',
      size: 44,
      header: () => {
        const allSelected =
          data.length > 0 &&
          data.every((row, i) => {
            const key = getRowKey(row, i);
            return selectedRows?.has(key);
          });
        return (
          <input
            type="checkbox"
            checked={allSelected}
            onChange={(e) => {
              if (!onSelectionChange) return;
              if (e.target.checked) {
                const all = new Set<string>(
                  data.map((r, i) => getRowKey(r, i)),
                );
                onSelectionChange(all);
              } else {
                onSelectionChange(new Set());
              }
            }}
            aria-label="Select all rows"
            className="size-4 rounded border-input accent-primary"
          />
        );
      },
      cell: ({ row }) => {
        const key = getRowKey(row.original, 0);
        const isSelected = selectedRows?.has(key) ?? false;
        return (
          <input
            type="checkbox"
            checked={isSelected}
            onChange={(e) => {
              e.stopPropagation();
              if (!onSelectionChange || !selectedRows) return;
              const next = new Set(selectedRows);
              if (e.target.checked) {
                next.add(key);
              } else {
                next.delete(key);
              }
              onSelectionChange(next);
            }}
            onClick={(e) => e.stopPropagation()}
            aria-label={`Select row ${key}`}
            className="size-4 rounded border-input accent-primary"
          />
        );
      },
    };

    return [selectColumn, ...columns];
  }, [columns, enableRowSelection, data, selectedRows, onSelectionChange]);

  return (
    <div className="overflow-x-auto rounded-md border">
      <table className="w-full caption-bottom text-sm">
        <thead className="border-b bg-muted/50">
          <tr>
            {effectiveColumns.map((column, columnIndex) => {
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
              columns={effectiveColumns.length}
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
                    onRowClick && 'cursor-pointer',
                    selectedRows?.has(getRowKey(row, rowIndex)) && 'bg-primary/5',
                  )}
                  onClick={handleRowClick}
                >
                  {effectiveColumns.map((column, columnIndex) => {
                    const columnKey = getColumnKey(column, columnIndex);
                    const value = getColumnValue(row, column);
                    const rowSelected = selectedRows?.has(getRowKey(row, rowIndex)) ?? false;

                    return (
                      <td key={columnKey} className="px-4 py-3 align-middle">
                        {column.cell
                          ? flexRender(
                              column.cell,
                              makeCellContext(row, rowIndex, column, columnIndex, rowSelected),
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
              <td colSpan={effectiveColumns.length} className="h-48">
                <EmptyState title={emptyMessage ?? t('noData')} action={emptyAction} />
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
