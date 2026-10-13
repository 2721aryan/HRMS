package com.company.hrms.leave.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "leave_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code; // CL, SL, PL, LOP, MAT, PAT

    @Column(nullable = false, length = 80)
    private String name;

    @Builder.Default
    @Column(name = "is_paid", nullable = false)
    private boolean paid = true;

    @Builder.Default
    @Column(name = "annual_quota", nullable = false, precision = 5, scale = 1)
    private BigDecimal annualQuota = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "accrual_type", nullable = false, length = 20)
    private String accrualType = "YEARLY"; // YEARLY, MONTHLY, NONE

    @Builder.Default
    @Column(name = "carry_forward_allowed", nullable = false)
    private boolean carryForwardAllowed = false;

    @Builder.Default
    @Column(name = "max_carry_forward", nullable = false, precision = 5, scale = 1)
    private BigDecimal maxCarryForward = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "allow_half_day", nullable = false)
    private boolean allowHalfDay = true;

    @Builder.Default
    @Column(name = "requires_document", nullable = false)
    private boolean requiresDocument = false;

    @Column(name = "max_consecutive_days", precision = 5, scale = 1)
    private BigDecimal maxConsecutiveDays;

    @Column(name = "applicable_gender", length = 20)
    private String applicableGender;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
