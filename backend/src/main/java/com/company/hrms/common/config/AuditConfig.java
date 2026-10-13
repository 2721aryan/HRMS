package com.company.hrms.common.config;

import com.company.hrms.auth.security.UserDetailsImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
public class AuditConfig {

    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserDetailsImpl)) {
                return Optional.empty();
            }
            return Optional.of(((UserDetailsImpl) auth.getPrincipal()).getUserId());
        };
    }
}
