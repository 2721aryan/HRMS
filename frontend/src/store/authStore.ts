import { create } from 'zustand';
import type { AuthUser } from '../types';

interface AuthState {
  user: AuthUser | null;
  sidebarCollapsed: boolean;
  setUser: (user: AuthUser | null) => void;
  toggleSidebar: () => void;
  hasRole: (...roles: string[]) => boolean;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  sidebarCollapsed: false,
  setUser: (user) => set({ user }),
  toggleSidebar: () => set((s) => ({ sidebarCollapsed: !s.sidebarCollapsed })),
  hasRole: (...roles) => {
    const { user } = get();
    return !!user && roles.some((r) => user.roles.includes(r as any));
  },
}));
