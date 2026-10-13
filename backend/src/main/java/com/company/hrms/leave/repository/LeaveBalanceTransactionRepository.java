package com.company.hrms.leave.repository;

import com.company.hrms.leave.entity.LeaveBalanceTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveBalanceTransactionRepository extends JpaRepository<LeaveBalanceTransaction, Long> {
    List<LeaveBalanceTransaction> findByLeaveBalanceIdOrderByCreatedAtDesc(Long leaveBalanceId);
}
