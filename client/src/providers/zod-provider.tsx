'use client';

import { useEffect } from 'react';
import { z } from 'zod';
import { useTranslations } from 'next-intl';

export function ZodProvider({ children }: { children: React.ReactNode }) {
  const t = useTranslations('zod');

  useEffect(() => {
    z.setErrorMap((issue, ctx) => {
      // 1. If a custom message was explicitly provided in the schema: e.g. z.string().min(1, 'customKey')
      if (issue.message && issue.message !== ctx.defaultError) {
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
        case z.ZodIssueCode.invalid_type:
          if (issue.received === 'undefined') {
            return { message: t('required') };
          }
          return { message: t('invalidType') };

        case z.ZodIssueCode.too_small:
          if (issue.type === 'string') {
            return { message: t('stringMin', { min: issue.minimum }) };
          } else if (issue.type === 'number') {
            return { message: t('numberMin', { min: issue.minimum }) };
          } else if (issue.type === 'array') {
            return { message: t('arrayMin', { min: issue.minimum }) };
          }
          break;

        case z.ZodIssueCode.too_big:
          if (issue.type === 'string') {
            return { message: t('stringMax', { max: issue.maximum }) };
          } else if (issue.type === 'number') {
            return { message: t('numberMax', { max: issue.maximum }) };
          } else if (issue.type === 'array') {
            return { message: t('arrayMax', { max: issue.maximum }) };
          }
          break;

        case z.ZodIssueCode.invalid_string:
          if (issue.validation === 'email') {
            return { message: t('invalidEmail') };
          } else if (issue.validation === 'url') {
            return { message: t('invalidUrl') };
          }
          break;
      }

      return { message: ctx.defaultError };
    });
  }, [t]);

  return <>{children}</>;
}
