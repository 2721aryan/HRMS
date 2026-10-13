package com.company.hrms.auth.security;

import com.company.hrms.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService      jwtService;
    private final UserRepository  userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        final String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        final String token = header.substring(7);
        if (!jwtService.isValid(token)) {
            chain.doFilter(request, response);
            return;
        }

        final Long userId = jwtService.extractUserId(token);
        final int  tokenVersion = jwtService.extractTokenVersion(token);

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            userRepository.findById(userId).ifPresent(user -> {
                // Verify token version (instant invalidation) and is_active
                if (user.getTokenVersion() == tokenVersion && user.isActive()) {
                    UserDetailsImpl userDetails = UserDetailsImpl.build(user);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            });
        }

        chain.doFilter(request, response);
    }
}
