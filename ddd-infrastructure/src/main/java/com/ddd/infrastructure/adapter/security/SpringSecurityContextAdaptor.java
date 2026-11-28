package com.ddd.infrastructure.adapter.security;

import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.infrastructure.config.security.custom.UserDetailsCustom;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityContextAdaptor implements SecurityContextPort {
    @Override
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsCustom custom) {
            return custom.getUserId();
        }
        return null;
    }

    @Override
    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return auth.getName();
    }

    @Override
    public boolean hasRole(String role) {
        if (role == null) return false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(java.util.Objects::nonNull)
                .anyMatch(authority -> authority.equalsIgnoreCase("ROLE_" + role));
    }

    @Override
    public boolean isAuthenticated() {
        return getCurrentUserId() != null;
    }
}

