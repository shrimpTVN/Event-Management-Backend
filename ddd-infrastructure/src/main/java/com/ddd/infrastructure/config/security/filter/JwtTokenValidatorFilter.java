package com.ddd.infrastructure.config.security.filter;

import com.ddd.infrastructure.config.security.custom.UserDetailsCustom;
import com.ddd.infrastructure.constant.SecurityConstant;
import com.ddd.infrastructure.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
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
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

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
                Long userId = (Long) claims.get("userId");
                String authoritiesClaim = claims.get("roles").toString();

                UserDetailsCustom statelessPrincipal = new UserDetailsCustom(userId, username, "",
                        AuthorityUtils.commaSeparatedStringToAuthorityList(authoritiesClaim));

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        statelessPrincipal, null, statelessPrincipal.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token Expired");
                return;
            } catch( Exception e ){
                throw new BadCredentialsException("Invalid Token Received");
            }
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();

        // Duyệt qua mảng PUBLIC_ENDPOINTS, nếu path hiện tại khớp với bất kỳ pattern nào thì bỏ qua filter
        return Arrays.stream(SecurityConstant.PUBLIC_ENDPOINTS)
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}
