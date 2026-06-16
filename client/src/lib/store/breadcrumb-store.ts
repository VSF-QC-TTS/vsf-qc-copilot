import { create } from 'zustand';

interface BreadcrumbState {
  mapping: Record<string, string>;
  setMapping: (id: string, name: string) => void;
}

export const useBreadcrumbStore = create<BreadcrumbState>((set) => ({
  mapping: {},
  setMapping: (id, name) =>
    set((state) => {
      // Prevent unnecessary updates
      if (state.mapping[id] === name) return state;
      return {
        mapping: {
          ...state.mapping,
          [id]: name,
        },
      };
    }),
}));
