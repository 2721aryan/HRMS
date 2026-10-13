package com.company.hrms.org.controller;

import com.company.hrms.org.dto.CreateDepartmentRequest;
import com.company.hrms.org.dto.DepartmentDto;
import com.company.hrms.org.service.DepartmentService;
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
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "Department organization endpoints")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @Operation(summary = "Get all active departments")
    public ResponseEntity<List<DepartmentDto>> getAllActive(
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return ResponseEntity.ok(activeOnly ? departmentService.getAllActive() : departmentService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID")
    public ResponseEntity<DepartmentDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Create department (Admin/HR)")
    public ResponseEntity<DepartmentDto> create(@Valid @RequestBody CreateDepartmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Update department (Admin/HR)")
    public ResponseEntity<DepartmentDto> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateDepartmentRequest req
    ) {
        return ResponseEntity.ok(departmentService.update(id, req));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Toggle department active status (Admin/HR)")
    public ResponseEntity<DepartmentDto> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.toggleActive(id));
    }
}
