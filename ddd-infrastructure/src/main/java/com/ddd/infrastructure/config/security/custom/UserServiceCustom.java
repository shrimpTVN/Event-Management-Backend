package com.ddd.infrastructure.config.security.custom;

import com.ddd.domain.model.User;
import com.ddd.domain.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserServiceCustom implements UserDetailsService {

    private final UserRepository userRepository;


    @Override
    public UserDetailsCustom loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        User user  = userRepository.findByUsername(username);

        String roleName = user.getRole().toUpperCase();
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_"+roleName;
        }
        GrantedAuthority authority = new SimpleGrantedAuthority(roleName);
        return new UserDetailsCustom(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(authority));
    }

}
