import { useMessages } from 'next-intl';
import { NextIntlClientProvider } from 'next-intl';
import Image from 'next/image';
import { APP_NAME } from '@/lib/branding';

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
          <div className="flex h-12 w-12 items-center justify-center rounded-xl overflow-hidden shadow-sm">
            <Image src="/logo.png" alt="Logo" width={48} height={48} className="object-cover" />
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
