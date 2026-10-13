package com.company.hrms.attendance.controller;

import com.company.hrms.attendance.dto.*;
import com.company.hrms.attendance.service.AttendanceService;
import com.company.hrms.auth.security.UserDetailsImpl;
import com.company.hrms.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Attendance tracking and shift management endpoints")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/today")
    @Operation(summary = "Get current employee today's attendance status")
    public ResponseEntity<TodayAttendanceDto> getTodayAttendance(@AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(attendanceService.getTodayAttendance(principal.getEmployeeId()));
    }

    @PostMapping("/punch-in")
    @Operation(summary = "Clock in for the day")
    public ResponseEntity<TodayAttendanceDto> punchIn(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestBody(required = false) PunchRequest req
    ) {
        return ResponseEntity.ok(attendanceService.punchIn(principal.getEmployeeId(), req));
    }

    @PostMapping("/punch-out")
    @Operation(summary = "Clock out for the day")
    public ResponseEntity<TodayAttendanceDto> punchOut(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestBody(required = false) PunchRequest req
    ) {
        return ResponseEntity.ok(attendanceService.punchOut(principal.getEmployeeId(), req));
    }

    @GetMapping("/my-records")
    @Operation(summary = "Get current employee attendance records within date range")
    public ResponseEntity<List<AttendanceRecordDto>> getMyRecords(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(attendanceService.getEmployeeAttendance(principal.getEmployeeId(), from, to));
    }

    @GetMapping("/records")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Company attendance records with filters (Admin/HR/Manager)")
    public ResponseEntity<PageResponse<AttendanceRecordDto>> searchRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(attendanceService.searchAttendance(startDate, endDate, departmentId, status, page, size));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Get today's organization attendance metrics")
    public ResponseEntity<AttendanceStatsDto> getStats() {
        return ResponseEntity.ok(attendanceService.getTodayStats());
    }

    @PostMapping("/regularize")
    @Operation(summary = "Submit attendance regularization request")
    public ResponseEntity<AttendanceRegularizationDto> requestRegularization(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody RegularizationRequest req
    ) {
        return ResponseEntity.ok(attendanceService.requestRegularization(principal.getEmployeeId(), req));
    }

    @GetMapping("/my-regularizations")
    @Operation(summary = "Get current employee regularization requests")
    public ResponseEntity<List<AttendanceRegularizationDto>> getMyRegularizations(
            @AuthenticationPrincipal UserDetailsImpl principal
    ) {
        return ResponseEntity.ok(attendanceService.getEmployeeRegularizations(principal.getEmployeeId()));
    }

    @GetMapping("/regularizations")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "List all regularization requests (Admin/HR/Manager)")
    public ResponseEntity<PageResponse<AttendanceRegularizationDto>> getAllRegularizations(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(attendanceService.getAllRegularizations(status, page, size));
    }

    @PatchMapping("/regularizations/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Approve attendance regularization")
    public ResponseEntity<AttendanceRegularizationDto> approveRegularization(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String comment = body != null ? body.get("comment") : null;
        return ResponseEntity.ok(attendanceService.approveRegularization(id, principal.getEmployeeId(), comment));
    }

    @PatchMapping("/regularizations/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Reject attendance regularization")
    public ResponseEntity<AttendanceRegularizationDto> rejectRegularization(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String comment = body != null ? body.get("comment") : null;
        return ResponseEntity.ok(attendanceService.rejectRegularization(id, principal.getEmployeeId(), comment));
    }
}
