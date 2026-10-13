package com.company.hrms.employee.dto;

import java.time.LocalDate;

public record EmployeeSummaryDto(
        Long id,
        String employeeCode,
        String firstName,
        String lastName,
        String fullName,
        String workEmail,
        String phone,
        Long departmentId,
        String departmentName,
        Long designationId,
        String designationTitle,
        String employmentType,
        String status,
        LocalDate dateOfJoining
) {}
