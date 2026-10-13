import { apiClient, setAccessToken } from './client';
import type { AuthUser } from '../types';

export interface LoginPayload { username: string; password: string; }
export interface LoginResult { accessToken: string; user: AuthUser; mustChangePassword: boolean; }

export async function login(payload: LoginPayload): Promise<LoginResult> {
  const { data } = await apiClient.post('/auth/login', payload);
  return {
    accessToken: data.accessToken,
    mustChangePassword: data.mustChangePassword,
    user: {
      userId: data.userId,
      employeeId: data.employeeId,
      username: data.username,
      roles: data.roles,
      employeeCode: data.employeeCode,
      fullName: data.fullName,
    },
  };
}

export async function refreshToken(): Promise<string> {
  const { data } = await apiClient.post('/auth/refresh');
  return data.accessToken;
}

export async function logout(): Promise<void> {
  await apiClient.post('/auth/logout');
  setAccessToken(null);
}

export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  await apiClient.post('/auth/change-password', { currentPassword, newPassword });
}
