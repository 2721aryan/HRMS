package com.company.hrms.org.controller;

import com.company.hrms.org.dto.LocationDto;
import com.company.hrms.org.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Location organization endpoints")
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    @Operation(summary = "Get all active locations")
    public ResponseEntity<List<LocationDto>> getAllActive() {
        return ResponseEntity.ok(locationService.getAllActive());
    }
}
