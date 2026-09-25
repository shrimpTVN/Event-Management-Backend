package com.ddd.infrastructure.constant;

public class SecurityConstant {
    private SecurityConstant(){}

    public static final String[] PUBLIC_ENDPOINTS = {
            "/",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api/auth/register",
            "/api/auth/login",
            "/api/semesters/**"
    };

    public static final String[] ADMIN_ENDPOINTS = {
            "/api/semesters/admin/**",
            "/api/users/admin/**"
    };

    public static final String[] SECURE_ENDPOINTS = {
            "/api/**"
    };

    public static final String CORS_PATH_PATTERN = "/**";
    public static final String[] CORS_ALLOW_ORIGINS = {
            "http://localhost:*",
            "https://localhost:*",
    };

    public static final String[] CORS_ALLOWED_HEADERS = {"*"};
    public static final String CORS_EXPOSED_HEADER = "Access-Control-Allow-Origin";

    public static final String[] CORS_ALLOW_METHODS = {
            "OPTIONS",
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE"
    };
}
