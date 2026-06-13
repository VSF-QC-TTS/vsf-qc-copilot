'use client';

import { useEffect } from 'react';
import { useTranslations } from 'next-intl';
import { ArrowClockwise, WarningCircle } from '@phosphor-icons/react';

import { Button } from '@/components/ui/button';

type ErrorProps = {
  error: Error & { digest?: string };
  reset: () => void;
};

export default function DashboardError({ error, reset }: ErrorProps) {
  const t = useTranslations('errors');
  const tCommon = useTranslations('common');

  useEffect(() => {
    console.error('[DashboardError]', error);
  }, [error]);

  return (
    <div className="flex min-h-[50vh] flex-col items-center justify-center px-4 text-center">
      <div className="w-full max-w-md space-y-6 rounded-lg border bg-card p-8">
        <div className="flex flex-col items-center gap-3">
          <WarningCircle size={48} className="text-destructive" />
          <h2 className="text-xl font-semibold">{t('generic')}</h2>
        </div>

        {process.env.NODE_ENV === 'development' && (
          <pre className="max-h-40 overflow-auto rounded bg-muted p-3 text-left text-xs text-muted-foreground">
            {error.message}
            {error.stack ? `\n\n${error.stack}` : ''}
          </pre>
        )}

        <Button onClick={reset} className="w-full">
          <ArrowClockwise size={16} weight="bold" className="mr-2" />
          {tCommon('retry')}
        </Button>
      </div>
    </div>
  );
}
