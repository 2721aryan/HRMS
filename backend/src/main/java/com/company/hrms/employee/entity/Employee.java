package com.company.hrms.employee.entity;

import com.company.hrms.org.entity.Department;
import com.company.hrms.org.entity.Designation;
import com.company.hrms.org.entity.Location;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", nullable = false, unique = true, length = 20)
    private String employeeCode;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", length = 80)
    private String lastName;

    @Column(name = "work_email", nullable = false, unique = true, length = 150)
    private String workEmail;

    @Column(name = "personal_email", length = 150)
    private String personalEmail;

    @Column(length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender; // MALE, FEMALE, OTHER, UNDISCLOSED

    @Column(name = "marital_status", length = 20)
    private String maritalStatus;

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Column(length = 80)
    private String city;

    @Column(length = 80)
    private String state;

    @Column(name = "postal_code", length = 12)
    private String postalCode;

    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designation_id")
    private Designation designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Builder.Default
    @Column(name = "employment_type", nullable = false, length = 20)
    private String employmentType = "FULL_TIME"; // FULL_TIME, PART_TIME, CONTRACT, INTERN

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, ON_NOTICE, INACTIVE

    @Column(name = "date_of_joining", nullable = false)
    private LocalDate dateOfJoining;

    @Column(name = "probation_end_date")
    private LocalDate probationEndDate;

    @Column(name = "date_of_exit")
    private LocalDate dateOfExit;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public String getFullName() {
        return lastName != null && !lastName.isBlank()
                ? firstName + " " + lastName
                : firstName;
    }
}
