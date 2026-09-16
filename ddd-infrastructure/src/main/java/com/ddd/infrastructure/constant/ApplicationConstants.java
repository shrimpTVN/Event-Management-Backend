package com.ddd.infrastructure.constant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApplicationConstants {

    // Configuration property keys
    public static final String JWT_SECRET_KEY = "jwt.secret";
    public static final String JWT_SECRET_ENV_KEY = "JWT_SECRET";
    public static final String JWT_EXPIRATION_KEY = "jwt.expiration-ms";
    public static final String JWT_EXPIRATION_ENV_KEY = "JWT_EXPIRATION_MS";
    public static final String JWT_COOKIE_NAME_KEY = "jwt.cookie-name";
    public static final String JWT_COOKIE_NAME_ENV_KEY = "JWT_COOKIE_NAME";
    public static final String JWT_HEADER_KEY = "jwt.header";
    public static final String JWT_HEADER_ENV_KEY = "JWT_HEADER";

    // Values loaded from application.yml and .env
    public static String JWT_SECRET = "jxgEQeXHuPq8VdbyYFNkANdudQ53YUn4";
    public static long JWT_EXPIRATION_MS = 86400000L;
    public static String JWT_COOKIE_NAME = "ddd_jwt";
    public static String JWT_HEADER = "Authorization";

    @Value("${jwt.secret:${JWT_SECRET:jxgEQeXHuPq8VdbyYFNkANdudQ53YUn4}}")
    public void setJwtSecret(String jwtSecret) {
        JWT_SECRET = jwtSecret;
    }

    @Value("${jwt.expiration-ms:${JWT_EXPIRATION_MS:86400000}}")
    public void setJwtExpirationMs(long jwtExpirationMs) {
        JWT_EXPIRATION_MS = jwtExpirationMs;
    }

    @Value("${jwt.cookie-name:${JWT_COOKIE_NAME:ddd_jwt}}")
    public void setJwtCookieName(String jwtCookieName) {
        JWT_COOKIE_NAME = jwtCookieName;
    }

    @Value("${jwt.header:${JWT_HEADER:Authorization}}")
    public void setJwtHeader(String jwtHeader) {
        JWT_HEADER = jwtHeader;
    }
}
