package com.ddd.infrastructure.adapter.security;

import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.infrastructure.config.security.custom.UserDetailsCustom;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;
@Component
public class SpringSecurityContextAdaptor implements SecurityContextPort {
    @Override
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsCustom custom){
            return  custom.getUserId();
        }
        return null;
    }

    @Override
    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    @Override
    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> Objects.requireNonNull(a.getAuthority()).equalsIgnoreCase("ROLE_"+role));

    }

    @Override
    public boolean isAuthenticated() {
        return getCurrentUserId() != null;
    }
}

