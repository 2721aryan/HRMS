package com.company.hrms.employee.repository;

import com.company.hrms.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByWorkEmail(String workEmail);
    Optional<Employee> findByEmployeeCode(String employeeCode);
    boolean existsByEmployeeCode(String employeeCode);
    boolean existsByWorkEmail(String workEmail);

    Optional<Employee> findTopByOrderByEmployeeCodeDesc();

    long countByStatus(String status);

    @Query("SELECT e FROM Employee e " +
           "LEFT JOIN FETCH e.department " +
           "LEFT JOIN FETCH e.designation " +
           "LEFT JOIN FETCH e.location " +
           "WHERE (:status IS NULL OR e.status = :status) " +
           "AND (:departmentId IS NULL OR e.department.id = :departmentId) " +
           "AND (:search IS NULL OR " +
           "     LOWER(e.firstName) LIKE :search OR " +
           "     LOWER(e.lastName) LIKE :search OR " +
           "     LOWER(e.employeeCode) LIKE :search OR " +
           "     LOWER(e.workEmail) LIKE :search)")
    Page<Employee> searchEmployees(
            @Param("search") String search,
            @Param("departmentId") Long departmentId,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("SELECT e.department.name, COUNT(e) FROM Employee e WHERE e.status = 'ACTIVE' AND e.department IS NOT NULL GROUP BY e.department.name")
    List<Object[]> countActiveEmployeesByDepartment();

    @Query("SELECT e FROM Employee e LEFT JOIN FETCH e.department LEFT JOIN FETCH e.designation LEFT JOIN FETCH e.location ORDER BY e.id ASC")
    List<Employee> findAllWithRelations();
}
