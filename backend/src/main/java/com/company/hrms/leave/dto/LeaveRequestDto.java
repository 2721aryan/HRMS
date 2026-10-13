package com.company.hrms.leave.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LeaveRequestDto(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        String departmentName,
        Long leaveTypeId,
        String leaveTypeCode,
        String leaveTypeName,
        LocalDate startDate,
        LocalDate endDate,
        String startDayPart,
        String endDayPart,
        BigDecimal totalDays,
        String reason,
        String status,
        Long approverId,
        String approverName,
        Instant decidedAt,
        String decisionComment,
        Instant createdAt
) {}
