package com.company.hrms.org.dto;

public record DesignationDto(
        Long id,
        String title,
        String grade,
        boolean active
) {}
