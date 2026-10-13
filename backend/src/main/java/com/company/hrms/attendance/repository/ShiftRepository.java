package com.company.hrms.attendance.repository;

import com.company.hrms.attendance.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
    Optional<Shift> findByName(String name);
    Optional<Shift> findFirstByActiveTrue();
}
