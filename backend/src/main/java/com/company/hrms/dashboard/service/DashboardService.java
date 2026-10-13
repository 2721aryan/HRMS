package com.company.hrms.dashboard.service;

import com.company.hrms.attendance.repository.AttendanceRecordRepository;
import com.company.hrms.attendance.repository.AttendanceRegularizationRepository;
import com.company.hrms.auth.security.UserDetailsImpl;
import com.company.hrms.dashboard.dto.DashboardStatsDto;
import com.company.hrms.employee.repository.EmployeeRepository;
import com.company.hrms.leave.entity.LeaveBalance;
import com.company.hrms.leave.repository.LeaveBalanceRepository;
import com.company.hrms.leave.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceRegularizationRepository regularizationRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats(UserDetailsImpl principal) {
        boolean isHrOrAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));
        boolean isManager = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

        LocalDate today = LocalDate.now(ZONE);
        short currentYear = (short) today.getYear();

        if (isHrOrAdmin) {
            long total = employeeRepository.count();
            long active = employeeRepository.countByStatus("ACTIVE");
            long present = attendanceRecordRepository.countByAttendanceDateAndStatus(today, "PRESENT");
            long onLeave = leaveRequestRepository.countCompanyOnLeaveOnDate(today);
            long pendingLeave = leaveRequestRepository.countByStatus("PENDING");
            long pendingReg = regularizationRepository.countByStatus("PENDING");

            Map<String, Long> byDept = new LinkedHashMap<>();
            List<Object[]> rows = employeeRepository.countActiveEmployeesByDepartment();
            for (Object[] r : rows) {
                if (r[0] != null && r[1] != null) {
                    byDept.put((String) r[0], ((Number) r[1]).longValue());
                }
            }

            return new DashboardStatsDto(
                    "COMPANY",
                    null, null, null, null,
                    null, null, null, null,
                    total, active, present, onLeave, pendingLeave + pendingReg, byDept
            );
        } else if (isManager) {
            Long managerId = principal.getEmployeeId();
            long teamSize = employeeRepository.countByManagerIdAndStatus(managerId, "ACTIVE");
            long teamPresent = attendanceRecordRepository.countByManagerAndAttendanceDateAndStatus(managerId, today, "PRESENT");
            long teamOnLeave = leaveRequestRepository.countTeamOnLeaveOnDate(managerId, today);
            long pendingLeave = leaveRequestRepository.countByManagerAndStatus(managerId, "PENDING");
            long pendingReg = regularizationRepository.countByManagerAndStatus(managerId, "PENDING");

            return new DashboardStatsDto(
                    "TEAM",
                    null, null, null, null,
                    teamSize, teamPresent, teamOnLeave, pendingLeave + pendingReg,
                    null, null, null, null, null, null
            );
        } else {
            // Employee personal scope
            Long empId = principal.getEmployeeId();
            String todayStatus = attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(empId, today)
                    .map(r -> r.getStatus())
                    .orElse("NOT_MARKED");

            LocalDate firstDayOfMonth = today.withDayOfMonth(1);
            long presentDays = attendanceRecordRepository.countByEmployeeIdAndAttendanceDateBetweenAndStatus(empId, firstDayOfMonth, today, "PRESENT")
                    + attendanceRecordRepository.countByEmployeeIdAndAttendanceDateBetweenAndStatus(empId, firstDayOfMonth, today, "HALF_DAY");

            List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeIdAndYear(empId, currentYear);
            double totalLeaveAvailable = balances.stream()
                    .map(LeaveBalance::computeAvailable)
                    .mapToDouble(BigDecimal::doubleValue)
                    .sum();

            long pendingLeave = leaveRequestRepository.countByEmployeeIdAndStatus(empId, "PENDING");
            long pendingReg = regularizationRepository.countByEmployeeIdAndStatus(empId, "PENDING");

            return new DashboardStatsDto(
                    "PERSONAL",
                    todayStatus, presentDays, totalLeaveAvailable, pendingLeave + pendingReg,
                    null, null, null, null,
                    null, null, null, null, null, null
            );
        }
    }
}
