package com.company.hrms.leave.dto;

import java.math.BigDecimal;

public record LeaveBalanceDto(
        Long id,
        Long leaveTypeId,
        String leaveTypeCode,
        String leaveTypeName,
        int year,
        BigDecimal opening,
        BigDecimal accrued,
        BigDecimal adjusted,
        BigDecimal used,
        BigDecimal pending,
        BigDecimal available
) {}
