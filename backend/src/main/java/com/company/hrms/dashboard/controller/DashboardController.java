package com.company.hrms.dashboard.controller;

import com.company.hrms.auth.security.UserDetailsImpl;
import com.company.hrms.dashboard.dto.DashboardStatsDto;
import com.company.hrms.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Role-scoped dashboard analytics")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard metrics scoped to user role (Personal, Team, or Company)")
    public ResponseEntity<DashboardStatsDto> getStats(@AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(dashboardService.getDashboardStats(principal));
    }
}
