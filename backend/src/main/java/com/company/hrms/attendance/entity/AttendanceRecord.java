package com.company.hrms.attendance.entity;

import com.company.hrms.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "attendance_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in")
    private Instant checkIn;

    @Column(name = "check_out")
    private Instant checkOut;

    @Builder.Default
    @Column(name = "work_minutes", nullable = false)
    private int workMinutes = 0;

    @Column(nullable = false, length = 15)
    private String status; // PRESENT, ABSENT, HALF_DAY, ON_LEAVE, HOLIDAY, WEEKLY_OFF

    @Builder.Default
    @Column(name = "is_late", nullable = false)
    private boolean late = false;

    @Builder.Default
    @Column(nullable = false, length = 15)
    private String source = "WEB"; // WEB, MANUAL, BIOMETRIC, IMPORT, SYSTEM

    @Column(name = "leave_request_id")
    private Long leaveRequestId;

    @Builder.Default
    @Column(nullable = false)
    private boolean locked = false;

    @Column(length = 255)
    private String remarks;

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
