package com.company.hrms.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateEmployeeRequest(
        String employeeCode,
        @NotBlank @Size(max = 80) String firstName,
        @Size(max = 80) String lastName,
        @NotBlank @Email @Size(max = 150) String workEmail,
        @Email @Size(max = 150) String personalEmail,
        @Size(max = 20) String phone,
        LocalDate dateOfBirth,
        String gender,
        String maritalStatus,
        String addressLine,
        String city,
        String state,
        String postalCode,
        String emergencyContactName,
        String emergencyContactPhone,
        Long departmentId,
        Long designationId,
        Long locationId,
        Long managerId,
        String employmentType,
        @NotNull LocalDate dateOfJoining,
        LocalDate probationEndDate
) {}
