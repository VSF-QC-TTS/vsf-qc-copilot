'use client';

import * as React from 'react';
import { useTranslations } from 'next-intl';
import { useTheme } from 'next-themes';
import { toast, Toaster } from 'sonner';

import { setOnApiError } from '@/lib/api/client';
import type { ApiError } from '@/lib/api/types';
import { getErrorMessage } from '@/lib/utils/error-messages';

export function ApiErrorToaster() {
  const tErrors = useTranslations('errors');
  const { resolvedTheme } = useTheme();

  React.useEffect(() => {
    setOnApiError((error: ApiError) => {
      const message = getErrorMessage(error, tErrors);
      toast.error(message, {
        id: `${error.status}-${error.code}-${message}`,
      });
    });

    return () => setOnApiError(null);
  }, [tErrors]);

  return (
    <Toaster
      closeButton
      richColors
      position="top-right"
      theme={resolvedTheme === 'dark' ? 'dark' : 'light'}
    />
  );
}
