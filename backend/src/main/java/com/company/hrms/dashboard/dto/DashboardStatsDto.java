package com.company.hrms.dashboard.dto;

import java.util.Map;

public record DashboardStatsDto(
        String scope, // "PERSONAL", "TEAM", "COMPANY"

        // PERSONAL fields
        String todayAttendanceStatus,
        Long monthPresentDays,
        Double remainingLeaveDays,
        Long myPendingRequestsCount,

        // TEAM fields
        Long teamSize,
        Long teamPresentToday,
        Long teamOnLeaveToday,
        Long teamPendingApprovals,

        // COMPANY fields
        Long totalEmployees,
        Long activeEmployees,
        Long companyPresentToday,
        Long companyOnLeaveToday,
        Long companyPendingApprovals,
        Map<String, Long> departmentBreakdown
) {}
