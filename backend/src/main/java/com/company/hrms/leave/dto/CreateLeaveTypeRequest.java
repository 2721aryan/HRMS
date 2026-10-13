package com.company.hrms.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateLeaveTypeRequest(
        @NotBlank(message = "Leave code is required")
        @Size(max = 20)
        String code,

        @NotBlank(message = "Leave name is required")
        @Size(max = 80)
        String name,

        boolean paid,

        @NotNull(message = "Annual quota is required")
        @PositiveOrZero(message = "Annual quota must be 0 or positive")
        BigDecimal annualQuota,

        @NotBlank(message = "Accrual type is required")
        String accrualType,

        boolean allowHalfDay
) {}
