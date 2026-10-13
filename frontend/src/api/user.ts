import { apiClient } from './client';
import type { Role } from '../types';

export interface UserItem {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  username: string;
  roles: Role[];
  active: boolean;
  lastLoginAt: string | null;
  createdAt: string;
}

export interface CreateUserPayload {
  employeeId: number;
  username: string;
  password: string;
  roles: Role[];
}

export async function fetchUsers(): Promise<UserItem[]> {
  const { data } = await apiClient.get<UserItem[]>('/users');
  return data;
}

export async function createUser(payload: CreateUserPayload): Promise<UserItem> {
  const { data } = await apiClient.post<UserItem>('/users', payload);
  return data;
}

export async function updateUserRoles(userId: number, roles: Role[]): Promise<UserItem> {
  const { data } = await apiClient.put<UserItem>(`/users/${userId}/roles`, { roles });
  return data;
}

export async function toggleUserStatus(userId: number, active: boolean): Promise<UserItem> {
  const { data } = await apiClient.patch<UserItem>(`/users/${userId}/status`, { active });
  return data;
}

export async function resetUserPassword(userId: number, newPassword: string): Promise<void> {
  await apiClient.post(`/users/${userId}/reset-password`, { newPassword });
}
