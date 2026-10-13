package com.company.hrms.leave.repository;

import com.company.hrms.leave.entity.LeaveBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    @Query("SELECT b FROM LeaveBalance b " +
           "JOIN FETCH b.leaveType " +
           "WHERE b.employee.id = :employeeId AND b.year = :year")
    List<LeaveBalance> findByEmployeeIdAndYear(@Param("employeeId") Long employeeId, @Param("year") short year);

    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeIdAndYear(Long employeeId, Long leaveTypeId, short year);

    /**
     * Pessimistic write lock — used during leave application to prevent concurrent
     * double-spend on the same balance row. Serializes concurrent requests at DB level.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM LeaveBalance b " +
           "WHERE b.employee.id = :employeeId " +
           "AND b.leaveType.id = :leaveTypeId " +
           "AND b.year = :year")
    Optional<LeaveBalance> findByEmployeeAndTypeAndYearForUpdate(
            @Param("employeeId") Long employeeId,
            @Param("leaveTypeId") Long leaveTypeId,
            @Param("year") short year);
}
