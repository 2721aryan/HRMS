package com.company.hrms.attendance.dto;

import java.time.Instant;
import java.time.LocalTime;

public record TodayAttendanceDto(
        boolean hasCheckedIn,
        boolean hasCheckedOut,
        Instant checkIn,
        Instant checkOut,
        int workMinutes,
        String status,
        boolean late,
        LocalTime shiftStart,
        LocalTime shiftEnd,
        String shiftName
) {}
