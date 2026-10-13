package com.company.hrms.auth.controller;

import com.company.hrms.auth.dto.*;
import com.company.hrms.auth.security.UserDetailsImpl;
import com.company.hrms.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    private static final String REFRESH_COOKIE = "hrms_refresh";
    private static final int    COOKIE_MAX_AGE  = 12 * 60 * 60; // 12 hours in seconds

    @Operation(summary = "Login and obtain access + refresh tokens")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletResponse response) {
        LoginResponse body = authService.login(request);
        String rawRefresh = authService.generateRefreshToken(body.userId());
        addRefreshCookie(response, rawRefresh);
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Silently refresh access token using httpOnly cookie")
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(HttpServletRequest request) {
        String raw = extractRefreshCookie(request);
        LoginResponse refreshed = authService.refresh(raw);
        return ResponseEntity.ok(Map.of("accessToken", refreshed.accessToken()));
    }

    @Operation(summary = "Logout and revoke refresh token")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String raw = extractRefreshCookie(request);
        authService.logout(raw);
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Change password (requires current password)")
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserDetailsImpl user,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.getUserId(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get current authenticated user info")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LoginResponse> me(@AuthenticationPrincipal UserDetailsImpl user,
                                            HttpServletRequest request) {
        String raw = extractRefreshCookie(request);
        return ResponseEntity.ok(authService.refresh(raw));
    }

    // ─── Cookie helpers ──────────────────────────────────────────────────────
    private void addRefreshCookie(HttpServletResponse response, String raw) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, raw)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth")   // only sent to auth endpoints
                .maxAge(COOKIE_MAX_AGE)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true).path("/api/v1/auth").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_COOKIE.equals(c.getName()))
                .findFirst().map(Cookie::getValue).orElse(null);
    }
}
