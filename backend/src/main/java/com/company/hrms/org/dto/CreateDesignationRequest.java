package com.company.hrms.org.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDesignationRequest(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 20) String grade
) {}
