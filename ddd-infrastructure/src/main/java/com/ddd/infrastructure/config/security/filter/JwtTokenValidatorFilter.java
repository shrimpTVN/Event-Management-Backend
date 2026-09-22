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
    private final JwtUtil jwtUtil;

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
                String authoritiesClaim = claims.get("roles").toString();

                UserDetailsCustom statelessPrincipal = new UserDetailsCustom(userId, username, authoritiesClaim,"",
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
