import { create } from 'zustand';

type SidebarState = {
  isCollapsed: boolean;
  isMobileOpen: boolean;
  toggle: () => void;
  setCollapsed: (collapsed: boolean) => void;
  setMobileOpen: (open: boolean) => void;
};

export const useSidebarStore = create<SidebarState>((set) => {
  // Restore from localStorage on init (client-side only)
  const stored = typeof window !== 'undefined' ? localStorage.getItem('sidebar-collapsed') : null;
  return {
    isCollapsed: stored === 'true',
    isMobileOpen: false,
    toggle: () => set((s) => {
      const next = !s.isCollapsed;
      if (typeof window !== 'undefined') localStorage.setItem('sidebar-collapsed', String(next));
      return { isCollapsed: next };
    }),
    setCollapsed: (collapsed) => {
      if (typeof window !== 'undefined') localStorage.setItem('sidebar-collapsed', String(collapsed));
      set({ isCollapsed: collapsed });
    },
    setMobileOpen: (open) => set({ isMobileOpen: open }),
  };
});
