package com.ddd.infrastructure.config.security.custom;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

@Getter
public class UserDetailsCustom implements UserDetails {
    private final Long userId;
    private final String username;
    private final String password;
    private final Set<String> roles;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsCustom(Long userId, String email, String password, Set<String> roles,
                             Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.password = password;
        this.username = email;
        this.roles = roles != null ? roles : Collections.emptySet();
        this.authorities = authorities;
    }

    /**
     * Backward-compatible constructor accepting single role.
     */
    public UserDetailsCustom(Long userId, String email, String password, String role,
                             Collection<? extends GrantedAuthority> authorities) {
        this(userId, email, password, role != null ? Collections.singleton(role) : Collections.emptySet(), authorities);
    }

    /**
     * Lightweight constructor for stateless principal reconstruction from JWT claims.
     */
    public UserDetailsCustom(Long userId, String email, Set<String> roles,
                             Collection<? extends GrantedAuthority> authorities) {
        this(userId, email, "", roles, authorities);
    }

    /**
     * Backward-compatible lightweight constructor for single role.
     */
    public UserDetailsCustom(Long userId, String email, String role,
                             Collection<? extends GrantedAuthority> authorities) {
        this(userId, email, "", role != null ? Collections.singleton(role) : Collections.emptySet(), authorities);
    }

    /**
     * Backward-compatible getter returning primary role.
     */
    public String getRole() {
        return (roles != null && !roles.isEmpty()) ? roles.iterator().next() : null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
