package com.ddd.infrastructure.utils;

import com.ddd.infrastructure.config.properties.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil(
            new JwtProperties("bItuCsIDL5c+BspirFjkgEYurvJBLNosOfR6f7/wuRTF", 900_000L, "ddd_jwt", "Authorization", 604_800_000L, "ddd_refresh")
    );

    @Test
    void buildClearRefreshCookieMatchesRefreshCookiePathAndAttributes() {
        ResponseCookie issued = jwtUtil.buildRefreshCookie("raw-refresh-token", 604_800L);
        ResponseCookie cleared = jwtUtil.buildClearRefreshCookie();

        assertEquals(issued.getName(), cleared.getName());
        assertEquals("/api/auth", issued.getPath());
        assertEquals(issued.getPath(), cleared.getPath());
        assertEquals(issued.getSameSite(), cleared.getSameSite());
        assertTrue(cleared.isHttpOnly());
        assertTrue(cleared.isSecure());
        assertEquals(0L, cleared.getMaxAge().getSeconds());
        assertEquals("", cleared.getValue());
    }
}
