package com.company.hrms.leave.repository;

import com.company.hrms.leave.entity.LeaveRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    @Query("SELECT r FROM LeaveRequest r " +
           "JOIN FETCH r.leaveType " +
           "WHERE r.employee.id = :employeeId " +
           "ORDER BY r.createdAt DESC")
    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(@Param("employeeId") Long employeeId);

    @Query("SELECT r FROM LeaveRequest r " +
           "JOIN FETCH r.employee e " +
           "JOIN FETCH r.leaveType " +
           "LEFT JOIN FETCH e.department d " +
           "WHERE (:status IS NULL OR r.status = :status) " +
           "AND (:departmentId IS NULL OR d.id = :departmentId) " +
           "ORDER BY r.createdAt DESC")
    Page<LeaveRequest> searchLeaveRequests(
            @Param("status") String status,
            @Param("departmentId") Long departmentId,
            Pageable pageable
    );

    @Query("SELECT COUNT(r) > 0 FROM LeaveRequest r " +
           "WHERE r.employee.id = :employeeId " +
           "AND r.status IN ('PENDING', 'APPROVED') " +
           "AND r.startDate <= :endDate AND r.endDate >= :startDate")
    boolean hasOverlappingLeave(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    long countByStatus(String status);
}
