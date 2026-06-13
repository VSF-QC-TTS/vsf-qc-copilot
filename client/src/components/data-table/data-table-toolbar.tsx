import { cn } from '@/lib/utils';

interface DataTableToolbarProps {
  children: React.ReactNode;
  className?: string;
}

export function DataTableToolbar({ children, className }: DataTableToolbarProps) {
  return (
    <div className={cn('flex items-center justify-between gap-2 py-4', className)}>
      {children}
    </div>
  );
}
