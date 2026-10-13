package com.company.hrms.common.config;

import com.company.hrms.auth.entity.Role;
import com.company.hrms.auth.entity.User;
import com.company.hrms.auth.repository.RoleRepository;
import com.company.hrms.auth.repository.UserRepository;
import com.company.hrms.employee.entity.Employee;
import com.company.hrms.employee.repository.EmployeeRepository;
import com.company.hrms.org.entity.Department;
import com.company.hrms.org.entity.Designation;
import com.company.hrms.org.entity.Location;
import com.company.hrms.org.repository.DepartmentRepository;
import com.company.hrms.org.repository.DesignationRepository;
import com.company.hrms.org.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final LocationRepository locationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 1. Seed Location
        Location hq = locationRepository.findByName("Headquarters (Bengaluru)")
                .orElseGet(() -> locationRepository.save(Location.builder()
                        .name("Headquarters (Bengaluru)")
                        .city("Bengaluru")
                        .state("Karnataka")
                        .country("India")
                        .address("Outer Ring Road, Bellandur")
                        .active(true)
                        .build()));

        // 2. Seed Departments
        Department engDept = departmentRepository.findByCode("ENG")
                .orElseGet(() -> departmentRepository.save(Department.builder()
                        .code("ENG").name("Engineering").active(true).build()));

        Department hrDept = departmentRepository.findByCode("HR")
                .orElseGet(() -> departmentRepository.save(Department.builder()
                        .code("HR").name("Human Resources").active(true).build()));

        Department prodDept = departmentRepository.findByCode("PROD")
                .orElseGet(() -> departmentRepository.save(Department.builder()
                        .code("PROD").name("Product & Design").active(true).build()));

        Department finDept = departmentRepository.findByCode("FIN")
                .orElseGet(() -> departmentRepository.save(Department.builder()
                        .code("FIN").name("Finance").active(true).build()));

        // 3. Seed Designations
        Designation emDesig = designationRepository.findByTitle("Engineering Manager")
                .orElseGet(() -> designationRepository.save(Designation.builder()
                        .title("Engineering Manager").grade("M1").active(true).build()));

        Designation leadDesig = designationRepository.findByTitle("Lead Full-Stack Engineer")
                .orElseGet(() -> designationRepository.save(Designation.builder()
                        .title("Lead Full-Stack Engineer").grade("L3").active(true).build()));

        Designation srFrontendDesig = designationRepository.findByTitle("Senior Frontend Engineer")
                .orElseGet(() -> designationRepository.save(Designation.builder()
                        .title("Senior Frontend Engineer").grade("L2").active(true).build()));

        Designation hrLeadDesig = designationRepository.findByTitle("HR Lead")
                .orElseGet(() -> designationRepository.save(Designation.builder()
                        .title("HR Lead").grade("L2").active(true).build()));

        Designation prodDesig = designationRepository.findByTitle("Product Designer")
                .orElseGet(() -> designationRepository.save(Designation.builder()
                        .title("Product Designer").grade("L2").active(true).build()));

        // 4. Ensure Admin User
        if (userRepository.findByUsername("admin").isEmpty()) {
            log.info("Creating initial system admin employee and user account...");

            Employee adminEmp = employeeRepository.findByWorkEmail("admin@hrms.local")
                    .orElseGet(() -> employeeRepository.save(
                            Employee.builder()
                                    .employeeCode("EMP-0001")
                                    .firstName("System")
                                    .lastName("Admin")
                                    .workEmail("admin@hrms.local")
                                    .department(engDept)
                                    .designation(emDesig)
                                    .location(hq)
                                    .phone("+91 9876543210")
                                    .city("Bengaluru")
                                    .state("Karnataka")
                                    .dateOfJoining(LocalDate.of(2023, 1, 15))
                                    .status("ACTIVE")
                                    .build()
                    ));

            Role adminRole = roleRepository.findByCode("ADMIN")
                    .orElseGet(() -> roleRepository.save(
                            Role.builder()
                                    .code("ADMIN")
                                    .name("System Admin")
                                    .description("Full system access and configuration")
                                    .build()
                    ));

            User adminUser = User.builder()
                    .employeeId(adminEmp.getId())
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .roles(Set.of(adminRole))
                    .active(true)
                    .mustChangePassword(false)
                    .tokenVersion(0)
                    .build();

            userRepository.save(adminUser);
            log.info("Initial admin created successfully: username='admin', password='Admin@123'");
        }

        // 5. Seed sample employees if only admin exists
        if (employeeRepository.count() <= 1) {
            log.info("Seeding realistic sample employees...");

            Employee manager = employeeRepository.save(Employee.builder()
                    .employeeCode("EMP-0002")
                    .firstName("Aarav")
                    .lastName("Sharma")
                    .workEmail("aarav.sharma@company.com")
                    .personalEmail("aarav.dev@gmail.com")
                    .phone("+91 9811223344")
                    .gender("MALE")
                    .department(engDept)
                    .designation(leadDesig)
                    .location(hq)
                    .city("Bengaluru")
                    .state("Karnataka")
                    .emergencyContactName("Sunita Sharma")
                    .emergencyContactPhone("+91 9811223300")
                    .employmentType("FULL_TIME")
                    .status("ACTIVE")
                    .dateOfJoining(LocalDate.of(2023, 3, 1))
                    .build());

            employeeRepository.saveAll(List.of(
                    Employee.builder()
                            .employeeCode("EMP-0003")
                            .firstName("Priya")
                            .lastName("Patel")
                            .workEmail("priya.patel@company.com")
                            .personalEmail("priya.patel.design@gmail.com")
                            .phone("+91 9822334455")
                            .gender("FEMALE")
                            .department(prodDept)
                            .designation(prodDesig)
                            .location(hq)
                            .manager(manager)
                            .city("Bengaluru")
                            .state("Karnataka")
                            .emergencyContactName("Rajesh Patel")
                            .emergencyContactPhone("+91 9822334400")
                            .employmentType("FULL_TIME")
                            .status("ACTIVE")
                            .dateOfJoining(LocalDate.of(2023, 6, 15))
                            .build(),

                    Employee.builder()
                            .employeeCode("EMP-0004")
                            .firstName("Rohan")
                            .lastName("Verma")
                            .workEmail("rohan.verma@company.com")
                            .phone("+91 9833445566")
                            .gender("MALE")
                            .department(engDept)
                            .designation(srFrontendDesig)
                            .location(hq)
                            .manager(manager)
                            .city("Bengaluru")
                            .state("Karnataka")
                            .employmentType("FULL_TIME")
                            .status("ACTIVE")
                            .dateOfJoining(LocalDate.of(2023, 8, 1))
                            .build(),

                    Employee.builder()
                            .employeeCode("EMP-0005")
                            .firstName("Ananya")
                            .lastName("Iyer")
                            .workEmail("ananya.iyer@company.com")
                            .phone("+91 9844556677")
                            .gender("FEMALE")
                            .department(hrDept)
                            .designation(hrLeadDesig)
                            .location(hq)
                            .city("Bengaluru")
                            .state("Karnataka")
                            .employmentType("FULL_TIME")
                            .status("ACTIVE")
                            .dateOfJoining(LocalDate.of(2024, 1, 10))
                            .build()
            ));

            log.info("Sample employees seeded successfully.");
        }
    }
}
