'use client';

import * as React from 'react';
import { Plus, Minus } from '@phosphor-icons/react';
import { useTranslations } from 'next-intl';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';

// ---------------------------------------------------------------------------
// Shared input style (matches project patterns)
// ---------------------------------------------------------------------------

const inputClassName =
  'flex h-9 w-full rounded-md border border-input bg-background px-3 py-1.5 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface KeyValueEditorProps {
  value: Record<string, string>;
  onChange: (val: Record<string, string>) => void;
  label: string;
  keyPlaceholder?: string;
  valuePlaceholder?: string;
  disabled?: boolean;
  /** If true, value inputs render as type=password */
  masked?: boolean;
  className?: string;
}

// ---------------------------------------------------------------------------
// Internal representation — maintains insertion order via array
// ---------------------------------------------------------------------------

type KVRow = { id: string; key: string; value: string };

let nextId = 0;
function uid() {
  return `kv-${++nextId}`;
}

function toRows(record: Record<string, string>): KVRow[] {
  const entries = Object.entries(record);
  if (entries.length === 0) return [{ id: uid(), key: '', value: '' }];
  return entries.map(([key, value]) => ({ id: uid(), key, value }));
}

function toRecord(rows: KVRow[]): Record<string, string> {
  const record: Record<string, string> = {};
  for (const row of rows) {
    const k = row.key.trim();
    if (k) record[k] = row.value;
  }
  return record;
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function KeyValueEditor({
  value,
  onChange,
  label,
  keyPlaceholder,
  valuePlaceholder,
  disabled = false,
  masked = false,
  className,
}: KeyValueEditorProps) {
  const t = useTranslations('common');

  // Derive rows from value only on initial mount to avoid cursor-jump issues
  const [rows, setRows] = React.useState<KVRow[]>(() => toRows(value));

  // Sync outward whenever rows change
  const emit = React.useCallback(
    (updated: KVRow[]) => {
      setRows(updated);
      onChange(toRecord(updated));
    },
    [onChange],
  );

  const handleKeyChange = (id: string, newKey: string) => {
    emit(rows.map((r) => (r.id === id ? { ...r, key: newKey } : r)));
  };

  const handleValueChange = (id: string, newValue: string) => {
    emit(rows.map((r) => (r.id === id ? { ...r, value: newValue } : r)));
  };

  const addRow = () => {
    emit([...rows, { id: uid(), key: '', value: '' }]);
  };

  const removeRow = (id: string) => {
    const next = rows.filter((r) => r.id !== id);
    // Always keep at least one row
    emit(next.length > 0 ? next : [{ id: uid(), key: '', value: '' }]);
  };

  return (
    <div className={cn('space-y-2', className)}>
      <span className="text-sm font-medium leading-none text-foreground">
        {label}
      </span>

      <div className="space-y-2">
        {rows.map((row) => (
          <div key={row.id} className="flex items-center gap-2">
            <input
              type="text"
              placeholder={keyPlaceholder ?? t('key')}
              disabled={disabled}
              value={row.key}
              onChange={(e) => handleKeyChange(row.id, e.target.value)}
              className={cn(inputClassName, 'flex-1')}
            />
            <input
              type={masked ? 'password' : 'text'}
              placeholder={valuePlaceholder ?? t('value')}
              disabled={disabled}
              value={row.value}
              onChange={(e) => handleValueChange(row.id, e.target.value)}
              className={cn(inputClassName, 'flex-1')}
            />
            <Button
              type="button"
              variant="outline"
              size="icon"
              className="size-9 shrink-0"
              disabled={disabled}
              onClick={() => removeRow(row.id)}
              aria-label={t('remove')}
            >
              <Minus size={16} weight="bold" />
            </Button>
          </div>
        ))}
      </div>

      <Button
        type="button"
        variant="outline"
        size="sm"
        disabled={disabled}
        onClick={addRow}
      >
        <Plus size={16} weight="bold" />
        {t('add')}
      </Button>
    </div>
  );
}
