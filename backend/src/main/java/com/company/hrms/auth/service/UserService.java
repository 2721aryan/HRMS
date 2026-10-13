package com.company.hrms.auth.service;

import com.company.hrms.auth.dto.CreateUserRequest;
import com.company.hrms.auth.dto.UpdateUserRolesRequest;
import com.company.hrms.auth.dto.UserDto;
import com.company.hrms.auth.entity.Role;
import com.company.hrms.auth.entity.User;
import com.company.hrms.auth.repository.RoleRepository;
import com.company.hrms.auth.repository.UserRepository;
import com.company.hrms.common.exception.BusinessException;
import com.company.hrms.common.exception.ResourceNotFoundException;
import com.company.hrms.employee.entity.Employee;
import com.company.hrms.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return toDto(user);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest req) {
        Employee employee = employeeRepository.findById(req.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", req.employeeId()));

        if (userRepository.findByUsername(req.username()).isPresent()) {
            throw new BusinessException("Username is already taken: " + req.username());
        }

        if (userRepository.findAll().stream().anyMatch(u -> u.getEmployeeId().equals(req.employeeId()))) {
            throw new BusinessException("Employee already has a login account: " + employee.getFullName());
        }

        Set<Role> roles = resolveRoles(req.roles());

        User user = User.builder()
                .employeeId(employee.getId())
                .username(req.username())
                .passwordHash(passwordEncoder.encode(req.password()))
                .roles(roles)
                .active(true)
                .mustChangePassword(false)
                .build();

        return toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto updateUserRoles(Long userId, UpdateUserRolesRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Set<Role> roles = resolveRoles(req.roles());
        user.setRoles(roles);
        // Invalidate current JWT sessions so new roles take effect on re-login/refresh
        user.setTokenVersion(user.getTokenVersion() + 1);

        return toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto toggleUserStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setActive(active);
        if (!active) {
            user.setTokenVersion(user.getTokenVersion() + 1);
        }
        return toDto(userRepository.save(user));
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
    }

    private Set<Role> resolveRoles(Set<String> roleCodes) {
        Set<Role> roles = new HashSet<>();
        for (String code : roleCodes) {
            Role role = roleRepository.findByCode(code.trim().toUpperCase())
                    .orElseThrow(() -> new BusinessException("Invalid role code: " + code));
            roles.add(role);
        }
        return roles;
    }

    private UserDto toDto(User user) {
        Employee emp = employeeRepository.findById(user.getEmployeeId()).orElse(null);
        String code = emp != null ? emp.getEmployeeCode() : "N/A";
        String name = emp != null ? emp.getFullName() : user.getUsername();
        Set<String> roleCodes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());

        return new UserDto(
                user.getId(),
                user.getEmployeeId(),
                code,
                name,
                user.getUsername(),
                roleCodes,
                user.isActive(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
