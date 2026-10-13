package com.company.hrms.auth.controller;

import com.company.hrms.auth.dto.CreateUserRequest;
import com.company.hrms.auth.dto.UpdateUserRolesRequest;
import com.company.hrms.auth.dto.UserDto;
import com.company.hrms.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users & Access Control", description = "Admin user provisioning and role assignment")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List all user accounts and their assigned roles (Admin only)")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user account details by ID (Admin only)")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    @Operation(summary = "Create login credentials for an employee and assign roles (Admin only)")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(req));
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Update privileges / roles for a user (Admin only)")
    public ResponseEntity<UserDto> updateUserRoles(@PathVariable Long id, @Valid @RequestBody UpdateUserRolesRequest req) {
        return ResponseEntity.ok(userService.updateUserRoles(id, req));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a user account (Admin only)")
    public ResponseEntity<UserDto> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean active = body.getOrDefault("active", true);
        return ResponseEntity.ok(userService.toggleUserStatus(id, active));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset password for a user (Admin only)")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        userService.resetPassword(id, newPassword);
        return ResponseEntity.noContent().build();
    }
}
