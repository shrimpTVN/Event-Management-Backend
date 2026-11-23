package com.ddd.application.service.user.impl;

import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.User;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountSecurityQueryServiceTest {

    @Mock
    private SecurityContextPort securityContextPort;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountSecurityQueryServiceImpl accountSecurityQueryService;

    @Test
    @DisplayName("shouldReturnTrueWhenUserHasNonBlankPassword")
    void shouldReturnTrueWhenUserHasNonBlankPassword() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setPassword("$2a$10$hashedPasswordHere");

        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        boolean hasPassword = accountSecurityQueryService.hasPassword();

        assertTrue(hasPassword);
        verify(securityContextPort).getCurrentUserId();
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("shouldReturnFalseWhenUserPasswordIsNull")
    void shouldReturnFalseWhenUserPasswordIsNull() {
        Long userId = 2L;
        User user = new User();
        user.setId(userId);
        user.setPassword(null);

        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        boolean hasPassword = accountSecurityQueryService.hasPassword();

        assertFalse(hasPassword);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n", "\r\n", "  \t \n "})
    @DisplayName("shouldReturnFalseWhenUserPasswordIsWhitespaceOnly")
    void shouldReturnFalseWhenUserPasswordIsWhitespaceOnly(String blankPassword) {
        Long userId = 3L;
        User user = new User();
        user.setId(userId);
        user.setPassword(blankPassword);

        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        boolean hasPassword = accountSecurityQueryService.hasPassword();

        assertFalse(hasPassword);
    }

    @Test
    @DisplayName("shouldThrowUnauthorizedExceptionWhenNotAuthenticated")
    void shouldThrowUnauthorizedExceptionWhenNotAuthenticated() {
        when(securityContextPort.getCurrentUserId()).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> accountSecurityQueryService.hasPassword());
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenUserNotFound")
    void shouldThrowResourceNotFoundExceptionWhenUserNotFound() {
        Long userId = 999L;
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountSecurityQueryService.hasPassword());
    }
}
