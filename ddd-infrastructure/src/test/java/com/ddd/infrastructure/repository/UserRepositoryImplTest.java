package com.ddd.infrastructure.repository;

import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.mapper.UserMapper;
import com.ddd.infrastructure.repository.jpaRepository.RoleJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleJpaRepository roleJpaRepository;

    @InjectMocks
    private UserRepositoryImpl userRepository;

    @Test
    @DisplayName("setPasswordIfAbsent should set password and return true when password is null")
    void setPasswordIfAbsent_whenPasswordIsNull() {
        Long userId = 1L;
        String encodedPassword = "encodedPassword123";
        UserJpaEntity user = new UserJpaEntity();
        user.setId(userId);
        user.setPassword(null);

        when(userJpaRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));

        boolean result = userRepository.setPasswordIfAbsent(userId, encodedPassword);

        assertTrue(result);
        assertEquals(encodedPassword, user.getPassword());
        verify(userJpaRepository).save(user);
    }

    @Test
    @DisplayName("setPasswordIfAbsent should set password and return true when password is blank")
    void setPasswordIfAbsent_whenPasswordIsBlank() {
        Long userId = 2L;
        String encodedPassword = "encodedPassword123";
        UserJpaEntity user = new UserJpaEntity();
        user.setId(userId);
        user.setPassword("   ");

        when(userJpaRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));

        boolean result = userRepository.setPasswordIfAbsent(userId, encodedPassword);

        assertTrue(result);
        assertEquals(encodedPassword, user.getPassword());
        verify(userJpaRepository).save(user);
    }

    @Test
    @DisplayName("setPasswordIfAbsent should return false and not overwrite when password exists")
    void setPasswordIfAbsent_whenPasswordAlreadyExists() {
        Long userId = 3L;
        String existingPassword = "existingHashedPassword";
        String newEncodedPassword = "newEncodedPassword123";
        UserJpaEntity user = new UserJpaEntity();
        user.setId(userId);
        user.setPassword(existingPassword);

        when(userJpaRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));

        boolean result = userRepository.setPasswordIfAbsent(userId, newEncodedPassword);

        assertFalse(result);
        assertEquals(existingPassword, user.getPassword());
        verify(userJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("setPasswordIfAbsent should throw ResourceNotFoundException when user not found")
    void setPasswordIfAbsent_whenUserNotFound() {
        Long userId = 404L;
        when(userJpaRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userRepository.setPasswordIfAbsent(userId, "newPass"));
        verify(userJpaRepository, never()).save(any());
    }
}
