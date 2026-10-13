package com.company.hrms.attendance.dto;

import java.time.LocalDate;

public record AttendanceStatsDto(
        LocalDate date,
        long presentCount,
        long absentCount,
        long onLeaveCount,
        long halfDayCount,
        long lateCount,
        long totalEmployees
) {}
