package com.company.hrms.leave.repository;

import com.company.hrms.leave.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    List<Holiday> findByHolidayDateBetweenOrderByHolidayDateAsc(LocalDate start, LocalDate end);
    boolean existsByHolidayDate(LocalDate date);
}
