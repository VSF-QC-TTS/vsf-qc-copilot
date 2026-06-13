import { useMessages } from 'next-intl';
import { NextIntlClientProvider } from 'next-intl';
import { APP_MONOGRAM, APP_NAME } from '@/lib/branding';

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const messages = useMessages();

  return (
    <div className="flex min-h-[100dvh] flex-col items-center justify-center bg-background px-4 py-12">
      <div className="w-full max-w-md space-y-8">
        <div className="flex flex-col items-center gap-3 text-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary shadow-sm">
            <span className="text-base font-bold text-primary-foreground">
              {APP_MONOGRAM}
            </span>
          </div>
          <h1 className="text-2xl font-semibold tracking-tight">{APP_NAME}</h1>
        </div>
        <NextIntlClientProvider messages={messages}>
          {children}
        </NextIntlClientProvider>
      </div>
    </div>
  );
}
