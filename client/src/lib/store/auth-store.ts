import { create } from 'zustand';

type UserRole = 'QC_MEMBER' | 'QC_LEAD' | 'ADMIN';
type UserStatus = 'PENDING_EMAIL_VERIFICATION' | 'ACTIVE' | 'DISABLED';

type User = {
  publicId: string;
  email: string;
  displayName: string;
  role: UserRole;
  status: UserStatus;
  lastLoginAt: string | null;
};

type AuthState = {
  accessToken: string | null;
  user: User | null;
  isAuthenticated: boolean;
};

type AuthActions = {
  login: (accessToken: string, user: User) => void;
  logout: () => void;
  setToken: (accessToken: string) => void;
  setUser: (user: User) => void;
};

export type AuthStore = AuthState & AuthActions;

export const useAuthStore = create<AuthStore>((set) => ({
  accessToken: null,
  user: null,
  isAuthenticated: false,

  login: (accessToken, user) =>
    set({ accessToken, user, isAuthenticated: true }),

  logout: () =>
    set({ accessToken: null, user: null, isAuthenticated: false }),

  setToken: (accessToken) =>
    set({ accessToken, isAuthenticated: true }),

  setUser: (user) =>
    set({ user }),
}));
