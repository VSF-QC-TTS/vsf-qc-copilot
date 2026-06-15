'use client';

import { AuthGuard } from '@/providers/auth-guard';
import { Sidebar } from '@/components/layout/sidebar';
import { Header } from '@/components/layout/header';
import { useSidebarStore } from '@/lib/store/sidebar-store';
import { cn } from '@/lib/utils';

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isCollapsed } = useSidebarStore();

  return (
    <AuthGuard>
      <div className="relative flex min-h-[100dvh] overflow-hidden">
        <Sidebar />
        <div className={cn(
          "flex flex-1 flex-col overflow-hidden transition-[margin] duration-300 ease-in-out",
          isCollapsed ? "lg:ml-16" : "lg:ml-60"
        )}>
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
