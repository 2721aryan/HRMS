package com.company.hrms.leave.repository;

import com.company.hrms.leave.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
    Optional<LeaveType> findByCode(String code);
    List<LeaveType> findByActiveTrue();
}
