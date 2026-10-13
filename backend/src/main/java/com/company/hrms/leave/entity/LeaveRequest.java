package com.company.hrms.leave.entity;

import com.company.hrms.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "leave_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Builder.Default
    @Column(name = "start_day_part", nullable = false, length = 15)
    private String startDayPart = "FULL_DAY"; // FULL_DAY, FIRST_HALF, SECOND_HALF

    @Builder.Default
    @Column(name = "end_day_part", nullable = false, length = 15)
    private String endDayPart = "FULL_DAY"; // FULL_DAY, FIRST_HALF, SECOND_HALF

    @Column(name = "total_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal totalDays;

    @Column(length = 500)
    private String reason;

    @Builder.Default
    @Column(nullable = false, length = 15)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, CANCELLED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private Employee approver;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_comment", length = 500)
    private String decisionComment;

    @Column(name = "document_id")
    private Long documentId;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
