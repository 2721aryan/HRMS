package com.company.hrms.attendance.repository;

import com.company.hrms.attendance.entity.AttendanceRegularization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttendanceRegularizationRepository extends JpaRepository<AttendanceRegularization, Long> {

    List<AttendanceRegularization> findByEmployeeIdOrderByAttendanceDateDesc(Long employeeId);

    @Query("SELECT r FROM AttendanceRegularization r " +
           "JOIN FETCH r.employee e " +
           "WHERE (:status IS NULL OR r.status = :status) " +
           "ORDER BY r.createdAt DESC")
    Page<AttendanceRegularization> findAllWithEmployee(@Param("status") String status, Pageable pageable);

    long countByStatus(String status);
}
