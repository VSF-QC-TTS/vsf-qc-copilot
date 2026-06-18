'use client';

import { useState } from 'react';
import { useTranslations } from 'next-intl';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link } from '@/i18n/navigation';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { forgotPassword } from '@/lib/api/auth';
import {
  forgotPasswordSchema,
  type ForgotPasswordFormValues,
} from '@/lib/validations/auth';

const inputClassName =
  "flex h-10 w-full rounded-lg border border-border/80 bg-background/50 px-3 py-2 text-sm transition-all duration-200 placeholder:text-muted-foreground hover:border-primary/30 focus-visible:border-primary/50 focus-visible:bg-background focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-primary/10 disabled:cursor-not-allowed disabled:opacity-50 shadow-xs";

export default function ForgotPasswordPage() {
  const t = useTranslations('auth');
  const [submitted, setSubmitted] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
  });

  async function onSubmit(data: ForgotPasswordFormValues) {
    setIsLoading(true);
    try {
      await forgotPassword(data.email);
    } catch {
      // Intentionally swallow — never reveal whether the account exists
    } finally {
      setIsLoading(false);
      setSubmitted(true);
    }
  }

  if (submitted) {
    return (
      <div className="flex flex-col items-center gap-4 text-center py-4 animate-in zoom-in-95 duration-400">
        <div className="flex h-14 w-14 items-center justify-center rounded-full bg-green-500/10 border border-green-500/20 text-green-600 dark:text-green-400 shadow-xs">
          <svg
            className="size-6 shrink-0"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth="2.5"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M3 19v-8.93a2 2 0 01.89-1.664l8-5.333a2 2 0 012.22 0l8 5.333A2 2 0 0121 10.07V19M3 19a2 2 0 002 2h14a2 2 0 002-2M3 19l6.75-4.5M21 19l-6.75-4.5M3 10l6.75 4.5M21 10l-6.75 4.5m0 0l-2.25-1.5a2 2 0 00-2.22 0l-2.25 1.5"
            />
          </svg>
        </div>
        <div className="space-y-1.5">
          <h3 className="text-lg font-bold text-foreground">Kiểm tra hộp thư của bạn</h3>
          <p className="text-sm text-muted-foreground max-w-[280px]">
            {t('forgotPasswordSuccess')}
          </p>
        </div>
        <Button asChild variant="outline" className="mt-2 transition-transform active:scale-[0.98] w-full max-w-[150px]">
          <Link href="/login">
            {t('backToLogin')}
          </Link>
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="space-y-1.5 text-center">
        <h2 className="text-xl font-bold tracking-tight text-foreground">
          {t('forgotPasswordTitle')}
        </h2>
        <p className="text-sm text-muted-foreground">
          {t('forgotPasswordDescription')}
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div className="space-y-1.5">
          <label
            htmlFor="email"
            className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/90"
          >
            {t('emailLabel')}
          </label>
          <input
            id="email"
            type="email"
            placeholder={t('emailPlaceholder')}
            autoComplete="email"
            disabled={isLoading}
            className={cn(
              inputClassName,
              errors.email &&
                "border-destructive focus-visible:ring-destructive/20 focus-visible:border-destructive",
            )}
            {...register('email')}
          />
          {errors.email && (
            <p className="text-xs font-medium text-destructive mt-1" role="alert">
              {errors.email.message}
            </p>
          )}
        </div>

        <Button 
          type="submit" 
          disabled={isLoading} 
          className="w-full transition-transform active:scale-[0.98] font-medium shadow-xs"
        >
          {isLoading ? t('submitting') : t('forgotPasswordSubmit')}
        </Button>
      </form>

      <div className="text-center pt-2">
        <Link
          href="/login"
          className="text-sm font-medium text-muted-foreground underline-offset-4 hover:text-primary transition-colors hover:underline"
        >
          {t('backToLogin')}
        </Link>
      </div>
    </div>
  );
}
