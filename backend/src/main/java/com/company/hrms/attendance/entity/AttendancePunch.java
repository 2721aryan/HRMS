package com.company.hrms.attendance.entity;

import com.company.hrms.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "attendance_punches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendancePunch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "punch_time", nullable = false)
    private Instant punchTime;

    @Column(name = "punch_type", length = 5)
    private String punchType; // IN, OUT

    @Builder.Default
    @Column(nullable = false, length = 15)
    private String source = "WEB"; // WEB, BIOMETRIC, IMPORT

    @Column(name = "device_id", length = 50)
    private String deviceId;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
