package com.ddd.infrastructure.config.security.filter;

import com.ddd.infrastructure.config.security.custom.UserDetailsCustom;
import com.ddd.infrastructure.constant.SecurityConstant;
import com.ddd.infrastructure.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@RequiredArgsConstructor
public class JwtTokenValidatorFilter extends OncePerRequestFilter {
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
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

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String path = (servletPath != null && !servletPath.isEmpty()) ? servletPath : request.getRequestURI();
        if (path == null) {
            return false;
        }
        for (String pattern : AUTH_BYPASS_PATHS) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String jwt = jwtUtil.extractToken(request);

        if (jwt!=null){
            try {
                Claims claims=jwtUtil.parseClaims(jwt);

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
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write(
                        "{\"exceptionCode\":\"AUTH.004\",\"message\":\"Token expired\"}");
                return;
            } catch( Exception e ){
                throw new BadCredentialsException("Invalid Token Received");
            }
        }
        filterChain.doFilter(request, response);
    }
}
