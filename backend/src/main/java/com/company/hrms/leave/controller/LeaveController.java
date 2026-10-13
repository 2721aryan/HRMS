package com.company.hrms.leave.controller;

import com.company.hrms.auth.security.UserDetailsImpl;
import com.company.hrms.common.dto.PageResponse;
import com.company.hrms.leave.dto.*;
import com.company.hrms.leave.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
@Tag(name = "Leaves", description = "Leave balance, application, and approval workflows")
public class LeaveController {

    private final LeaveService leaveService;

    @GetMapping("/types")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get list of active leave policy types")
    public ResponseEntity<List<LeaveTypeDto>> getLeaveTypes() {
        return ResponseEntity.ok(leaveService.getLeaveTypes());
    }

    @GetMapping("/balances")
    @Operation(summary = "Get current employee leave balances")
    public ResponseEntity<List<LeaveBalanceDto>> getMyBalances(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(leaveService.getEmployeeBalances(principal.getEmployeeId(), year));
    }

    @GetMapping("/balances/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Get specific employee leave balances (Admin/HR/Manager)")
    public ResponseEntity<List<LeaveBalanceDto>> getEmployeeBalances(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(leaveService.getEmployeeBalances(employeeId, year));
    }

    @PostMapping("/apply")
    @Operation(summary = "Apply for leave")
    public ResponseEntity<LeaveRequestDto> applyLeave(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody ApplyLeaveRequest req
    ) {
        return ResponseEntity.ok(leaveService.applyLeave(principal.getEmployeeId(), req));
    }

    @GetMapping("/my-requests")
    @Operation(summary = "Get current employee leave application history")
    public ResponseEntity<List<LeaveRequestDto>> getMyRequests(
            @AuthenticationPrincipal UserDetailsImpl principal
    ) {
        return ResponseEntity.ok(leaveService.getMyRequests(principal.getEmployeeId()));
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Search all organization leave requests (Admin/HR/Manager)")
    public ResponseEntity<PageResponse<LeaveRequestDto>> searchRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(leaveService.searchLeaveRequests(status, departmentId, page, size));
    }

    @PatchMapping("/requests/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Approve leave request")
    public ResponseEntity<LeaveRequestDto> approveLeave(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String comment = body != null ? body.get("comment") : null;
        return ResponseEntity.ok(leaveService.approveLeave(id, principal.getEmployeeId(), comment));
    }

    @PatchMapping("/requests/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Reject leave request")
    public ResponseEntity<LeaveRequestDto> rejectLeave(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String comment = body != null ? body.get("comment") : null;
        return ResponseEntity.ok(leaveService.rejectLeave(id, principal.getEmployeeId(), comment));
    }

    @PatchMapping("/requests/{id}/cancel")
    @Operation(summary = "Cancel own leave request")
    public ResponseEntity<LeaveRequestDto> cancelLeave(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl principal
    ) {
        return ResponseEntity.ok(leaveService.cancelLeave(id, principal.getEmployeeId()));
    }
}
