package com.company.hrms.org.dto;

public record LocationDto(
        Long id,
        String name,
        String city,
        String state,
        String country,
        boolean active
) {}
