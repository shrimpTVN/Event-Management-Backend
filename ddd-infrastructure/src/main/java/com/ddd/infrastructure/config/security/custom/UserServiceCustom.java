package com.ddd.infrastructure.config.security.custom;

import com.ddd.domain.model.User;
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


    @Override
    public UserDetailsCustom loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        User user  = userRepository.findByEmail(email);
        if (user == null){
            throw new UsernameNotFoundException(email);
        }

        // TODO: Load role name from Role entity via roleId when Role repository is available
        String roleName = "ROLE_USER";
        GrantedAuthority authority = new SimpleGrantedAuthority(roleName);
        return new UserDetailsCustom(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getEmail(),
                roleName,
                Collections.singletonList(authority));
    }

}
