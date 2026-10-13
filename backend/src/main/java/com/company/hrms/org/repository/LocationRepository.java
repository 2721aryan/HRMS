package com.company.hrms.org.repository;

import com.company.hrms.org.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByName(String name);
    boolean existsByName(String name);
    List<Location> findAllByActiveTrueOrderByNameAsc();
}
