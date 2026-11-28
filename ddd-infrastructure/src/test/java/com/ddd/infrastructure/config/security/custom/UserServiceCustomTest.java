package com.ddd.infrastructure.config.security.custom;

import com.ddd.domain.model.Role;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceCustomTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceCustom userServiceCustom;

    @Test
    void loadUserByUsernameReflectsInactiveStatusInIsEnabled() {
        User user = new User();
        user.setId(99L);
        user.setEmail("disabled@student.ctu.edu.vn");
        user.setPassword("encoded");
        user.setActive(false);
        Role studentRole = new Role();
        studentRole.setName("STUDENT");
        user.setRoles(Set.of(studentRole));

        when(userRepository.findByEmail("disabled@student.ctu.edu.vn")).thenReturn(user);

        UserDetailsCustom details = userServiceCustom.loadUserByUsername("disabled@student.ctu.edu.vn");

        assertFalse(details.isEnabled());
    }

    @Test
    void loadUserByUsernameReflectsActiveStatusInIsEnabled() {
        User user = new User();
        user.setId(100L);
        user.setEmail("active@student.ctu.edu.vn");
        user.setPassword("encoded");
        user.setActive(true);
        Role studentRole = new Role();
        studentRole.setName("STUDENT");
        user.setRoles(Set.of(studentRole));

        when(userRepository.findByEmail("active@student.ctu.edu.vn")).thenReturn(user);

        UserDetailsCustom details = userServiceCustom.loadUserByUsername("active@student.ctu.edu.vn");

        assertTrue(details.isEnabled());
    }
}
