package com.company.hrms.attendance.dto;

import java.time.Instant;
import java.time.LocalDate;

public record AttendanceRecordDto(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        String departmentName,
        LocalDate attendanceDate,
        Instant checkIn,
        Instant checkOut,
        int workMinutes,
        String status,
        boolean late,
        String source,
        String remarks
) {}
