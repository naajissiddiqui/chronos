import { create } from 'zustand';
import { User } from '@/types/auth';

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  setAuth: (user: User, token: string) => void;
  logout: () => void;
  initialize: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: null,
  isAuthenticated: false,
  isLoading: true,
  setAuth: (user, token) => {
    if (typeof window !== 'undefined') {
      localStorage.setItem('chronos_token', token);
      localStorage.setItem('chronos_user', JSON.stringify(user));
      if (user.organizationId) {
        localStorage.setItem('chronos_org_id', user.organizationId);
      }
    }
    set({ user, token, isAuthenticated: true, isLoading: false });
  },
  logout: () => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('chronos_token');
      localStorage.removeItem('chronos_user');
      localStorage.removeItem('chronos_org_id');
    }
    set({ user: null, token: null, isAuthenticated: false, isLoading: false });
  },
  initialize: () => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('chronos_token');
      const userStr = localStorage.getItem('chronos_user');
      if (token && userStr) {
        try {
          const user = JSON.parse(userStr);
          set({ user, token, isAuthenticated: true, isLoading: false });
          return;
        } catch {
          localStorage.removeItem('chronos_user');
          localStorage.removeItem('chronos_token');
        }
      }
    }
    set({ isLoading: false });
  },
}));
