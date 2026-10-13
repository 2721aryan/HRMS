package com.company.hrms.leave.dto;

import java.math.BigDecimal;

public record LeaveTypeDto(
        Long id,
        String code,
        String name,
        boolean paid,
        BigDecimal annualQuota,
        String accrualType,
        boolean allowHalfDay
) {}
