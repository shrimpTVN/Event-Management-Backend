package com.ddd.infrastructure.config.security.custom;

import com.ddd.domain.model.User;
import com.ddd.domain.repository.RoleRepository;
import com.ddd.domain.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserServiceCustom implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public UserDetailsCustom loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException(email);
        }

        String roleName = roleRepository.findById(user.getRoleId()).getName().toUpperCase();
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }

        GrantedAuthority authority = new SimpleGrantedAuthority(roleName);
        return new UserDetailsCustom(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                roleName,
                Collections.singletonList(authority));
    }

}
