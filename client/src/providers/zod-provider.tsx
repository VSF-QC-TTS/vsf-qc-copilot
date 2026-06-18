'use client';

import { useEffect } from 'react';
import { z } from 'zod';
import { useTranslations } from 'next-intl';

export function ZodProvider({ children }: { children: React.ReactNode }) {
  const t = useTranslations('zod');

  useEffect(() => {
    z.config({
      customError: (issue) => {
        // 1. If a custom message was explicitly provided in the schema: e.g. z.string().min(1, 'customKey')
        if (issue.message) {
          try {
            // Attempt to translate the custom message as a key in the 'zod' namespace
            // (Requires suppressing next-intl strict typing here)
            const translated = t(issue.message as any);
            if (translated && translated !== `${issue.message}`) {
              return { message: translated };
            }
          } catch {
            // Fallback to returning the raw message
          }
          return { message: issue.message };
        }

        // 2. Fallback to generic translations for common Zod issues
        switch (issue.code) {
          case 'invalid_type':
            if (issue.expected === 'nonoptional') {
              return { message: t('required') };
            }
            return { message: t('invalidType') };

          case 'too_small':
            if (issue.origin === 'string') {
              return { message: t('stringMin', { min: Number(issue.minimum) }) };
            } else if (issue.origin === 'number') {
              return { message: t('numberMin', { min: Number(issue.minimum) }) };
            } else if (issue.origin === 'array') {
              return { message: t('arrayMin', { min: Number(issue.minimum) }) };
            }
            break;

          case 'too_big':
            if (issue.origin === 'string') {
              return { message: t('stringMax', { max: Number(issue.maximum) }) };
            } else if (issue.origin === 'number') {
              return { message: t('numberMax', { max: Number(issue.maximum) }) };
            } else if (issue.origin === 'array') {
              return { message: t('arrayMax', { max: Number(issue.maximum) }) };
            }
            break;

          case 'invalid_format':
            if ('format' in issue && issue.format === 'email') {
              return { message: t('invalidEmail') };
            } else if ('format' in issue && issue.format === 'url') {
              return { message: t('invalidUrl') };
            }
            break;
        }

        return null;
      },
    });
  }, [t]);

  return <>{children}</>;
}
