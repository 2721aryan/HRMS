package com.company.hrms.attendance.entity;

import com.company.hrms.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "attendance_regularizations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceRegularization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_record_id")
    private AttendanceRecord attendanceRecord;

    @Column(name = "requested_check_in")
    private Instant requestedCheckIn;

    @Column(name = "requested_check_out")
    private Instant requestedCheckOut;

    @Column(nullable = false, length = 500)
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

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
