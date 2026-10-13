package com.company.hrms.auth.dto;

import java.time.Instant;
import java.util.Set;

public record UserDto(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        String username,
        Set<String> roles,
        boolean active,
        Instant lastLoginAt,
        Instant createdAt
) {}
