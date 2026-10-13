import { apiClient } from './client';

export interface TodayAttendance {
  hasCheckedIn: boolean;
  hasCheckedOut: boolean;
  checkIn: string | null;
  checkOut: string | null;
  workMinutes: number;
  status: string;
  late: boolean;
  shiftStart: string;
  shiftEnd: string;
  shiftName: string;
}

export interface AttendanceRecord {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  departmentName?: string;
  attendanceDate: string;
  checkIn: string | null;
  checkOut: string | null;
  workMinutes: number;
  status: 'PRESENT' | 'ABSENT' | 'HALF_DAY' | 'ON_LEAVE' | 'HOLIDAY' | 'WEEKLY_OFF';
  late: boolean;
  source: string;
  remarks?: string;
}

export interface AttendanceStats {
  date: string;
  presentCount: number;
  absentCount: number;
  onLeaveCount: number;
  halfDayCount: number;
  lateCount: number;
  totalEmployees: number;
}

export interface RegularizationItem {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  attendanceDate: string;
  requestedCheckIn: string | null;
  requestedCheckOut: string | null;
  reason: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
  approverId?: number;
  approverName?: string;
  decidedAt?: string;
  decisionComment?: string;
  createdAt: string;
}

export const attendanceApi = {
  getToday: async () => {
    const res = await apiClient.get<TodayAttendance>('/attendance/today');
    return res.data;
  },

  punchIn: async (remarks?: string) => {
    const res = await apiClient.post<TodayAttendance>('/attendance/punch-in', { remarks, source: 'WEB' });
    return res.data;
  },

  punchOut: async (remarks?: string) => {
    const res = await apiClient.post<TodayAttendance>('/attendance/punch-out', { remarks, source: 'WEB' });
    return res.data;
  },

  getMyRecords: async (from?: string, to?: string) => {
    const res = await apiClient.get<AttendanceRecord[]>('/attendance/my-records', {
      params: { from, to }
    });
    return res.data;
  },

  getRecords: async (params?: { startDate?: string; endDate?: string; departmentId?: number; status?: string; page?: number; size?: number }) => {
    const res = await apiClient.get<{ content: AttendanceRecord[]; totalElements: number }>('/attendance/records', {
      params
    });
    return res.data;
  },

  getStats: async () => {
    const res = await apiClient.get<AttendanceStats>('/attendance/stats');
    return res.data;
  },

  requestRegularization: async (data: { attendanceDate: string; requestedCheckIn?: string; requestedCheckOut?: string; reason: string }) => {
    const res = await apiClient.post<RegularizationItem>('/attendance/regularize', data);
    return res.data;
  },

  getMyRegularizations: async () => {
    const res = await apiClient.get<RegularizationItem[]>('/attendance/my-regularizations');
    return res.data;
  },

  getRegularizations: async (params?: { status?: string; page?: number; size?: number }) => {
    const res = await apiClient.get<{ content: RegularizationItem[]; totalElements: number }>('/attendance/regularizations', {
      params
    });
    return res.data;
  },

  approveRegularization: async (id: number, comment?: string) => {
    const res = await apiClient.patch<RegularizationItem>(`/attendance/regularizations/${id}/approve`, { comment });
    return res.data;
  },

  rejectRegularization: async (id: number, comment?: string) => {
    const res = await apiClient.patch<RegularizationItem>(`/attendance/regularizations/${id}/reject`, { comment });
    return res.data;
  }
};
