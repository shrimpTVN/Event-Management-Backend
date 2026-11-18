package com.ddd.application.service.impl;

import com.ddd.application.dto.user.UserDto;
import com.ddd.application.service.googleTokenVerifier.GoogleTokenVerifier;
import com.ddd.application.service.otp.OtpCommandService;
import com.ddd.application.service.user.UserCommandService;
import com.ddd.application.service.userprofile.FanpageAdminProfileCommandService;
import com.ddd.application.service.userprofile.StudentProfileCommandService;
import com.ddd.domain.enums.RoleEnum;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.repository.RefreshTokenRepository;
import com.ddd.domain.repository.UserRepository;
import com.ddd.infrastructure.utils.JwtUtil;
import com.ddd.infrastructure.utils.TokenHashUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserCommandService userCommandService;
    @Mock
    private StudentProfileCommandService studentProfileCommandService;
    @Mock
    private FanpageAdminProfileCommandService fanpageAdminProfileCommandService;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private GoogleTokenVerifier googleTokenVerifier;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private TokenHashUtil tokenHashUtil;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OtpCommandService otpCommandService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Nested
    @DisplayName("Register with Email + Password + OTP Tests")
    class RegisterTests {

        @Test
        @DisplayName("shouldRegisterSuccessfullyWithValidOtp: verifies OTP and creates user without profile")
        void shouldRegisterSuccessfullyWithValidOtp() {
            UserDto userDto = new UserDto(
                    "student@example.com",
                    "password123",
                    null,
                    "John",
                    "Doe",
                    null,
                    null
            );
            String otpCode = "123456";

            doNothing().when(otpCommandService).verifyOtp("student@example.com", otpCode);
            when(userCommandService.createUser(userDto, RoleEnum.STUDENT.name())).thenReturn(1L);

            authService.register(userDto, otpCode);

            verify(otpCommandService).verifyOtp("student@example.com", otpCode);
            verify(userCommandService).createUser(userDto, RoleEnum.STUDENT.name());
            verifyNoInteractions(studentProfileCommandService);
        }

        @Test
        @DisplayName("shouldThrowExceptionWhenOtpInvalid: fails fast when OTP is invalid")
        void shouldThrowExceptionWhenOtpInvalid() {
            UserDto userDto = new UserDto(
                    "student@example.com",
                    "password123",
                    null,
                    "John",
                    "Doe",
                    null,
                    null
            );
            String invalidOtp = "000000";

            doThrow(new BusinessException("Invalid OTP code."))
                    .when(otpCommandService).verifyOtp("student@example.com", invalidOtp);

            assertThrows(BusinessException.class, () -> authService.register(userDto, invalidOtp));

            verify(otpCommandService).verifyOtp("student@example.com", invalidOtp);
            verifyNoInteractions(userCommandService);
            verifyNoInteractions(studentProfileCommandService);
        }
    }
}
