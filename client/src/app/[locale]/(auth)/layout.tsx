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
    <div className="relative flex min-h-[100dvh] flex-col items-center justify-center overflow-hidden bg-background px-4 py-12">
      {/* Premium Ambient Radial Glow Background */}
      <div className="absolute inset-0 -z-10 bg-[radial-gradient(circle_at_top,hsl(var(--primary)/0.07),transparent_55%)]" />
      <div className="absolute inset-0 -z-10 bg-[radial-gradient(circle_at_bottom,hsl(var(--primary)/0.02),transparent_40%)]" />

      <div className="w-full max-w-md space-y-6">
        {/* Animated Brand Header */}
        <div className="flex flex-col items-center gap-3 text-center animate-in fade-in slide-in-from-top-4 duration-500">
          <div className="group relative flex h-14 w-14 items-center justify-center rounded-2xl border bg-card p-1 shadow-md transition-all duration-300 hover:scale-105 hover:shadow-lg">
            {/* Logo Ambient Halo on Hover */}
            <div className="absolute inset-0 -z-10 rounded-2xl bg-primary/25 blur-md opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
            <div className="h-full w-full overflow-hidden rounded-xl">
              <Image 
                src="/logo.png" 
                alt="Logo" 
                width={48} 
                height={48} 
                className="object-cover transition-transform duration-500 group-hover:scale-110" 
              />
            </div>
          </div>
          <h1 className="text-2xl font-bold tracking-tight bg-gradient-to-b from-foreground to-foreground/85 bg-clip-text text-transparent">
            {APP_NAME}
          </h1>
        </div>

        {/* Polished Glassmorphic Form Card Wrapper */}
        <div className="rounded-2xl border border-border/80 bg-card/65 p-6 shadow-xl backdrop-blur-lg dark:bg-card/45 dark:shadow-2xl sm:p-8 animate-in fade-in slide-in-from-bottom-4 duration-600">
          <NextIntlClientProvider messages={messages}>
            {children}
          </NextIntlClientProvider>
        </div>
      </div>
    </div>
  );
}
