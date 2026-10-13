package com.company.hrms.employee.service;

import com.company.hrms.common.dto.PageResponse;
import com.company.hrms.common.exception.BusinessException;
import com.company.hrms.common.exception.ResourceNotFoundException;
import com.company.hrms.employee.dto.*;
import com.company.hrms.employee.entity.Employee;
import com.company.hrms.employee.repository.EmployeeRepository;
import com.company.hrms.org.entity.Department;
import com.company.hrms.org.entity.Designation;
import com.company.hrms.org.entity.Location;
import com.company.hrms.org.repository.DepartmentRepository;
import com.company.hrms.org.repository.DesignationRepository;
import com.company.hrms.org.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.company.hrms.auth.security.UserDetailsImpl;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final LocationRepository locationRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "firstName", "lastName", "employeeCode", "dateOfJoining", "status", "createdAt"
    );

    @Transactional(readOnly = true)
    public PageResponse<EmployeeSummaryDto> search(
            String search,
            Long departmentId,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDirection,
            UserDetailsImpl principal
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        String safeSortBy = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) ? sortBy : "id";
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, safeSortBy);
        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

        String searchPattern = (search != null && !search.isBlank())
                ? "%" + search.trim().toLowerCase() + "%"
                : null;

        Page<Employee> resultPage = employeeRepository.searchEmployees(
                searchPattern,
                departmentId,
                (status != null && !status.isBlank()) ? status.trim() : null,
                pageable
        );

        boolean isHrOrAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));

        List<EmployeeSummaryDto> dtoList = resultPage.getContent().stream()
                .map(e -> toSummaryDto(e, isHrOrAdmin))
                .toList();

        return PageResponse.of(dtoList, resultPage);
    }

    @Transactional(readOnly = true)
    public EmployeeDto getById(Long id, UserDetailsImpl principal) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));

        boolean isSelf = (principal != null && principal.getEmployeeId() != null && principal.getEmployeeId().equals(id));
        boolean isHrOrAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));
        boolean isManager = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

        if (!isSelf && !isHrOrAdmin) {
            if (isManager) {
                if (emp.getManager() == null || !emp.getManager().getId().equals(principal.getEmployeeId())) {
                    throw new AccessDeniedException("Managers can only view detailed profiles of their direct team members");
                }
                return toDto(emp);
            }
            throw new AccessDeniedException("Employees can only view their own detailed profile");
        }

        return toDto(emp);
    }

    @Transactional
    public EmployeeDto create(CreateEmployeeRequest req) {
        if (employeeRepository.existsByWorkEmail(req.workEmail())) {
            throw new BusinessException("Work email is already in use: " + req.workEmail());
        }

        String employeeCode = req.employeeCode();
        if (employeeCode == null || employeeCode.isBlank()) {
            employeeCode = generateNextEmployeeCode();
        } else if (employeeRepository.existsByEmployeeCode(employeeCode)) {
            throw new BusinessException("Employee code is already in use: " + employeeCode);
        }

        Employee emp = Employee.builder()
                .employeeCode(employeeCode)
                .firstName(req.firstName())
                .lastName(req.lastName())
                .workEmail(req.workEmail())
                .personalEmail(req.personalEmail())
                .phone(req.phone())
                .dateOfBirth(req.dateOfBirth())
                .gender(req.gender())
                .maritalStatus(req.maritalStatus())
                .addressLine(req.addressLine())
                .city(req.city())
                .state(req.state())
                .postalCode(req.postalCode())
                .emergencyContactName(req.emergencyContactName())
                .emergencyContactPhone(req.emergencyContactPhone())
                .employmentType(req.employmentType() != null ? req.employmentType() : "FULL_TIME")
                .status("ACTIVE")
                .dateOfJoining(req.dateOfJoining())
                .probationEndDate(req.probationEndDate())
                .build();

        if (req.departmentId() != null) {
            Department dept = departmentRepository.findById(req.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", req.departmentId()));
            emp.setDepartment(dept);
        }
        if (req.designationId() != null) {
            Designation desig = designationRepository.findById(req.designationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", req.designationId()));
            emp.setDesignation(desig);
        }
        if (req.locationId() != null) {
            Location loc = locationRepository.findById(req.locationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Location", "id", req.locationId()));
            emp.setLocation(loc);
        }
        if (req.managerId() != null) {
            Employee manager = employeeRepository.findById(req.managerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", "id", req.managerId()));
            emp.setManager(manager);
        }

        return toDto(employeeRepository.save(emp));
    }

    @Transactional
    public EmployeeDto update(Long id, UpdateEmployeeRequest req) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));

        if (!emp.getWorkEmail().equalsIgnoreCase(req.workEmail()) &&
            employeeRepository.existsByWorkEmail(req.workEmail())) {
            throw new BusinessException("Work email is already in use: " + req.workEmail());
        }

        if (req.managerId() != null && req.managerId().equals(id)) {
            throw new BusinessException("An employee cannot be their own manager");
        }

        emp.setFirstName(req.firstName());
        emp.setLastName(req.lastName());
        emp.setWorkEmail(req.workEmail());
        emp.setPersonalEmail(req.personalEmail());
        emp.setPhone(req.phone());
        emp.setDateOfBirth(req.dateOfBirth());
        emp.setGender(req.gender());
        emp.setMaritalStatus(req.maritalStatus());
        emp.setAddressLine(req.addressLine());
        emp.setCity(req.city());
        emp.setState(req.state());
        emp.setPostalCode(req.postalCode());
        emp.setEmergencyContactName(req.emergencyContactName());
        emp.setEmergencyContactPhone(req.emergencyContactPhone());
        if (req.employmentType() != null) emp.setEmploymentType(req.employmentType());
        if (req.status() != null) emp.setStatus(req.status());
        emp.setDateOfJoining(req.dateOfJoining());
        emp.setProbationEndDate(req.probationEndDate());
        emp.setDateOfExit(req.dateOfExit());
        emp.setUpdatedAt(Instant.now());

        if (req.departmentId() != null) {
            Department dept = departmentRepository.findById(req.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", req.departmentId()));
            emp.setDepartment(dept);
        } else {
            emp.setDepartment(null);
        }

        if (req.designationId() != null) {
            Designation desig = designationRepository.findById(req.designationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", req.designationId()));
            emp.setDesignation(desig);
        } else {
            emp.setDesignation(null);
        }

        if (req.locationId() != null) {
            Location loc = locationRepository.findById(req.locationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Location", "id", req.locationId()));
            emp.setLocation(loc);
        } else {
            emp.setLocation(null);
        }

        if (req.managerId() != null) {
            Employee manager = employeeRepository.findById(req.managerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", "id", req.managerId()));
            emp.setManager(manager);
        } else {
            emp.setManager(null);
        }

        return toDto(employeeRepository.save(emp));
    }

    @Transactional
    public EmployeeDto updateStatus(Long id, String status) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        emp.setStatus(status.toUpperCase());
        emp.setUpdatedAt(Instant.now());
        return toDto(employeeRepository.save(emp));
    }

    @Transactional(readOnly = true)
    public EmployeeStatsDto getStats(UserDetailsImpl principal) {
        boolean isHrOrAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));
        boolean isManager = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

        if (isHrOrAdmin) {
            long total = employeeRepository.count();
            long active = employeeRepository.countByStatus("ACTIVE");
            long onNotice = employeeRepository.countByStatus("ON_NOTICE");
            long inactive = employeeRepository.countByStatus("INACTIVE");

            Map<String, Long> byDepartment = new LinkedHashMap<>();
            List<Object[]> rows = employeeRepository.countActiveEmployeesByDepartment();
            for (Object[] row : rows) {
                if (row[0] != null && row[1] != null) {
                    byDepartment.put((String) row[0], ((Number) row[1]).longValue());
                }
            }

            return new EmployeeStatsDto(total, active, onNotice, inactive, byDepartment);
        } else if (isManager) {
            Long managerId = principal.getEmployeeId();
            long total = employeeRepository.countByManagerId(managerId);
            long active = employeeRepository.countByManagerIdAndStatus(managerId, "ACTIVE");
            long onNotice = employeeRepository.countByManagerIdAndStatus(managerId, "ON_NOTICE");
            long inactive = employeeRepository.countByManagerIdAndStatus(managerId, "INACTIVE");

            Map<String, Long> byDepartment = new LinkedHashMap<>();
            List<Object[]> rows = employeeRepository.countActiveTeamEmployeesByDepartment(managerId);
            for (Object[] row : rows) {
                if (row[0] != null && row[1] != null) {
                    byDepartment.put((String) row[0], ((Number) row[1]).longValue());
                }
            }

            return new EmployeeStatsDto(total, active, onNotice, inactive, byDepartment);
        } else {
            throw new AccessDeniedException("Employees do not have access to workforce statistics");
        }
    }

    @Transactional
    public EmployeeDto updateMyProfile(Long employeeId, UpdateMyProfileRequest req) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        if (req.personalEmail() != null) emp.setPersonalEmail(req.personalEmail());
        if (req.phone() != null) emp.setPhone(req.phone());
        if (req.addressLine() != null) emp.setAddressLine(req.addressLine());
        if (req.city() != null) emp.setCity(req.city());
        if (req.state() != null) emp.setState(req.state());
        if (req.postalCode() != null) emp.setPostalCode(req.postalCode());
        if (req.emergencyContactName() != null) emp.setEmergencyContactName(req.emergencyContactName());
        if (req.emergencyContactPhone() != null) emp.setEmergencyContactPhone(req.emergencyContactPhone());
        emp.setUpdatedAt(Instant.now());

        return toDto(employeeRepository.save(emp));
    }

    private synchronized String generateNextEmployeeCode() {
        long count = employeeRepository.count();
        return String.format("EMP-%04d", count + 1);
    }

    public EmployeeSummaryDto toSummaryDto(Employee e, boolean fullFields) {
        return new EmployeeSummaryDto(
                e.getId(),
                e.getEmployeeCode(),
                e.getFirstName(),
                e.getLastName(),
                e.getFullName(),
                e.getWorkEmail(),
                fullFields ? e.getPhone() : null,
                e.getDepartment() != null ? e.getDepartment().getId() : null,
                e.getDepartment() != null ? e.getDepartment().getName() : null,
                e.getDesignation() != null ? e.getDesignation().getId() : null,
                e.getDesignation() != null ? e.getDesignation().getTitle() : null,
                fullFields ? e.getEmploymentType() : null,
                e.getStatus(),
                fullFields ? e.getDateOfJoining() : null
        );
    }

    public EmployeeDto toDto(Employee e) {
        return new EmployeeDto(
                e.getId(),
                e.getEmployeeCode(),
                e.getFirstName(),
                e.getLastName(),
                e.getFullName(),
                e.getWorkEmail(),
                e.getPersonalEmail(),
                e.getPhone(),
                e.getDateOfBirth(),
                e.getGender(),
                e.getMaritalStatus(),
                e.getAddressLine(),
                e.getCity(),
                e.getState(),
                e.getPostalCode(),
                e.getEmergencyContactName(),
                e.getEmergencyContactPhone(),
                e.getDepartment() != null ? e.getDepartment().getId() : null,
                e.getDepartment() != null ? e.getDepartment().getName() : null,
                e.getDesignation() != null ? e.getDesignation().getId() : null,
                e.getDesignation() != null ? e.getDesignation().getTitle() : null,
                e.getLocation() != null ? e.getLocation().getId() : null,
                e.getLocation() != null ? e.getLocation().getName() : null,
                e.getManager() != null ? e.getManager().getId() : null,
                e.getManager() != null ? e.getManager().getFullName() : null,
                e.getEmploymentType(),
                e.getStatus(),
                e.getDateOfJoining(),
                e.getProbationEndDate(),
                e.getDateOfExit(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public byte[] exportToExcel() {
        List<Employee> employees = employeeRepository.findAllWithRelations();
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Employees");

            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            String[] columns = {"ID", "Code", "Full Name", "Email", "Phone", "Department", "Designation", "Location", "Type", "Status", "Joined On"};
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Employee e : employees) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(e.getId());
                row.createCell(1).setCellValue(e.getEmployeeCode());
                row.createCell(2).setCellValue(e.getFullName());
                row.createCell(3).setCellValue(e.getWorkEmail());
                row.createCell(4).setCellValue(e.getPhone() != null ? e.getPhone() : "");
                row.createCell(5).setCellValue(e.getDepartment() != null ? e.getDepartment().getName() : "");
                row.createCell(6).setCellValue(e.getDesignation() != null ? e.getDesignation().getTitle() : "");
                row.createCell(7).setCellValue(e.getLocation() != null ? e.getLocation().getName() : "");
                row.createCell(8).setCellValue(e.getEmploymentType());
                row.createCell(9).setCellValue(e.getStatus());
                row.createCell(10).setCellValue(e.getDateOfJoining() != null ? e.getDateOfJoining().toString() : "");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new BusinessException("Failed to export employees to Excel: " + e.getMessage());
        }
    }
}

