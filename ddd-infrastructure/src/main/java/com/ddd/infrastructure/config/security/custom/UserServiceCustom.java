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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceCustom implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetailsCustom loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException(email);
        }

        Set<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream()
                    .map(role -> {
                        String name = role.getName().toUpperCase();
                        return name.startsWith("ROLE_") ? name : "ROLE_" + name;
                    })
                    .collect(Collectors.toSet())
                : Collections.emptySet();

        List<GrantedAuthority> authorities = roleNames.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new UserDetailsCustom(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                roleNames,
                authorities);
    }

}
