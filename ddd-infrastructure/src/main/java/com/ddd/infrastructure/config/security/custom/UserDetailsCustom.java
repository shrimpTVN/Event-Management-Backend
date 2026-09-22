package com.ddd.infrastructure.config.security.custom;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
public class UserDetailsCustom implements UserDetails {
    private final Long userId;
    private final String username;
    private final String password;
    private final String email;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsCustom(Long userId, String username, String password, String email, String role,
                             Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
        this.authorities = authorities;
    }

    /**
     * Lightweight constructor for stateless principal reconstruction from JWT claims.
     * Used in JwtTokenValidatorFilter where name/email/role are not needed.
     */
    public UserDetailsCustom(Long userId, String username, String password,
                             Collection<? extends GrantedAuthority> authorities) {
        this(userId, username, password, null, null, authorities);
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
