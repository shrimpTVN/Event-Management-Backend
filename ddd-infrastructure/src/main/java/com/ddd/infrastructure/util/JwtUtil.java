package com.ddd.infrastructure.util;

import com.ddd.infrastructure.config.security.custom.UserDetailsCustom;
import com.ddd.infrastructure.constant.ApplicationConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {
    private final Environment env;

    private SecretKey getSecretKey() {
        String secret = ApplicationConstants.JWT_SECRET;
        if (secret == null || secret.isBlank()) {
            secret = env.getProperty(ApplicationConstants.JWT_SECRET_KEY,
                    env.getProperty(ApplicationConstants.JWT_SECRET_ENV_KEY));
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public long getExpirationMs() {
        if (ApplicationConstants.JWT_EXPIRATION_MS > 0) {
            return ApplicationConstants.JWT_EXPIRATION_MS;
        }
        String exp = env.getProperty(ApplicationConstants.JWT_EXPIRATION_KEY,
                env.getProperty(ApplicationConstants.JWT_EXPIRATION_ENV_KEY));
        try {
            return exp != null ? Long.parseLong(exp) : 86400000L;
        } catch (NumberFormatException e) {
            return 86400000L;
        }
    }

    public String getCookieName() {
        String name = ApplicationConstants.JWT_COOKIE_NAME;
        if (name == null || name.isBlank()) {
            name = env.getProperty(ApplicationConstants.JWT_COOKIE_NAME_KEY,
                    env.getProperty(ApplicationConstants.JWT_COOKIE_NAME_ENV_KEY));
        }
        return name;
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
        return parseClaims(token).get("UserId", Long.class);
    }

    public String generateJwtToken(Authentication authentication) {
        UserDetailsCustom fetchedUser = (UserDetailsCustom) authentication.getPrincipal();
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        if (fetchedUser != null) {
            return Jwts.builder()
                    .issuer("ticket-booking")
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
}
