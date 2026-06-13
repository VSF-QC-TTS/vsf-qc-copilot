'use client';

import {
  type ColumnDef,
  type SortingState,
  flexRender,
  getCoreRowModel,
  useReactTable,
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

export function DataTable<TData, TValue>({
  columns,
  data,
  totalItems,
  pageIndex,
  pageSize,
  onPaginationChange,
  sorting,
  onSortingChange,
  loading = false,
  emptyMessage = 'No results found.',
  emptyAction,
  onRowClick,
}: DataTableProps<TData, TValue>) {
  const table = useReactTable({
    data,
    columns,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
    manualSorting: true,
    pageCount: Math.ceil(totalItems / pageSize),
    state: {
      pagination: { pageIndex, pageSize },
      ...(sorting ? { sorting } : {}),
    },
    onPaginationChange: (updater) => {
      const next =
        typeof updater === 'function'
          ? updater({ pageIndex, pageSize })
          : updater;
      onPaginationChange(next.pageIndex, next.pageSize);
    },
    onSortingChange: onSortingChange
      ? (updater) => {
          const next =
            typeof updater === 'function'
              ? updater(sorting ?? [])
              : updater;
          onSortingChange(next);
        }
      : undefined,
  });

  return (
    <div className="overflow-x-auto rounded-md border">
      <table className="w-full caption-bottom text-sm">
        <thead className="border-b bg-muted/50">
          {table.getHeaderGroups().map((headerGroup) => (
            <tr key={headerGroup.id}>
              {headerGroup.headers.map((header) => (
                <th
                  key={header.id}
                  className="h-10 px-4 text-left align-middle font-medium text-muted-foreground"
                  style={{ width: header.getSize() !== 150 ? header.getSize() : undefined }}
                >
                  {header.isPlaceholder
                    ? null
                    : flexRender(
                        header.column.columnDef.header,
                        header.getContext()
                      )}
                </th>
              ))}
            </tr>
          ))}
        </thead>
        <tbody className="divide-y">
          {loading ? (
            <SkeletonTableRows
              columns={columns.length}
              count={pageSize}
            />
          ) : table.getRowModel().rows.length > 0 ? (
            table.getRowModel().rows.map((row) => {
              const handleRowClick = onRowClick
                ? () => onRowClick(row.original)
                : undefined;

              return (
                <tr
                  key={row.id}
                  data-state={row.getIsSelected() ? 'selected' : undefined}
                  className={cn(
                    'transition-colors hover:bg-muted/50',
                    onRowClick && 'cursor-pointer'
                  )}
                  onClick={handleRowClick}
                >
                  {row.getVisibleCells().map((cell) => (
                    <td key={cell.id} className="px-4 py-3 align-middle">
                      {flexRender(
                        cell.column.columnDef.cell,
                        cell.getContext()
                      )}
                    </td>
                  ))}
                </tr>
              );
            })
          ) : (
            <tr>
              <td colSpan={columns.length} className="h-48">
                <EmptyState title={emptyMessage} action={emptyAction} />
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
