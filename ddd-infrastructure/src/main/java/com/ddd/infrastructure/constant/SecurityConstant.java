package com.ddd.infrastructure.constant;

public class SecurityConstant {
    private SecurityConstant(){}

    public static final String[] ADMIN_ENDPOINTS = {
            "/api/semesters/admin/**",
            "/api/users/admin/**",
            "/users/admin/**",
            "/api/fanpages/admin/**",
            "/api/point-categories/admin/**",
            "/api/events/admin/**",
            "/api/criterias/admin/**",
            "/api/schools/admin/**",
            "/api/majors/admin/**",
            "/api/event-types/admin/**",
            "/api/associations/admin/**",
            "/api/roles/**",
            "/admin/statistics/**",
            "/api/admin/statistics/**"
    };

    public static final String[] FANPAGE_ADMIN_ENDPOINTS = {
            "/api/fanpages/fanpage-admin/**"
    };

    public static final String[] PUBLIC_ENDPOINTS = {
            "/",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/auth/register/fanpage-admin",
            "/auth/refresh-token",
            "/auth/logout",
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/login/google",
            "/api/auth/register/fanpage-admin",
            "/api/auth/send-otp",
            "/api/auth/verify-otp",
            "/api/auth/refresh-token",
            "/api/auth/logout",
            "/auth/**",
            "/api/semesters/**",
            "/api/fanpages/**",
            "/api/point-categories/**",
            "/api/events/**",
            "/api/criterias/**",
            "/academic-years/**",
            "/api/academic-years/**",
            "/google-test.html",
            "/test-upload.html",
            "/test-recommendation.html"
    };

    public static final String[] SECURE_ENDPOINTS = {
            "/auth/create-password",
            "/api/auth/create-password",
            "/auth/change-password",
            "/api/auth/change-password",
            "/users/me/security",
            "/api/users/me/security",
            "/users/me/staff-profile",
            "/api/users/me/staff-profile",
            "/api/events/fanpage-member/**",
            "/api/media/**",
            "/media/**",
            "/api/fanpages/*/follow",
            "/api/fanpages/*/unfollow",
            "/api/fanpages/following",
            "/events/*/store",
            "/api/events/*/store",
            "/events/*/is-stored",
            "/api/events/*/is-stored",
            "/events/me/stored",
            "/api/events/me/stored",
            "/events/stored",
            "/api/events/stored",
            "/fanpages/my-fanpages",
            "/api/fanpages/my-fanpages",
            "/events/views",
            "/api/events/views",
            "/events/recommendations",
            "/api/events/recommendations",
    };

    public static final String CORS_PATH_PATTERN = "/**";
    public static final String[] CORS_ALLOW_ORIGINS = {
            "http://localhost:*",
            "https://localhost:*"
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
