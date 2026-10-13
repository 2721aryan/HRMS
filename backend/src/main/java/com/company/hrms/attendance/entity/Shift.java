package com.company.hrms.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "shifts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Builder.Default
    @Column(name = "grace_minutes", nullable = false)
    private int graceMinutes = 15;

    @Builder.Default
    @Column(name = "full_day_minutes", nullable = false)
    private int fullDayMinutes = 480;

    @Builder.Default
    @Column(name = "half_day_minutes", nullable = false)
    private int halfDayMinutes = 240;

    @Builder.Default
    @Column(name = "off_sunday", nullable = false)
    private boolean offSunday = true;

    @Builder.Default
    @Column(name = "off_monday", nullable = false)
    private boolean offMonday = false;

    @Builder.Default
    @Column(name = "off_tuesday", nullable = false)
    private boolean offTuesday = false;

    @Builder.Default
    @Column(name = "off_wednesday", nullable = false)
    private boolean offWednesday = false;

    @Builder.Default
    @Column(name = "off_thursday", nullable = false)
    private boolean offThursday = false;

    @Builder.Default
    @Column(name = "off_friday", nullable = false)
    private boolean offFriday = false;

    @Builder.Default
    @Column(name = "off_saturday", nullable = false)
    private boolean offSaturday = true;

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
