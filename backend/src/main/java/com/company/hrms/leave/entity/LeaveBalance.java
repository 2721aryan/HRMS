package com.company.hrms.leave.entity;

import com.company.hrms.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "leave_balances", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"employee_id", "leave_type_id", "year"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(nullable = false)
    private short year;

    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal opening = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal accrued = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal adjusted = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal used = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal pending = BigDecimal.ZERO;

    // Generated column in Postgres: (opening + accrued + adjusted - used - pending)
    @Column(insertable = false, updatable = false, precision = 5, scale = 1)
    private BigDecimal available;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public BigDecimal computeAvailable() {
        if (available != null) return available;
        return opening.add(accrued).add(adjusted).subtract(used).subtract(pending);
    }
}
