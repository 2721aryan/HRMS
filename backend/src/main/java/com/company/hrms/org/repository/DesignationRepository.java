package com.company.hrms.org.repository;

import com.company.hrms.org.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DesignationRepository extends JpaRepository<Designation, Long> {
    Optional<Designation> findByTitle(String title);
    boolean existsByTitle(String title);
    List<Designation> findAllByActiveTrueOrderByTitleAsc();
}
