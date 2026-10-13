package com.company.hrms.leave.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "leave_balance_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveBalanceTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_balance_id", nullable = false)
    private LeaveBalance leaveBalance;

    @Column(name = "txn_type", nullable = false, length = 20)
    private String txnType; // ACCRUAL, CARRY_FORWARD, REQUEST, APPROVAL, REJECTION, CANCELLATION, ADJUSTMENT

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal days; // signed

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_request_id")
    private LeaveRequest leaveRequest;

    @Column(length = 255)
    private String remarks;

    @Column(name = "created_by")
    private Long createdBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
