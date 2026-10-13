import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { login as apiLogin, logout as apiLogout, refreshToken } from '../api/auth';
import { apiClient, setAccessToken } from '../api/client';
import { useAuthStore } from '../store/authStore';
import type { LoginPayload, LoginResult } from '../api/auth';
import type { AuthUser } from '../types';

interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  login: (payload: LoginPayload) => Promise<LoginResult>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [loading, setLoading] = useState(true);
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);

  // On mount: try silent refresh to restore session
  useEffect(() => {
    refreshToken()
      .then((token) => {
        setAccessToken(token);
        return apiClient.get('/auth/me');
      })
      .then(({ data }) => {
        if (data?.user) {
          setUser(data.user);
        } else if (data?.userId) {
          setUser({
            userId: data.userId,
            employeeId: data.employeeId,
            username: data.username,
            roles: data.roles,
            employeeCode: data.employeeCode,
            fullName: data.fullName,
          });
        }
      })
      .catch(() => { /* no active session — stay logged out */ })
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (payload: LoginPayload) => {
    const result = await apiLogin(payload);
    setAccessToken(result.accessToken);
    setUser(result.user);
    return result;
  }, []);

  const logout = useCallback(async () => {
    await apiLogout();
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
