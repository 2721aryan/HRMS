package com.company.hrms.org.controller;

import com.company.hrms.org.dto.CreateDesignationRequest;
import com.company.hrms.org.dto.DesignationDto;
import com.company.hrms.org.service.DesignationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/designations")
@RequiredArgsConstructor
@Tag(name = "Designations", description = "Designation organization endpoints")
public class DesignationController {

    private final DesignationService designationService;

    @GetMapping
    @Operation(summary = "Get all designations")
    public ResponseEntity<List<DesignationDto>> getAll(
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return ResponseEntity.ok(activeOnly ? designationService.getAllActive() : designationService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get designation by ID")
    public ResponseEntity<DesignationDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(designationService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Create designation (Admin/HR)")
    public ResponseEntity<DesignationDto> create(@Valid @RequestBody CreateDesignationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(designationService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Update designation (Admin/HR)")
    public ResponseEntity<DesignationDto> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateDesignationRequest req
    ) {
        return ResponseEntity.ok(designationService.update(id, req));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Toggle designation active status (Admin/HR)")
    public ResponseEntity<DesignationDto> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(designationService.toggleActive(id));
    }
}
