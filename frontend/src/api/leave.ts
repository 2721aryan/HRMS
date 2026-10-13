import { apiClient } from './client';

export interface LeaveType {
  id: number;
  code: string;
  name: string;
  paid: boolean;
  annualQuota: number;
  accrualType: string;
  allowHalfDay: boolean;
}

export interface LeaveBalance {
  id: number;
  leaveTypeId: number;
  leaveTypeCode: string;
  leaveTypeName: string;
  year: number;
  opening: number;
  accrued: number;
  adjusted: number;
  used: number;
  pending: number;
  available: number;
}

export interface LeaveRequestItem {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  departmentName?: string;
  leaveTypeId: number;
  leaveTypeCode: string;
  leaveTypeName: string;
  startDate: string;
  endDate: string;
  startDayPart: string;
  endDayPart: string;
  totalDays: number;
  reason?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
  approverId?: number;
  approverName?: string;
  decidedAt?: string;
  decisionComment?: string;
  createdAt: string;
}

export const leaveApi = {
  getTypes: async () => {
    const res = await apiClient.get<LeaveType[]>('/leaves/types');
    return res.data;
  },

  getMyBalances: async (year?: number) => {
    const res = await apiClient.get<LeaveBalance[]>('/leaves/balances', {
      params: { year }
    });
    return res.data;
  },

  getEmployeeBalances: async (employeeId: number, year?: number) => {
    const res = await apiClient.get<LeaveBalance[]>(`/leaves/balances/${employeeId}`, {
      params: { year }
    });
    return res.data;
  },

  apply: async (data: {
    leaveTypeId: number;
    startDate: string;
    endDate: string;
    startDayPart?: string;
    endDayPart?: string;
    reason?: string;
  }) => {
    const res = await apiClient.post<LeaveRequestItem>('/leaves/apply', data);
    return res.data;
  },

  getMyRequests: async () => {
    const res = await apiClient.get<LeaveRequestItem[]>('/leaves/my-requests');
    return res.data;
  },

  getRequests: async (params?: { status?: string; departmentId?: number; page?: number; size?: number }) => {
    const res = await apiClient.get<{ content: LeaveRequestItem[]; totalElements: number }>('/leaves/requests', {
      params
    });
    return res.data;
  },

  approve: async (id: number, comment?: string) => {
    const res = await apiClient.patch<LeaveRequestItem>(`/leaves/requests/${id}/approve`, { comment });
    return res.data;
  },

  reject: async (id: number, comment?: string) => {
    const res = await apiClient.patch<LeaveRequestItem>(`/leaves/requests/${id}/reject`, { comment });
    return res.data;
  },

  cancel: async (id: number) => {
    const res = await apiClient.patch<LeaveRequestItem>(`/leaves/requests/${id}/cancel`);
    return res.data;
  }
};
