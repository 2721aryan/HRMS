package com.company.hrms.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    private SecretKey signingKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception e) {
            keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UserDetailsImpl user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId",       user.getUserId())
                .claim("employeeId",   user.getEmployeeId())
                .claim("tokenVersion", user.getTokenVersion())
                .claim("roles",        user.getRoles())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiryMs))
                .signWith(signingKey())
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token)     { return extractClaims(token).getSubject(); }
    public Long   extractUserId(String token)       { return extractClaims(token).get("userId", Long.class); }
    public int    extractTokenVersion(String token) { return extractClaims(token).get("tokenVersion", Integer.class); }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token)  { return extractClaims(token).get("roles", List.class); }
}
