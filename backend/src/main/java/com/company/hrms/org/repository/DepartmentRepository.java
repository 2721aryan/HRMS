package com.company.hrms.org.repository;

import com.company.hrms.org.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByName(String name);
    List<Department> findAllByActiveTrueOrderByNameAsc();
}
