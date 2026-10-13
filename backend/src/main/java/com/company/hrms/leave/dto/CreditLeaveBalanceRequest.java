package com.company.hrms.leave.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreditLeaveBalanceRequest(
        @NotNull(message = "Employee ID is required")
        Long employeeId,

        @NotNull(message = "Leave Type ID is required")
        Long leaveTypeId,

        Short year,

        @NotNull(message = "Adjustment days is required")
        BigDecimal days,

        @Size(max = 255)
        String remarks
) {}
