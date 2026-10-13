import { apiClient } from './client';

export interface DashboardStats {
  scope: 'PERSONAL' | 'TEAM' | 'COMPANY';
  todayAttendanceStatus?: string;
  monthPresentDays?: number;
  remainingLeaveDays?: number;
  myPendingRequestsCount?: number;
  teamSize?: number;
  teamPresentToday?: number;
  teamOnLeaveToday?: number;
  teamPendingApprovals?: number;
  totalEmployees?: number;
  activeEmployees?: number;
  companyPresentToday?: number;
  companyOnLeaveToday?: number;
  companyPendingApprovals?: number;
  departmentBreakdown?: Record<string, number>;
}

export async function fetchDashboardStats(): Promise<DashboardStats> {
  const { data } = await apiClient.get('/dashboard/stats');
  return data;
}
