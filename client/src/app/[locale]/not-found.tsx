import { useTranslations } from 'next-intl';
import { Link } from '@/i18n/navigation';

export default function NotFoundPage() {
  const t = useTranslations('errors');
  const tCommon = useTranslations('common');

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-background px-4 text-center">
      <div className="w-full max-w-md space-y-6 rounded-lg border bg-card p-8">
        <div className="flex flex-col items-center gap-3">
          <span className="text-6xl font-bold text-muted-foreground">404</span>
          <h2 className="text-xl font-semibold">{t('notFound')}</h2>
        </div>
        <Link
          href="/"
          className="inline-flex h-10 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground ring-offset-background transition-colors hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
        >
          {tCommon('back')}
        </Link>
      </div>
    </div>
  );
}
