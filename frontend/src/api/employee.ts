import { apiClient } from './client';
import type { PaginatedResponse } from '../types';

export interface EmployeeSummary {
  id: number;
  employeeCode: string;
  firstName: string;
  lastName?: string | null;
  fullName: string;
  workEmail: string;
  phone?: string | null;
  departmentId?: number | null;
  departmentName?: string | null;
  designationId?: number | null;
  designationTitle?: string | null;
  employmentType: string;
  status: string;
  dateOfJoining: string;
}

export interface EmployeeDetail extends EmployeeSummary {
  personalEmail?: string | null;
  dateOfBirth?: string | null;
  gender?: string | null;
  maritalStatus?: string | null;
  addressLine?: string | null;
  city?: string | null;
  state?: string | null;
  postalCode?: string | null;
  emergencyContactName?: string | null;
  emergencyContactPhone?: string | null;
  locationId?: number | null;
  locationName?: string | null;
  managerId?: number | null;
  managerName?: string | null;
  probationEndDate?: string | null;
  dateOfExit?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateEmployeePayload {
  employeeCode?: string;
  firstName: string;
  lastName?: string;
  workEmail: string;
  personalEmail?: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: string;
  maritalStatus?: string;
  addressLine?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  departmentId?: number;
  designationId?: number;
  locationId?: number;
  managerId?: number;
  employmentType?: string;
  dateOfJoining: string;
  probationEndDate?: string;
}

export interface EmployeeStats {
  total: number;
  active: number;
  onNotice: number;
  inactive: number;
  byDepartment: Record<string, number>;
}

export interface EmployeeSearchParams {
  search?: string;
  departmentId?: number;
  status?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

export async function fetchEmployees(params: EmployeeSearchParams): Promise<PaginatedResponse<EmployeeSummary>> {
  const { data } = await apiClient.get<PaginatedResponse<EmployeeSummary>>('/employees', { params });
  return data;
}

export async function fetchEmployeeById(id: number): Promise<EmployeeDetail> {
  const { data } = await apiClient.get<EmployeeDetail>(`/employees/${id}`);
  return data;
}

export async function createEmployee(payload: CreateEmployeePayload): Promise<EmployeeDetail> {
  const { data } = await apiClient.post<EmployeeDetail>('/employees', payload);
  return data;
}

export async function updateEmployee(id: number, payload: Partial<CreateEmployeePayload>): Promise<EmployeeDetail> {
  const { data } = await apiClient.put<EmployeeDetail>(`/employees/${id}`, payload);
  return data;
}

export async function updateEmployeeStatus(id: number, status: string): Promise<EmployeeDetail> {
  const { data } = await apiClient.patch<EmployeeDetail>(`/employees/${id}/status`, { status });
  return data;
}

export async function fetchEmployeeStats(): Promise<EmployeeStats> {
  const { data } = await apiClient.get<EmployeeStats>('/employees/stats');
  return data;
}

export async function fetchMyProfile(): Promise<EmployeeDetail> {
  const { data } = await apiClient.get<EmployeeDetail>('/employees/me');
  return data;
}

export async function updateMyProfile(payload: {
  personalEmail?: string;
  phone?: string;
  addressLine?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
}): Promise<EmployeeDetail> {
  const { data } = await apiClient.patch<EmployeeDetail>('/employees/me', payload);
  return data;
}
