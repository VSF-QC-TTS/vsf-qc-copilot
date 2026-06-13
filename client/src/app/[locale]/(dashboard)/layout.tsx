'use client';

import { AuthGuard } from '@/providers/auth-guard';
import { Sidebar } from '@/components/layout/sidebar';
import { Header } from '@/components/layout/header';

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AuthGuard>
      <div className="flex min-h-[100dvh] overflow-hidden">
        <Sidebar />
        <div className="flex flex-1 flex-col overflow-hidden">
          <Header />
          <main className="flex-1 overflow-y-auto">
            <div className="mx-auto max-w-[1400px] px-4 py-5 sm:px-6">
              {children}
            </div>
          </main>
        </div>
      </div>
    </AuthGuard>
  );
}
