import { apiClient } from './client';

export interface Department {
  id: number;
  code: string;
  name: string;
  parentId?: number | null;
  headEmployeeId?: number | null;
  active: boolean;
}

export interface Designation {
  id: number;
  title: string;
  grade?: string | null;
  active: boolean;
}

export interface Location {
  id: number;
  name: string;
  city?: string | null;
  state?: string | null;
  country: string;
  active: boolean;
}

export interface CreateDepartmentPayload {
  code: string;
  name: string;
  parentId?: number | null;
  headEmployeeId?: number | null;
}

export interface CreateDesignationPayload {
  title: string;
  grade?: string | null;
}

export async function fetchDepartments(activeOnly = false): Promise<Department[]> {
  const { data } = await apiClient.get<Department[]>('/departments', { params: { activeOnly } });
  return data;
}

export async function createDepartment(payload: CreateDepartmentPayload): Promise<Department> {
  const { data } = await apiClient.post<Department>('/departments', payload);
  return data;
}

export async function updateDepartment(id: number, payload: CreateDepartmentPayload): Promise<Department> {
  const { data } = await apiClient.put<Department>(`/departments/${id}`, payload);
  return data;
}

export async function toggleDepartment(id: number): Promise<Department> {
  const { data } = await apiClient.patch<Department>(`/departments/${id}/toggle`);
  return data;
}

export async function fetchDesignations(activeOnly = false): Promise<Designation[]> {
  const { data } = await apiClient.get<Designation[]>('/designations', { params: { activeOnly } });
  return data;
}

export async function createDesignation(payload: CreateDesignationPayload): Promise<Designation> {
  const { data } = await apiClient.post<Designation>('/designations', payload);
  return data;
}

export async function updateDesignation(id: number, payload: CreateDesignationPayload): Promise<Designation> {
  const { data } = await apiClient.put<Designation>(`/designations/${id}`, payload);
  return data;
}

export async function toggleDesignation(id: number): Promise<Designation> {
  const { data } = await apiClient.patch<Designation>(`/designations/${id}/toggle`);
  return data;
}

export async function fetchLocations(): Promise<Location[]> {
  const { data } = await apiClient.get<Location[]>('/locations');
  return data;
}
