package com.ddd.infrastructure.config.security.filter;

import com.ddd.infrastructure.config.security.custom.UserDetailsCustom;
import com.ddd.infrastructure.config.security.handler.JwtAuthenticationEntryPoint;
import com.ddd.infrastructure.constant.SecurityConstant;
import com.ddd.infrastructure.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

public class JwtTokenValidatorFilter extends OncePerRequestFilter {
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String[] PROTECTED_PUBLIC_PREFIX_OVERRIDES = {
            "/events/stored",
            "/api/events/stored",
            "/fanpages/my-fanpages",
            "/api/fanpages/my-fanpages"
    };
    private static final String[] AUTH_BYPASS_PATHS = {
            "/auth/refresh-token",
            "/api/auth/refresh-token",
            "/auth/login",
            "/api/auth/login",
            "/auth/login/google",
            "/api/auth/login/google",
            "/auth/register",
            "/api/auth/register",
            "/auth/register/fanpage-admin",
            "/api/auth/register/fanpage-admin",
            "/auth/logout",
            "/api/auth/logout",
            "/auth/send-otp",
            "/api/auth/send-otp",
            "/auth/verify-otp",
            "/api/auth/verify-otp",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private final JwtUtil jwtUtil;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtTokenValidatorFilter(JwtUtil jwtUtil) {
        this(jwtUtil, new JwtAuthenticationEntryPoint());
    }

    public JwtTokenValidatorFilter(JwtUtil jwtUtil, AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtUtil = jwtUtil;
        this.authenticationEntryPoint = authenticationEntryPoint != null
                ? authenticationEntryPoint
                : new JwtAuthenticationEntryPoint();
    }

    private String resolveRequestPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return (servletPath != null && !servletPath.isEmpty()) ? servletPath : request.getRequestURI();
    }

    private static boolean matchesAny(String path, String[] patterns) {
        for (String pattern : patterns) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPublicGuestRequest(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return false;
        }
        String path = resolveRequestPath(request);
        if (path == null) {
            return false;
        }
        if (matchesAny(path, SecurityConstant.ADMIN_ENDPOINTS)
                || matchesAny(path, SecurityConstant.FANPAGE_ADMIN_ENDPOINTS)
                || matchesAny(path, SecurityConstant.SECURE_ENDPOINTS)
                || matchesAny(path, PROTECTED_PUBLIC_PREFIX_OVERRIDES)) {
            return false;
        }
        return matchesAny(path, SecurityConstant.PUBLIC_ENDPOINTS);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = resolveRequestPath(request);
        if (path == null) {
            return false;
        }
        return matchesAny(path, AUTH_BYPASS_PATHS);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String jwt = jwtUtil.extractToken(request);

        if (jwt != null && !jwt.isBlank()) {
            try {
                Claims claims = jwtUtil.parseClaims(jwt);

                String username = claims.get("username").toString();
                // Safe cast: JWT library may parse small numbers as Integer
                Number userIdNum = (Number) claims.get("userId");
                Long userId = userIdNum != null ? userIdNum.longValue() : null;
                Object rolesObj = claims.get("roles");
                String authoritiesClaim = rolesObj != null ? rolesObj.toString() : "";

                java.util.Set<String> roleSet = Arrays.stream(authoritiesClaim.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(java.util.stream.Collectors.toSet());

                UserDetailsCustom statelessPrincipal = new UserDetailsCustom(
                        userId,
                        username,
                        roleSet,
                        AuthorityUtils.commaSeparatedStringToAuthorityList(authoritiesClaim));

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        statelessPrincipal, null, statelessPrincipal.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException e) {
                SecurityContextHolder.clearContext();
                if (isPublicGuestRequest(request)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write(
                        "{\"data\":null,\"message\":\"Token expired\",\"exceptionCode\":\"AUTH.004\"}");
                return;
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                if (isPublicGuestRequest(request)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                authenticationEntryPoint.commence(
                        request,
                        response,
                        new BadCredentialsException("Invalid Token Received", e));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
