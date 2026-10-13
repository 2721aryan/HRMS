package com.company.hrms.attendance.dto;

public record PunchRequest(
        String remarks,
        String source
) {}
