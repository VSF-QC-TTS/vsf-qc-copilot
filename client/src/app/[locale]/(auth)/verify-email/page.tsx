'use client';

import { Suspense, useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { Check, CircleNotch, XCircle } from '@phosphor-icons/react';

import { Link } from '@/i18n/navigation';
import { verifyEmail } from '@/lib/api/auth';

type VerifyState = 'loading' | 'success' | 'error' | 'missing-token';

function VerifyEmailContent() {
  const t = useTranslations('auth');
  const searchParams = useSearchParams();
  const token = searchParams.get('token');

  const calledRef = useRef(false);
  const [state, setState] = useState<VerifyState>(
    token ? 'loading' : 'missing-token',
  );

  useEffect(() => {
    if (!token || calledRef.current) return;
    calledRef.current = true;

    verifyEmail(token)
      .then(() => setState('success'))
      .catch(() => setState('error'));
  }, [token]);

  if (state === 'missing-token') {
    return (
      <div className="flex flex-col items-center gap-4 text-center">
        <XCircle size={48} weight="bold" className="text-destructive" />
        <p className="text-sm text-destructive">
          {t('verifyEmailMissingToken')}
        </p>
      </div>
    );
  }

  if (state === 'loading') {
    return (
      <div className="flex flex-col items-center gap-4 text-center">
        <CircleNotch
          size={48}
          weight="bold"
          className="animate-spin text-primary"
        />
        <p className="text-sm text-muted-foreground">{t('verifyingEmail')}</p>
      </div>
    );
  }

  if (state === 'success') {
    return (
      <div className="flex flex-col items-center gap-4 text-center">
        <Check size={48} weight="bold" className="text-green-600" />
        <p className="text-lg font-medium">{t('verifyEmailSuccess')}</p>
        <Link
          href="/login"
          className="text-sm font-medium text-primary underline-offset-4 hover:underline"
        >
          {t('goToLogin')}
        </Link>
      </div>
    );
  }

  // state === 'error'
  return (
    <div className="flex flex-col items-center gap-4 text-center">
      <XCircle size={48} weight="bold" className="text-destructive" />
      <p className="text-sm text-destructive">{t('verifyEmailFailed')}</p>
      <Link
        href="/login"
        className="text-sm font-medium text-primary underline-offset-4 hover:underline"
      >
        {t('goToLogin')}
      </Link>
    </div>
  );
}

export default function VerifyEmailPage() {
  const t = useTranslations('auth');

  return (
    <Suspense
      fallback={
        <div className="flex flex-col items-center gap-4 text-center">
          <CircleNotch
            size={48}
            weight="bold"
            className="animate-spin text-primary"
          />
          <p className="text-sm text-muted-foreground">{t('verifyingEmail')}</p>
        </div>
      }
    >
      <VerifyEmailContent />
    </Suspense>
  );
}
