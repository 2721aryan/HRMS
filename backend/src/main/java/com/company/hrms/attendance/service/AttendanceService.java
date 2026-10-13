package com.company.hrms.attendance.service;

import com.company.hrms.attendance.dto.*;
import com.company.hrms.attendance.entity.*;
import com.company.hrms.attendance.repository.*;
import com.company.hrms.common.dto.PageResponse;
import com.company.hrms.common.exception.BusinessException;
import com.company.hrms.common.exception.ResourceNotFoundException;
import com.company.hrms.employee.entity.Employee;
import com.company.hrms.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.company.hrms.auth.security.UserDetailsImpl;
import org.springframework.security.access.AccessDeniedException;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRecordRepository recordRepository;
    private final AttendancePunchRepository punchRepository;
    private final AttendanceRegularizationRepository regularizationRepository;
    private final ShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private Shift getDefaultShift() {
        return shiftRepository.findFirstByActiveTrue()
                .orElseGet(() -> Shift.builder()
                        .name("General Shift")
                        .startTime(LocalTime.of(9, 30))
                        .endTime(LocalTime.of(18, 30))
                        .graceMinutes(15)
                        .fullDayMinutes(480)
                        .halfDayMinutes(240)
                        .build());
    }

    @Transactional(readOnly = true)
    public TodayAttendanceDto getTodayAttendance(Long employeeId) {
        LocalDate today = LocalDate.now(ZONE);
        Shift shift = getDefaultShift();

        Optional<AttendanceRecord> recOpt = recordRepository.findByEmployeeIdAndAttendanceDate(employeeId, today);

        if (recOpt.isEmpty()) {
            return new TodayAttendanceDto(
                    false,
                    false,
                    null,
                    null,
                    0,
                    "NOT_MARKED",
                    false,
                    shift.getStartTime(),
                    shift.getEndTime(),
                    shift.getName()
            );
        }

        AttendanceRecord rec = recOpt.get();
        int minutes = rec.getWorkMinutes();
        if (rec.getCheckIn() != null && rec.getCheckOut() == null) {
            minutes = (int) ChronoUnit.MINUTES.between(rec.getCheckIn(), Instant.now());
        }

        return new TodayAttendanceDto(
                rec.getCheckIn() != null,
                rec.getCheckOut() != null,
                rec.getCheckIn(),
                rec.getCheckOut(),
                minutes,
                rec.getStatus(),
                rec.isLate(),
                shift.getStartTime(),
                shift.getEndTime(),
                shift.getName()
        );
    }

    @Transactional
    public TodayAttendanceDto punchIn(Long employeeId, PunchRequest req) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        LocalDate today = LocalDate.now(ZONE);
        Instant now = Instant.now();
        LocalTime nowTime = LocalTime.now(ZONE);
        Shift shift = getDefaultShift();

        // Step 1: ensure the record exists (idempotent upsert)
        if (recordRepository.findByEmployeeIdAndAttendanceDate(employeeId, today).isEmpty()) {
            AttendanceRecord newRecord = AttendanceRecord.builder()
                    .employee(emp)
                    .attendanceDate(today)
                    .build();
            recordRepository.save(newRecord);
        }

        // Step 2: re-fetch with PESSIMISTIC_WRITE lock to serialize concurrent punch-ins.
        // A second concurrent request will block here until this transaction commits,
        // then see checkIn != null and throw the already-checked-in error.
        AttendanceRecord record = recordRepository.findByEmployeeIdAndDateForUpdate(employeeId, today)
                .orElseThrow(() -> new BusinessException("Could not acquire attendance record"));

        if (record.getCheckIn() != null) {
            throw new BusinessException("You have already checked in for today at " +
                    record.getCheckIn().atZone(ZONE).toLocalTime().truncatedTo(ChronoUnit.MINUTES));
        }

        // Late calculation: start_time + grace_minutes
        LocalTime lateThreshold = shift.getStartTime().plusMinutes(shift.getGraceMinutes());
        boolean isLate = nowTime.isAfter(lateThreshold);

        record.setCheckIn(now);
        record.setStatus("PRESENT");
        record.setLate(isLate);
        record.setSource(req != null && req.source() != null ? req.source() : "WEB");
        if (req != null && req.remarks() != null) {
            record.setRemarks(req.remarks());
        }

        recordRepository.save(record);

        punchRepository.save(AttendancePunch.builder()
                .employee(emp)
                .punchTime(now)
                .punchType("IN")
                .source(record.getSource())
                .build());

        return getTodayAttendance(employeeId);
    }

    @Transactional
    public TodayAttendanceDto punchOut(Long employeeId, PunchRequest req) {
        LocalDate today = LocalDate.now(ZONE);
        Instant now = Instant.now();
        Shift shift = getDefaultShift();

        AttendanceRecord record = recordRepository.findByEmployeeIdAndAttendanceDate(employeeId, today)
                .orElseThrow(() -> new BusinessException("Cannot clock out: No check-in record found for today."));

        if (record.getCheckIn() == null) {
            throw new BusinessException("Cannot clock out: Check-in was not recorded.");
        }

        record.setCheckOut(now);
        int workedMinutes = (int) ChronoUnit.MINUTES.between(record.getCheckIn(), now);
        record.setWorkMinutes(workedMinutes);

        if (workedMinutes < shift.getHalfDayMinutes()) {
            record.setStatus("HALF_DAY");
        } else {
            record.setStatus("PRESENT");
        }

        if (req != null && req.remarks() != null) {
            record.setRemarks(req.remarks());
        }

        recordRepository.save(record);

        Employee emp = record.getEmployee();
        punchRepository.save(AttendancePunch.builder()
                .employee(emp)
                .punchTime(now)
                .punchType("OUT")
                .source(record.getSource())
                .build());

        return getTodayAttendance(employeeId);
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> getEmployeeAttendance(Long employeeId, LocalDate from, LocalDate to) {
        if (from == null) from = LocalDate.now(ZONE).withDayOfMonth(1);
        if (to == null) to = LocalDate.now(ZONE);

        return recordRepository.findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(employeeId, from, to)
                .stream()
                .map(this::toRecordDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceRecordDto> searchAttendance(
            LocalDate startDate,
            LocalDate endDate,
            Long departmentId,
            String status,
            int page,
            int size,
            UserDetailsImpl principal
    ) {
        boolean isHrOrAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));
        boolean isManager = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "attendanceDate"));
        String s = (status != null && !status.isBlank()) ? status.trim() : null;

        Page<AttendanceRecord> pageResult;
        if (isHrOrAdmin) {
            pageResult = recordRepository.searchAttendance(startDate, endDate, departmentId, s, pageable);
        } else if (isManager) {
            pageResult = recordRepository.searchAttendanceByManager(principal.getEmployeeId(), startDate, endDate, departmentId, s, pageable);
        } else {
            throw new AccessDeniedException("Employees can only view their own attendance records");
        }

        List<AttendanceRecordDto> dtoList = pageResult.getContent().stream()
                .map(this::toRecordDto)
                .toList();

        return PageResponse.of(dtoList, pageResult);
    }

    @Transactional(readOnly = true)
    public AttendanceStatsDto getTodayStats(UserDetailsImpl principal) {
        boolean isHrOrAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));
        boolean isManager = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

        LocalDate today = LocalDate.now(ZONE);
        if (isHrOrAdmin) {
            long present = recordRepository.countByAttendanceDateAndStatus(today, "PRESENT");
            long halfDay = recordRepository.countByAttendanceDateAndStatus(today, "HALF_DAY");
            long onLeave = recordRepository.countByAttendanceDateAndStatus(today, "ON_LEAVE");
            long late = recordRepository.countByAttendanceDateAndLateTrue(today);
            long totalActive = employeeRepository.countByStatus("ACTIVE");
            long absent = Math.max(0, totalActive - (present + halfDay + onLeave));

            return new AttendanceStatsDto(today, present, absent, onLeave, halfDay, late, totalActive);
        } else if (isManager) {
            Long managerId = principal.getEmployeeId();
            long present = recordRepository.countByManagerAndAttendanceDateAndStatus(managerId, today, "PRESENT");
            long halfDay = recordRepository.countByManagerAndAttendanceDateAndStatus(managerId, today, "HALF_DAY");
            long onLeave = recordRepository.countByManagerAndAttendanceDateAndStatus(managerId, today, "ON_LEAVE");
            long late = recordRepository.countByManagerAndAttendanceDateAndLateTrue(managerId, today);
            long totalActive = employeeRepository.countByManagerIdAndStatus(managerId, "ACTIVE");
            long absent = Math.max(0, totalActive - (present + halfDay + onLeave));

            return new AttendanceStatsDto(today, present, absent, onLeave, halfDay, late, totalActive);
        } else {
            throw new AccessDeniedException("Employees do not have access to organization attendance statistics");
        }
    }

    @Transactional
    public AttendanceRegularizationDto requestRegularization(Long employeeId, RegularizationRequest req) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        LocalDate today = LocalDate.now(ZONE);
        if (req.attendanceDate().isAfter(today)) {
            throw new BusinessException("Cannot request regularization for a future date.");
        }
        if (req.attendanceDate().isBefore(today.minusDays(30))) {
            throw new BusinessException("Regularization can only be requested for dates within the last 30 days.");
        }
        if (emp.getDateOfJoining() != null && req.attendanceDate().isBefore(emp.getDateOfJoining())) {
            throw new BusinessException("Regularization date cannot be before employee joining date (" + emp.getDateOfJoining() + ").");
        }

        AttendanceRecord record = recordRepository.findByEmployeeIdAndAttendanceDate(employeeId, req.attendanceDate())
                .orElse(null);

        AttendanceRegularization reg = AttendanceRegularization.builder()
                .employee(emp)
                .attendanceDate(req.attendanceDate())
                .attendanceRecord(record)
                .requestedCheckIn(req.requestedCheckIn())
                .requestedCheckOut(req.requestedCheckOut())
                .reason(req.reason())
                .status("PENDING")
                .build();

        AttendanceRegularization saved = regularizationRepository.save(reg);
        return toRegularizationDto(saved);
    }

    @Transactional(readOnly = true)
    public List<AttendanceRegularizationDto> getEmployeeRegularizations(Long employeeId) {
        return regularizationRepository.findByEmployeeIdOrderByAttendanceDateDesc(employeeId)
                .stream()
                .map(this::toRegularizationDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceRegularizationDto> getAllRegularizations(String status, int page, int size, UserDetailsImpl principal) {
        boolean isHrOrAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));
        boolean isManager = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        String s = (status != null && !status.isBlank()) ? status.trim() : null;

        Page<AttendanceRegularization> res;
        if (isHrOrAdmin) {
            res = regularizationRepository.findAllWithEmployee(s, pageable);
        } else if (isManager) {
            res = regularizationRepository.findAllByManager(principal.getEmployeeId(), s, pageable);
        } else {
            throw new AccessDeniedException("Employees can only view their own regularization requests");
        }

        List<AttendanceRegularizationDto> dtos = res.getContent().stream()
                .map(this::toRegularizationDto)
                .toList();

        return PageResponse.of(dtos, res);
    }

    @Transactional
    public AttendanceRegularizationDto approveRegularization(Long id, Long approverId, String comment, UserDetailsImpl principal) {
        AttendanceRegularization reg = regularizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceRegularization", "id", id));

        boolean isHrOrAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));

        if (!isHrOrAdmin) {
            if (reg.getEmployee().getManager() == null || !reg.getEmployee().getManager().getId().equals(approverId)) {
                throw new AccessDeniedException("Managers can only approve regularizations for their direct team members");
            }
        }

        Employee approver = employeeRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", approverId));

        reg.setStatus("APPROVED");
        reg.setApprover(approver);
        reg.setDecidedAt(Instant.now());
        reg.setDecisionComment(comment);

        // Update or create AttendanceRecord
        AttendanceRecord record = reg.getAttendanceRecord();
        if (record == null) {
            record = recordRepository.findByEmployeeIdAndAttendanceDate(reg.getEmployee().getId(), reg.getAttendanceDate())
                    .orElseGet(() -> AttendanceRecord.builder()
                            .employee(reg.getEmployee())
                            .attendanceDate(reg.getAttendanceDate())
                            .source("MANUAL")
                            .build());
        }

        if (reg.getRequestedCheckIn() != null) {
            record.setCheckIn(reg.getRequestedCheckIn());
        }
        if (reg.getRequestedCheckOut() != null) {
            record.setCheckOut(reg.getRequestedCheckOut());
        }

        if (record.getCheckIn() != null && record.getCheckOut() != null) {
            int mins = (int) ChronoUnit.MINUTES.between(record.getCheckIn(), record.getCheckOut());
            record.setWorkMinutes(mins);
            record.setStatus(mins < 240 ? "HALF_DAY" : "PRESENT");
        } else {
            record.setStatus("PRESENT");
        }
        record.setLate(false);
        record.setRemarks("Regularized by " + approver.getFullName() + (comment != null ? ": " + comment : ""));

        recordRepository.save(record);
        regularizationRepository.save(reg);

        return toRegularizationDto(reg);
    }

    @Transactional
    public AttendanceRegularizationDto rejectRegularization(Long id, Long approverId, String comment, UserDetailsImpl principal) {
        AttendanceRegularization reg = regularizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceRegularization", "id", id));

        boolean isHrOrAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));

        if (!isHrOrAdmin) {
            if (reg.getEmployee().getManager() == null || !reg.getEmployee().getManager().getId().equals(approverId)) {
                throw new AccessDeniedException("Managers can only reject regularizations for their direct team members");
            }
        }

        Employee approver = employeeRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", approverId));

        reg.setStatus("REJECTED");
        reg.setApprover(approver);
        reg.setDecidedAt(Instant.now());
        reg.setDecisionComment(comment);

        regularizationRepository.save(reg);
        return toRegularizationDto(reg);
    }

    private AttendanceRecordDto toRecordDto(AttendanceRecord r) {
        return new AttendanceRecordDto(
                r.getId(),
                r.getEmployee().getId(),
                r.getEmployee().getEmployeeCode(),
                r.getEmployee().getFullName(),
                r.getEmployee().getDepartment() != null ? r.getEmployee().getDepartment().getName() : null,
                r.getAttendanceDate(),
                r.getCheckIn(),
                r.getCheckOut(),
                r.getWorkMinutes(),
                r.getStatus(),
                r.isLate(),
                r.getSource(),
                r.getRemarks()
        );
    }

    private AttendanceRegularizationDto toRegularizationDto(AttendanceRegularization r) {
        return new AttendanceRegularizationDto(
                r.getId(),
                r.getEmployee().getId(),
                r.getEmployee().getEmployeeCode(),
                r.getEmployee().getFullName(),
                r.getAttendanceDate(),
                r.getRequestedCheckIn(),
                r.getRequestedCheckOut(),
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
