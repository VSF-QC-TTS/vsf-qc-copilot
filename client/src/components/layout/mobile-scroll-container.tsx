import { cn } from '@/lib/utils';

type MobileScrollContainerProps = {
  children: React.ReactNode;
  className?: string;
};

export function MobileScrollContainer({
  children,
  className,
}: MobileScrollContainerProps) {
  return (
    <div
      className={cn(
        'overflow-x-auto -mx-4 px-4 md:mx-0 md:px-0',
        className,
      )}
    >
      {children}
    </div>
  );
}
