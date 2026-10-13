package com.company.hrms.org.service;

import com.company.hrms.common.exception.BusinessException;
import com.company.hrms.common.exception.ResourceNotFoundException;
import com.company.hrms.org.dto.CreateDesignationRequest;
import com.company.hrms.org.dto.DesignationDto;
import com.company.hrms.org.entity.Designation;
import com.company.hrms.org.repository.DesignationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DesignationService {

    private final DesignationRepository designationRepository;

    public List<DesignationDto> getAll() {
        return designationRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public List<DesignationDto> getAllActive() {
        return designationRepository.findAllByActiveTrueOrderByTitleAsc().stream()
                .map(this::toDto)
                .toList();
    }

    public DesignationDto getById(Long id) {
        return designationRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", id));
    }

    @Transactional
    public DesignationDto create(CreateDesignationRequest req) {
        if (designationRepository.existsByTitle(req.title())) {
            throw new BusinessException("Designation title already exists: " + req.title());
        }

        Designation desig = Designation.builder()
                .title(req.title())
                .grade(req.grade())
                .active(true)
                .build();

        return toDto(designationRepository.save(desig));
    }

    @Transactional
    public DesignationDto update(Long id, CreateDesignationRequest req) {
        Designation desig = designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", id));

        desig.setTitle(req.title());
        desig.setGrade(req.grade());
        desig.setUpdatedAt(Instant.now());

        return toDto(designationRepository.save(desig));
    }

    @Transactional
    public DesignationDto toggleActive(Long id) {
        Designation desig = designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", id));
        desig.setActive(!desig.isActive());
        desig.setUpdatedAt(Instant.now());
        return toDto(designationRepository.save(desig));
    }

    private DesignationDto toDto(Designation d) {
        return new DesignationDto(d.getId(), d.getTitle(), d.getGrade(), d.isActive());
    }
}
