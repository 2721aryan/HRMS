package com.company.hrms.employee.dto;

import java.util.Map;

public record EmployeeStatsDto(
        long total,
        long active,
        long onNotice,
        long inactive,
        Map<String, Long> byDepartment
) {}
