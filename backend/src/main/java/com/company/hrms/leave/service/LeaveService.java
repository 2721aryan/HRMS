package com.company.hrms.leave.service;

import com.company.hrms.attendance.entity.AttendanceRecord;
import com.company.hrms.attendance.repository.AttendanceRecordRepository;
import com.company.hrms.common.dto.PageResponse;
import com.company.hrms.common.exception.BusinessException;
import com.company.hrms.common.exception.ResourceNotFoundException;
import com.company.hrms.employee.entity.Employee;
import com.company.hrms.employee.repository.EmployeeRepository;
import com.company.hrms.leave.dto.*;
import com.company.hrms.leave.entity.*;
import com.company.hrms.leave.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.company.hrms.auth.security.UserDetailsImpl;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceTransactionRepository transactionRepository;
    private final HolidayRepository holidayRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    @Transactional(readOnly = true)
    public List<LeaveTypeDto> getLeaveTypes() {
        return leaveTypeRepository.findByActiveTrue().stream()
                .map(t -> new LeaveTypeDto(
                        t.getId(),
                        t.getCode(),
                        t.getName(),
                        t.isPaid(),
                        t.getAnnualQuota(),
                        t.getAccrualType(),
                        t.isAllowHalfDay()
                ))
                .toList();
    }

    @Transactional
    public List<LeaveBalanceDto> getEmployeeBalances(Long employeeId, Integer year, UserDetailsImpl principal) {
        boolean isSelf = (principal != null && principal.getEmployeeId() != null && principal.getEmployeeId().equals(employeeId));
        boolean isHrOrAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));
        boolean isManager = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        if (!isSelf && !isHrOrAdmin) {
            if (isManager) {
                if (emp.getManager() == null || !emp.getManager().getId().equals(principal.getEmployeeId())) {
                    throw new AccessDeniedException("Managers can only view leave balances for their direct team members");
                }
            } else {
                throw new AccessDeniedException("Employees can only view their own leave balances");
            }
        }

        short targetYear = (short) ((year != null) ? year : LocalDate.now().getYear());
        List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, targetYear);

        // Auto-seed annual balances if missing
        if (balances.isEmpty()) {
            List<LeaveType> types = leaveTypeRepository.findByActiveTrue();
            balances = new ArrayList<>();
            for (LeaveType type : types) {
                LeaveBalance b = LeaveBalance.builder()
                        .employee(emp)
                        .leaveType(type)
                        .year(targetYear)
                        .opening(BigDecimal.ZERO)
                        .accrued(type.getAnnualQuota())
                        .adjusted(BigDecimal.ZERO)
                        .used(BigDecimal.ZERO)
                        .pending(BigDecimal.ZERO)
                        .build();
                balances.add(leaveBalanceRepository.save(b));

                if (type.getAnnualQuota().compareTo(BigDecimal.ZERO) > 0) {
                    transactionRepository.save(LeaveBalanceTransaction.builder()
                            .leaveBalance(b)
                            .txnType("ACCRUAL")
                            .days(type.getAnnualQuota())
                            .remarks("Yearly quota allotment for " + targetYear)
                            .build());
                }
            }
        }

        return balances.stream().map(this::toBalanceDto).toList();
    }

    public BigDecimal calculateWorkingDays(LocalDate start, LocalDate end, String startPart, String endPart) {
        if (end.isBefore(start)) {
            throw new BusinessException("End date cannot be before start date");
        }

        BigDecimal days = BigDecimal.ZERO;
        LocalDate curr = start;

        while (!curr.isAfter(end)) {
            DayOfWeek dow = curr.getDayOfWeek();
            // Sat and Sun are non-working days
            boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
            boolean isHoliday = holidayRepository.existsByHolidayDate(curr);

            if (!isWeekend && !isHoliday) {
                if (curr.equals(start) && curr.equals(end)) {
                    if ("FIRST_HALF".equals(startPart) || "SECOND_HALF".equals(startPart) ||
                        "FIRST_HALF".equals(endPart) || "SECOND_HALF".equals(endPart)) {
                        days = days.add(new BigDecimal("0.5"));
                    } else {
                        days = days.add(BigDecimal.ONE);
                    }
                } else if (curr.equals(start) && ("FIRST_HALF".equals(startPart) || "SECOND_HALF".equals(startPart))) {
                    days = days.add(new BigDecimal("0.5"));
                } else if (curr.equals(end) && ("FIRST_HALF".equals(endPart) || "SECOND_HALF".equals(endPart))) {
                    days = days.add(new BigDecimal("0.5"));
                } else {
                    days = days.add(BigDecimal.ONE);
                }
            }
            curr = curr.plusDays(1);
        }

        if (days.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Selected date range contains no working days (all weekends or company holidays).");
        }

        return days;
    }

    @Transactional
    public LeaveRequestDto applyLeave(Long employeeId, ApplyLeaveRequest req) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        if (leaveRequestRepository.hasOverlappingLeave(employeeId, req.startDate(), req.endDate())) {
            throw new BusinessException("You already have a pending or approved leave request overlapping these dates.");
        }

        LeaveType leaveType = leaveTypeRepository.findById(req.leaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("LeaveType", "id", req.leaveTypeId()));

        String startPart = req.startDayPart() != null ? req.startDayPart() : "FULL_DAY";
        String endPart = req.endDayPart() != null ? req.endDayPart() : "FULL_DAY";

        BigDecimal totalDays = calculateWorkingDays(req.startDate(), req.endDate(), startPart, endPart);

        // Fetch or create balance for this leave type in current year.
        // Use a plain fetch first to check existence, then re-fetch with lock.
        short year = (short) req.startDate().getYear();
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(employeeId, leaveType.getId(), year)
                .orElseGet(() -> {
                    LeaveBalance b = LeaveBalance.builder()
                            .employee(emp)
                            .leaveType(leaveType)
                            .year(year)
                            .opening(BigDecimal.ZERO)
                            .accrued(leaveType.getAnnualQuota())
                            .adjusted(BigDecimal.ZERO)
                            .used(BigDecimal.ZERO)
                            .pending(BigDecimal.ZERO)
                            .build();
                    return leaveBalanceRepository.save(b);
                });

        // Re-fetch with PESSIMISTIC_WRITE lock to prevent concurrent double-spend.
        // Any second concurrent request will block here until the first transaction commits.
        balance = leaveBalanceRepository.findByEmployeeAndTypeAndYearForUpdate(employeeId, leaveType.getId(), year)
                .orElseThrow(() -> new BusinessException("Leave balance not found"));

        // Validate quota unless Loss of Pay (LOP)
        if (!"LOP".equalsIgnoreCase(leaveType.getCode())) {
            BigDecimal available = balance.computeAvailable();
            if (available.compareTo(totalDays) < 0) {
                throw new BusinessException("Insufficient " + leaveType.getName() + " balance. Available: " +
                        available + " days, requested: " + totalDays + " days.");
            }
        }

        // Put days into pending
        balance.setPending(balance.getPending().add(totalDays));
        leaveBalanceRepository.save(balance);

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(emp)
                .leaveType(leaveType)
                .startDate(req.startDate())
                .endDate(req.endDate())
                .startDayPart(startPart)
                .endDayPart(endPart)
                .totalDays(totalDays)
                .reason(req.reason())
                .status("PENDING")
                .createdBy(employeeId)
                .build();

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        transactionRepository.save(LeaveBalanceTransaction.builder()
                .leaveBalance(balance)
                .txnType("REQUEST")
                .days(totalDays.negate())
                .leaveRequest(saved)
                .remarks("Leave request #" + saved.getId() + " submitted")
                .createdBy(employeeId)
                .build());

        return toRequestDto(saved);
    }

    @Transactional
    public LeaveRequestDto approveLeave(Long leaveRequestId, Long approverId, String comment, UserDetailsImpl principal) {
        LeaveRequest req = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", leaveRequestId));

        if (!"PENDING".equals(req.getStatus())) {
            throw new BusinessException("Cannot approve leave request with status: " + req.getStatus());
        }

        boolean isHrOrAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));

        if (!isHrOrAdmin) {
            if (req.getEmployee().getManager() == null || !req.getEmployee().getManager().getId().equals(approverId)) {
                throw new AccessDeniedException("Managers can only approve leave requests for their direct team members");
            }
        }

        Employee approver = employeeRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", approverId));

        short year = (short) req.getStartDate().getYear();
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(
                req.getEmployee().getId(), req.getLeaveType().getId(), year
        ).orElseThrow(() -> new BusinessException("Leave balance not found for employee"));

        // Move from pending to used
        balance.setPending(balance.getPending().subtract(req.getTotalDays()));
        balance.setUsed(balance.getUsed().add(req.getTotalDays()));
        leaveBalanceRepository.save(balance);

        req.setStatus("APPROVED");
        req.setApprover(approver);
        req.setDecidedAt(Instant.now());
        req.setDecisionComment(comment);
        req.setUpdatedBy(approverId);
        leaveRequestRepository.save(req);

        transactionRepository.save(LeaveBalanceTransaction.builder()
                .leaveBalance(balance)
                .txnType("APPROVAL")
                .days(req.getTotalDays().negate())
                .leaveRequest(req)
                .remarks("Approved by " + approver.getFullName())
                .createdBy(approverId)
                .build());

        // Update attendance records for the dates of leave
        LocalDate curr = req.getStartDate();
        while (!curr.isAfter(req.getEndDate())) {
            DayOfWeek dow = curr.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                final LocalDate d = curr;
                AttendanceRecord att = attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(req.getEmployee().getId(), d)
                        .orElseGet(() -> AttendanceRecord.builder()
                                .employee(req.getEmployee())
                                .attendanceDate(d)
                                .build());
                att.setStatus("ON_LEAVE");
                att.setLeaveRequestId(req.getId());
                att.setRemarks("On " + req.getLeaveType().getName());
                attendanceRecordRepository.save(att);
            }
            curr = curr.plusDays(1);
        }

        return toRequestDto(req);
    }

    @Transactional
    public LeaveRequestDto rejectLeave(Long leaveRequestId, Long approverId, String comment, UserDetailsImpl principal) {
        LeaveRequest req = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", leaveRequestId));

        if (!"PENDING".equals(req.getStatus())) {
            throw new BusinessException("Cannot reject leave request with status: " + req.getStatus());
        }

        boolean isHrOrAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));

        if (!isHrOrAdmin) {
            if (req.getEmployee().getManager() == null || !req.getEmployee().getManager().getId().equals(approverId)) {
                throw new AccessDeniedException("Managers can only reject leave requests for their direct team members");
            }
        }

        Employee approver = employeeRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", approverId));

        short year = (short) req.getStartDate().getYear();
        leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(
                req.getEmployee().getId(), req.getLeaveType().getId(), year
        ).ifPresent(balance -> {
            balance.setPending(balance.getPending().subtract(req.getTotalDays()));
            leaveBalanceRepository.save(balance);

            transactionRepository.save(LeaveBalanceTransaction.builder()
                    .leaveBalance(balance)
                    .txnType("REJECTION")
                    .days(req.getTotalDays())
                    .leaveRequest(req)
                    .remarks("Rejected by " + approver.getFullName() + (comment != null ? ": " + comment : ""))
                    .createdBy(approverId)
                    .build());
        });

        req.setStatus("REJECTED");
        req.setApprover(approver);
        req.setDecidedAt(Instant.now());
        req.setDecisionComment(comment);
        req.setUpdatedBy(approverId);
        leaveRequestRepository.save(req);

        return toRequestDto(req);
    }

    @Transactional
    public LeaveRequestDto cancelLeave(Long leaveRequestId, Long employeeId) {
        LeaveRequest req = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", leaveRequestId));

        // Issue #7 fix: ownership check — employees can only cancel their OWN requests
        if (!req.getEmployee().getId().equals(employeeId)) {
            throw new AccessDeniedException("You can only cancel your own leave requests");
        }

        if (!"PENDING".equals(req.getStatus()) && !"APPROVED".equals(req.getStatus())) {
            throw new BusinessException("Cannot cancel leave with status: " + req.getStatus());
        }

        short year = (short) req.getStartDate().getYear();
        leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(
                req.getEmployee().getId(), req.getLeaveType().getId(), year
        ).ifPresent(balance -> {
            if ("PENDING".equals(req.getStatus())) {
                balance.setPending(balance.getPending().subtract(req.getTotalDays()));
            } else if ("APPROVED".equals(req.getStatus())) {
                balance.setUsed(balance.getUsed().subtract(req.getTotalDays()));
            }
            leaveBalanceRepository.save(balance);

            transactionRepository.save(LeaveBalanceTransaction.builder()
                    .leaveBalance(balance)
                    .txnType("CANCELLATION")
                    .days(req.getTotalDays())
                    .leaveRequest(req)
                    .remarks("Cancelled by employee")
                    .createdBy(employeeId)
                    .build());
        });

        req.setStatus("CANCELLED");
        leaveRequestRepository.save(req);

        return toRequestDto(req);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> getMyRequests(Long employeeId) {
        return leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId).stream()
                .map(this::toRequestDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<LeaveRequestDto> searchLeaveRequests(String status, Long departmentId, int page, int size, UserDetailsImpl principal) {
        boolean isHrOrAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));
        boolean isManager = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        String s = (status != null && !status.isBlank()) ? status.trim() : null;

        Page<LeaveRequest> res;
        if (isHrOrAdmin) {
            res = leaveRequestRepository.searchLeaveRequests(s, departmentId, pageable);
        } else if (isManager) {
            res = leaveRequestRepository.searchLeaveRequestsByManager(principal.getEmployeeId(), s, departmentId, pageable);
        } else {
            throw new AccessDeniedException("Employees can only view their own leave requests");
        }

        List<LeaveRequestDto> dtos = res.getContent().stream()
                .map(this::toRequestDto)
                .toList();

        return PageResponse.of(dtos, res);
    }

    @Transactional
    public LeaveTypeDto createLeaveType(CreateLeaveTypeRequest req) {
        if (leaveTypeRepository.findByCode(req.code().toUpperCase()).isPresent()) {
            throw new BusinessException("Leave type with code " + req.code() + " already exists");
        }

        String accrual = req.accrualType() != null ? req.accrualType().toUpperCase().trim() : "YEARLY";
        if ("ANNUAL".equals(accrual)) {
            accrual = "YEARLY";
        }
        if (!java.util.Set.of("YEARLY", "MONTHLY", "NONE").contains(accrual)) {
            throw new BusinessException("Invalid accrual type. Must be YEARLY, MONTHLY, or NONE");
        }

        LeaveType type = LeaveType.builder()
                .code(req.code().toUpperCase().trim())
                .name(req.name().trim())
                .paid(req.paid())
                .annualQuota(req.annualQuota())
                .accrualType(accrual)
                .allowHalfDay(req.allowHalfDay())
                .active(true)
                .build();

        LeaveType saved = leaveTypeRepository.save(type);
        return new LeaveTypeDto(
                saved.getId(),
                saved.getCode(),
                saved.getName(),
                saved.isPaid(),
                saved.getAnnualQuota(),
                saved.getAccrualType(),
                saved.isAllowHalfDay()
        );
    }

    @Transactional
    public LeaveBalanceDto creditLeaveBalance(CreditLeaveBalanceRequest req, Long adminId) {
        Employee emp = employeeRepository.findById(req.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", req.employeeId()));

        LeaveType type = leaveTypeRepository.findById(req.leaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("LeaveType", "id", req.leaveTypeId()));

        short year = (req.year() != null) ? req.year() : (short) LocalDate.now().getYear();

        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(emp.getId(), type.getId(), year)
                .orElseGet(() -> leaveBalanceRepository.save(LeaveBalance.builder()
                        .employee(emp)
                        .leaveType(type)
                        .year(year)
                        .opening(BigDecimal.ZERO)
                        .accrued(BigDecimal.ZERO)
                        .adjusted(BigDecimal.ZERO)
                        .used(BigDecimal.ZERO)
                        .pending(BigDecimal.ZERO)
                        .build()));

        balance.setAdjusted(balance.getAdjusted().add(req.days()));
        leaveBalanceRepository.save(balance);

        transactionRepository.save(LeaveBalanceTransaction.builder()
                .leaveBalance(balance)
                .txnType(req.days().compareTo(BigDecimal.ZERO) >= 0 ? "CREDIT" : "DEBIT")
                .days(req.days())
                .remarks(req.remarks() != null ? req.remarks() : "Manual balance adjustment")
                .createdBy(adminId)
                .build());

        return toBalanceDto(balance);
    }

    private LeaveBalanceDto toBalanceDto(LeaveBalance b) {
        return new LeaveBalanceDto(
                b.getId(),
                b.getLeaveType().getId(),
                b.getLeaveType().getCode(),
                b.getLeaveType().getName(),
                b.getYear(),
                b.getOpening(),
                b.getAccrued(),
                b.getAdjusted(),
                b.getUsed(),
                b.getPending(),
                b.computeAvailable()
        );
    }

    private LeaveRequestDto toRequestDto(LeaveRequest r) {
        return new LeaveRequestDto(
                r.getId(),
                r.getEmployee().getId(),
                r.getEmployee().getEmployeeCode(),
                r.getEmployee().getFullName(),
                r.getEmployee().getDepartment() != null ? r.getEmployee().getDepartment().getName() : null,
                r.getLeaveType().getId(),
                r.getLeaveType().getCode(),
                r.getLeaveType().getName(),
                r.getStartDate(),
                r.getEndDate(),
                r.getStartDayPart(),
                r.getEndDayPart(),
                r.getTotalDays(),
                r.getReason(),
                r.getStatus(),
                r.getApprover() != null ? r.getApprover().getId() : null,
                r.getApprover() != null ? r.getApprover().getFullName() : null,
                r.getDecidedAt(),
                r.getDecisionComment(),
                r.getCreatedAt()
        );
    }
}
