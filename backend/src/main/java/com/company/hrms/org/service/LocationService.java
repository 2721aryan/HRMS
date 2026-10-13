package com.company.hrms.org.service;

import com.company.hrms.org.dto.LocationDto;
import com.company.hrms.org.entity.Location;
import com.company.hrms.org.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;

    public List<LocationDto> getAllActive() {
        return locationRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    private LocationDto toDto(Location loc) {
        return new LocationDto(
                loc.getId(),
                loc.getName(),
                loc.getCity(),
                loc.getState(),
                loc.getCountry(),
                loc.isActive()
        );
    }
}
