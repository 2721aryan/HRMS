package com.company.hrms.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

public record RegularizationRequest(
        @NotNull LocalDate attendanceDate,
        Instant requestedCheckIn,
        Instant requestedCheckOut,
        @NotBlank @Size(max = 500) String reason
) {}
