package com.company.hrms.attendance.dto;

import java.time.Instant;
import java.time.LocalDate;

public record AttendanceRegularizationDto(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        LocalDate attendanceDate,
        Instant requestedCheckIn,
        Instant requestedCheckOut,
        String reason,
        String status,
        Long approverId,
        String approverName,
        Instant decidedAt,
        String decisionComment,
        Instant createdAt
) {}
