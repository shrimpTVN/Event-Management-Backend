package com.ddd.infrastructure.utils;

import com.ddd.infrastructure.config.properties.JwtProperties;
import com.ddd.infrastructure.config.security.custom.UserDetailsCustom;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import com.ddd.domain.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {
    private final JwtProperties jwtProperties;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public long getExpirationMs() {
        return jwtProperties.expirationMs();
    }

    public String getCookieName() {
        return jwtProperties.cookieName();
    }

    public String extractToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        String cookieName = getCookieName();
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(cookieName)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public Boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Token Invalid: {}", e.getMessage());
            return false;
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserIdFromToken(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    public String generateJwtToken(Authentication authentication) {
        UserDetailsCustom fetchedUser = (UserDetailsCustom) authentication.getPrincipal();
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        if (fetchedUser != null) {
            return Jwts.builder()
                    .issuer("ddd-application")
                    .subject("JWT Token")
                    .claim("username", fetchedUser.getUsername())
                    .claim("userId", fetchedUser.getUserId())
                    .claim("roles", roles)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + getExpirationMs()))
                    .signWith(getSecretKey())
                    .compact();
        }
        throw new IllegalArgumentException("Invalid authentication information");
    }

    public long getRefreshExpirationMs() {
        return jwtProperties.refreshExpirationMs();
    }

    public String getRefreshCookieName() {
        return jwtProperties.refreshCookieName();
    }

    public String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        String cookieName = getRefreshCookieName();
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(cookieName)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public ResponseCookie buildRefreshCookie(String rawRefreshToken, long maxAgeSeconds) {
        return ResponseCookie.from(getRefreshCookieName(), rawRefreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAgeSeconds)
                .build();
    }

    public ResponseCookie buildClearRefreshCookie() {
        return ResponseCookie.from(getRefreshCookieName(), "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/auth")
                .maxAge(0)
                .build();
    }

    public String generateJwtTokenFromUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        //Parse roles to ensure they are in the correct format for JWT claims
        String roles = user.getRoles() != null
                ? user.getRoles().stream()
                        .map(role -> {
                            String name = role.getName().toUpperCase();
                            return name.startsWith("ROLE_") ? name : "ROLE_" + name;
                        })
                        .collect(Collectors.joining(","))
                : "";

        return Jwts.builder()
                .issuer("ddd-application")
                .subject("JWT Token")
                .claim("username", user.getEmail())
                .claim("userId", user.getId())
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + getExpirationMs()))
                .signWith(getSecretKey())
                .compact();
    }
}
