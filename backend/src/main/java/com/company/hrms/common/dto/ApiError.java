package com.company.hrms.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    @Builder.Default
    private final Instant timestamp = Instant.now();
    private final Map<String, String> validationErrors;
}
