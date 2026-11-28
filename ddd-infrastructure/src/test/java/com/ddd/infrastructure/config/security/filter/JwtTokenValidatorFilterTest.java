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
        assertEquals("{\"data\":null,\"message\":\"Token expired\",\"exceptionCode\":\"AUTH.004\"}", response.getContentAsString());
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

    @Test
    void expiredAccessCookieOnPublicGetEndpointPassesAnonymouslyWithout401() throws Exception {
        for (String uri : new String[]{"/api/events", "/api/fanpages"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
            request.setCookies(new Cookie("ddd_jwt", buildToken(-10_000L)));
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = mock(FilterChain.class);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertEquals(200, response.getStatus());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }

    @Test
    void expiredAccessCookieOnProtectedSubPathsUnderPublicPrefixStillReturns401() throws Exception {
        for (String uri : new String[]{"/api/events/me/stored", "/api/events/stored", "/api/fanpages/following", "/api/fanpages/my-fanpages"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
            request.setCookies(new Cookie("ddd_jwt", buildToken(-10_000L)));
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = mock(FilterChain.class);

            filter.doFilter(request, response, filterChain);

            verifyNoInteractions(filterChain);
            assertEquals(401, response.getStatus());
            assertEquals("{\"data\":null,\"message\":\"Token expired\",\"exceptionCode\":\"AUTH.004\"}", response.getContentAsString());
        }
    }

    @Test
    void blankAccessCookieDoesNotFailFilter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me/profile");
        request.setCookies(new Cookie("ddd_jwt", "   "));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void malformedAccessCookieOnProtectedEndpointDelegatesToEntryPointWith401Json() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me/profile");
        request.setCookies(new Cookie("ddd_jwt", "not.a.valid.jwt"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertEquals(401, response.getStatus());
        assertEquals("{\"data\":null,\"message\":\"Authentication required\",\"exceptionCode\":\"001\"}", response.getContentAsString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void adminEndpointsIncludeCatalogAndRoleEndpoints() {
        var matcher = new org.springframework.util.AntPathMatcher();
        String[] adminPaths = {
                "/api/schools/admin",
                "/api/schools/admin/1",
                "/api/majors/admin",
                "/api/majors/admin/2",
                "/api/event-types/admin",
                "/api/event-types/admin/3",
                "/api/associations/admin",
                "/api/associations/admin/4",
                "/api/roles",
                "/api/roles/5"
        };
        for (String path : adminPaths) {
            boolean matched = false;
            for (String pattern : com.ddd.infrastructure.constant.SecurityConstant.ADMIN_ENDPOINTS) {
                if (matcher.match(pattern, path)) {
                    matched = true;
                    break;
                }
            }
            assertTrue(matched, () -> "ADMIN_ENDPOINTS missing pattern for " + path);
        }
    }

    @Test
    void corsAllowedOriginsDoesNotContainNullOrigin() {
        for (String origin : com.ddd.infrastructure.constant.SecurityConstant.CORS_ALLOW_ORIGINS) {
            assertNotEquals("null", origin);
        }
    }
}
