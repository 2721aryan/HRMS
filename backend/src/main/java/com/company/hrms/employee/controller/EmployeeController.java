package com.company.hrms.employee.controller;

import com.company.hrms.common.dto.PageResponse;
import com.company.hrms.employee.dto.*;
import com.company.hrms.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import com.company.hrms.auth.security.UserDetailsImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Employee Master endpoints")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @Operation(summary = "Search and filter employees (paginated)")
    public ResponseEntity<PageResponse<EmployeeSummaryDto>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection,
            @AuthenticationPrincipal UserDetailsImpl principal
    ) {
        return ResponseEntity.ok(employeeService.search(search, departmentId, status, page, size, sortBy, sortDirection, principal));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated employee profile")
    public ResponseEntity<EmployeeDto> getMyProfile(@AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(employeeService.getById(principal.getEmployeeId(), principal));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update personal contact and address details for own profile")
    public ResponseEntity<EmployeeDto> updateMyProfile(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody UpdateMyProfileRequest req
    ) {
        return ResponseEntity.ok(employeeService.updateMyProfile(principal.getEmployeeId(), req));
    }

    @GetMapping("/{id:[0-9]+}")
    @Operation(summary = "Get employee details by ID")
    public ResponseEntity<EmployeeDto> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl principal
    ) {
        return ResponseEntity.ok(employeeService.getById(id, principal));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Get employee count metrics and department breakdown")
    public ResponseEntity<EmployeeStatsDto> getStats(@AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(employeeService.getStats(principal));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Create a new employee")
    public ResponseEntity<EmployeeDto> create(@Valid @RequestBody CreateEmployeeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(req));
    }

    @PutMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Update employee details")
    public ResponseEntity<EmployeeDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest req
    ) {
        return ResponseEntity.ok(employeeService.update(id, req));
    }

    @PatchMapping("/{id:[0-9]+}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Update employee employment status (ACTIVE, ON_NOTICE, INACTIVE)")
    public ResponseEntity<EmployeeDto> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String status = body.getOrDefault("status", "ACTIVE");
        return ResponseEntity.ok(employeeService.updateStatus(id, status));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Export employee directory as Excel spreadsheet")
    public ResponseEntity<byte[]> exportEmployees() {
        byte[] excelBytes = employeeService.exportToExcel();
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employees.xlsx\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}
