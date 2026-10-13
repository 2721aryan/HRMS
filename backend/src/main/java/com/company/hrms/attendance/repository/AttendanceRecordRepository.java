package com.company.hrms.attendance.repository;

import com.company.hrms.attendance.entity.AttendanceRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    /**
     * Pessimistic write lock version — used in punchIn to serialize concurrent requests
     * for the same employee+date, preventing double check-in race conditions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM AttendanceRecord r WHERE r.employee.id = :employeeId AND r.attendanceDate = :date")
    Optional<AttendanceRecord> findByEmployeeIdAndDateForUpdate(
            @Param("employeeId") Long employeeId,
            @Param("date") LocalDate date);

    List<AttendanceRecord> findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            Long employeeId, LocalDate from, LocalDate to);

    List<AttendanceRecord> findByAttendanceDate(LocalDate date);

    long countByAttendanceDateAndStatus(LocalDate date, String status);

    long countByAttendanceDateAndLateTrue(LocalDate date);

    @Query("SELECT r FROM AttendanceRecord r " +
           "JOIN FETCH r.employee e " +
           "LEFT JOIN FETCH e.department d " +
           "WHERE (cast(:startDate as LocalDate) IS NULL OR r.attendanceDate >= :startDate) " +
           "AND (cast(:endDate as LocalDate) IS NULL OR r.attendanceDate <= :endDate) " +
           "AND (cast(:departmentId as Long) IS NULL OR d.id = :departmentId) " +
           "AND (cast(:status as String) IS NULL OR r.status = :status)")
    Page<AttendanceRecord> searchAttendance(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("departmentId") Long departmentId,
            @Param("status") String status,
            Pageable pageable
    );
}
