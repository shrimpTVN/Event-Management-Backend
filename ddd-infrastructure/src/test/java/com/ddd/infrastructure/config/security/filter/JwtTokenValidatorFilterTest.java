package com.ddd.infrastructure.config.security.filter;

import com.ddd.infrastructure.config.properties.JwtProperties;
import com.ddd.infrastructure.utils.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtTokenValidatorFilterTest {

    private static final String SECRET = "bItuCsIDL5c+BspirFjkgEYurvJBLNosOfR6f7/wuRTF";
    private JwtUtil jwtUtil;
    private JwtTokenValidatorFilter filter;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(SECRET, 900_000L, "ddd_jwt", "Authorization", 604_800_000L, "ddd_refresh");
        jwtUtil = new JwtUtil(properties);
        filter = new JwtTokenValidatorFilter(jwtUtil);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String buildToken(long expirationOffsetMs) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .issuer("ddd-application")
                .subject("JWT Token")
                .claim("username", "student@student.ctu.edu.vn")
                .claim("userId", 42L)
                .claim("roles", "ROLE_STUDENT")
                .issuedAt(new Date(now - 60_000L))
                .expiration(new Date(now + expirationOffsetMs))
                .signWith(key)
                .compact();
    }

    @Test
    void expiredAccessCookieOnRefreshEndpointPassesFilterWithout401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/refresh-token");
        request.setCookies(new Cookie("ddd_jwt", buildToken(-10_000L)), new Cookie("ddd_refresh", "refresh-123"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void expiredAccessCookieOnLoginAndLogoutPassesFilter() throws Exception {
        for (String uri : new String[]{"/api/auth/login", "/api/auth/logout"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
            request.setCookies(new Cookie("ddd_jwt", buildToken(-10_000L)));
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = mock(FilterChain.class);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    void expiredAccessCookieOnProtectedEndpointReturns401WithAuth004() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me/profile");
        request.setCookies(new Cookie("ddd_jwt", buildToken(-10_000L)));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertEquals(401, response.getStatus());
        assertEquals("{\"exceptionCode\":\"AUTH.004\",\"message\":\"Token expired\"}", response.getContentAsString());
    }

    @Test
    void validAccessCookieSetsSecurityContextAndPassesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me/profile");
        request.setCookies(new Cookie("ddd_jwt", buildToken(60_000L)));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("student@student.ctu.edu.vn", SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
