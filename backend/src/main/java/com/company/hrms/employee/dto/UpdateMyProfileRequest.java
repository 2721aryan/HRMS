package com.company.hrms.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @Email(message = "Invalid personal email format")
        @Size(max = 150)
        String personalEmail,

        @Size(max = 20)
        String phone,

        @Size(max = 255)
        String addressLine,

        @Size(max = 80)
        String city,

        @Size(max = 80)
        String state,

        @Size(max = 12)
        String postalCode,

        @Size(max = 100)
        String emergencyContactName,

        @Size(max = 20)
        String emergencyContactPhone
) {}
