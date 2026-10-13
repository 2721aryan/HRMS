package com.company.hrms.auth.dto;

import java.util.List;

public record LoginResponse(
        String accessToken,
        Long userId,
        Long employeeId,
        String username,
        String fullName,
        String employeeCode,
        List<String> roles,
        boolean mustChangePassword
) {}
