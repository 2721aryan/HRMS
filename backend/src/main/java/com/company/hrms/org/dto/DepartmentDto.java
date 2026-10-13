package com.company.hrms.org.dto;

public record DepartmentDto(
        Long id,
        String code,
        String name,
        Long parentId,
        Long headEmployeeId,
        boolean active
) {}
