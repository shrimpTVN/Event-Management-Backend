package com.ddd.infrastructure.constant;

public class SecurityConstant {
    private SecurityConstant(){}

    public static final String[] ADMIN_ENDPOINTS = {
            "/api/semesters/admin/**",
            "/api/users/admin/**",
            "/api/fanpages/admin/**",
            "/api/point-categories/admin/**",
            "/api/events/admin/**",
            "/api/criterias/admin/**"
    };

    public static final String[] FANPAGE_ADMIN_ENDPOINTS = {
            "/api/fanpages/fanpage-admin/**"
    };

    public static final String[] PUBLIC_ENDPOINTS = {
            "/",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/auth/register",
            "/auth/login",
            "/auth/register/fanpage-admin",
            "/auth/refresh-token",
            "/auth/logout",
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/register/fanpage-admin",
            "/api/auth/refresh-token",
            "/api/auth/logout",
            "/api/semesters/**",
            "/api/fanpages/**",
            "/api/point-categories/**",
            "/api/events/**",
            "/api/criterias/**",
            "/academic-years/**",
            "/api/academic-years/**",
    };

    public static final String[] SECURE_ENDPOINTS = {
            "/api/events/fanpage-member/**",
            "/api/fanpages/*/follow",
            "/api/fanpages/*/unfollow",
            "/api/fanpages/following",
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
