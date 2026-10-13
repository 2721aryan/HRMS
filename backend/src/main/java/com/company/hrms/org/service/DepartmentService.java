package com.company.hrms.org.service;

import com.company.hrms.common.exception.BusinessException;
import com.company.hrms.common.exception.ResourceNotFoundException;
import com.company.hrms.org.dto.CreateDepartmentRequest;
import com.company.hrms.org.dto.DepartmentDto;
import com.company.hrms.org.entity.Department;
import com.company.hrms.org.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<DepartmentDto> getAll() {
        return departmentRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public List<DepartmentDto> getAllActive() {
        return departmentRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    public DepartmentDto getById(Long id) {
        return departmentRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
    }

    @Transactional
    public DepartmentDto create(CreateDepartmentRequest req) {
        if (departmentRepository.existsByCode(req.code())) {
            throw new BusinessException("Department code already exists: " + req.code());
        }
        if (departmentRepository.existsByName(req.name())) {
            throw new BusinessException("Department name already exists: " + req.name());
        }

        Department dept = Department.builder()
                .code(req.code().toUpperCase())
                .name(req.name())
                .parentId(req.parentId())
                .headEmployeeId(req.headEmployeeId())
                .active(true)
                .build();

        return toDto(departmentRepository.save(dept));
    }

    @Transactional
    public DepartmentDto update(Long id, CreateDepartmentRequest req) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        dept.setName(req.name());
        dept.setParentId(req.parentId());
        dept.setHeadEmployeeId(req.headEmployeeId());
        dept.setUpdatedAt(Instant.now());

        return toDto(departmentRepository.save(dept));
    }

    @Transactional
    public DepartmentDto toggleActive(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        dept.setActive(!dept.isActive());
        dept.setUpdatedAt(Instant.now());
        return toDto(departmentRepository.save(dept));
    }

    private DepartmentDto toDto(Department d) {
        return new DepartmentDto(
                d.getId(),
                d.getCode(),
                d.getName(),
                d.getParentId(),
                d.getHeadEmployeeId(),
                d.isActive()
        );
    }
}
