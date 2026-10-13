package com.company.hrms.leave.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ApplyLeaveRequest(
        @NotNull Long leaveTypeId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String startDayPart, // FULL_DAY, FIRST_HALF, SECOND_HALF
        String endDayPart,   // FULL_DAY, FIRST_HALF, SECOND_HALF
        @Size(max = 500) String reason
) {}
