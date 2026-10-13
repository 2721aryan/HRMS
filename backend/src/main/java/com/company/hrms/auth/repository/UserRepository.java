package com.company.hrms.auth.repository;

import com.company.hrms.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmployeeId(Long employeeId);
    boolean existsByUsername(String username);

    @Modifying
    @Query("UPDATE User u SET u.tokenVersion = u.tokenVersion + 1 WHERE u.id = :userId")
    void incrementTokenVersion(Long userId);

    @Modifying
    @Query("UPDATE User u SET u.failedAttempts = 0, u.lockedUntil = null WHERE u.id = :userId")
    void resetFailedAttempts(Long userId);
}
