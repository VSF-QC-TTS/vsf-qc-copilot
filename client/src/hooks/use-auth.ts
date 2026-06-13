import { useAuthStore } from '@/lib/store/auth-store';

export function useAuth() {
  const { accessToken, user, isAuthenticated, login, logout, setToken, setUser } = useAuthStore();
  return { accessToken, user, isAuthenticated, login, logout, setToken, setUser };
}
