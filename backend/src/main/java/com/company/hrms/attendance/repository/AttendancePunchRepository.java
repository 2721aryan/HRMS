package com.company.hrms.attendance.repository;

import com.company.hrms.attendance.entity.AttendancePunch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AttendancePunchRepository extends JpaRepository<AttendancePunch, Long> {
    List<AttendancePunch> findByEmployeeIdAndPunchTimeBetweenOrderByPunchTimeAsc(
            Long employeeId, Instant start, Instant end);
}
