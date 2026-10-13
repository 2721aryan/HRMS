package com.company.hrms.leave.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "holidays")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "location_id")
    private Long locationId;

    @Builder.Default
    @Column(name = "is_optional", nullable = false)
    private boolean optional = false;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
