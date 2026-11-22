package com.ddd.application.service.impl;

import com.ddd.application.dto.auth.GoogleUserInfo;
import com.ddd.application.dto.auth.LoginResult;
import com.ddd.application.dto.user.UserDto;
import com.ddd.application.service.googleTokenVerifier.GoogleTokenVerifier;
import com.ddd.application.service.otp.OtpCommandService;
import com.ddd.application.service.user.UserCommandService;
import com.ddd.application.service.userprofile.FanpageAdminProfileCommandService;
import com.ddd.application.service.userprofile.StudentProfileCommandService;
import com.ddd.domain.enums.RoleEnum;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.model.RefreshToken;
import com.ddd.domain.model.Role;
import com.ddd.domain.model.User;
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

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
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

    @Nested
    @DisplayName("Google OAuth2 Login & JIT Provisioning Tests")
    class GoogleOAuth2Tests {

        @Test
        @DisplayName("shouldAutoProvisionNewUserOnGoogleLogin: creates User with STUDENT role if not exists")
        void shouldAutoProvisionNewUserOnGoogleLogin() {
            String idToken = "valid-google-id-token";
            GoogleUserInfo googleUserInfo = GoogleUserInfo.builder()
                    .email("newgoogleuser@example.com")
                    .googleId("google-sub-12345")
                    .firstName("Jane")
                    .lastName("Doe")
                    .pictureUrl("https://example.com/photo.jpg")
                    .build();

            when(googleTokenVerifier.verifyToken(idToken)).thenReturn(googleUserInfo);
            when(userRepository.findByEmail("newgoogleuser@example.com")).thenReturn(null);

            UserDto expectedUserDto = new UserDto(
                    "newgoogleuser@example.com",
                    null,
                    "google-sub-12345",
                    "Jane",
                    "Doe",
                    null,
                    "https://example.com/photo.jpg"
            );
            when(userCommandService.createUser(expectedUserDto, RoleEnum.STUDENT.name())).thenReturn(10L);

            User provisionedUser = new User();
            provisionedUser.setId(10L);
            provisionedUser.setEmail("newgoogleuser@example.com");
            provisionedUser.setProviderId("google-sub-12345");
            Role studentRole = new Role();
            studentRole.setId(1L);
            studentRole.setName(RoleEnum.STUDENT.name());
            provisionedUser.setRoles(Set.of(studentRole));

            when(userRepository.findById(10L)).thenReturn(Optional.of(provisionedUser));
            when(jwtUtil.generateJwtTokenFromUser(provisionedUser)).thenReturn("mock-jwt-token");
            when(tokenHashUtil.hash(anyString())).thenReturn("hashed-refresh-token");
            when(jwtUtil.getRefreshExpirationMs()).thenReturn(604800000L);

            LoginResult result = authService.loginWithGoogle(idToken);

            assertNotNull(result);
            assertEquals("mock-jwt-token", result.jwtToken());
            assertEquals(10L, result.loginDto().userId());
            assertEquals("newgoogleuser@example.com", result.loginDto().email());
            assertTrue(result.loginDto().roles().contains(RoleEnum.STUDENT.name()));

            verify(userCommandService).createUser(expectedUserDto, RoleEnum.STUDENT.name());
            verify(refreshTokenRepository).save(any(RefreshToken.class));
            verifyNoInteractions(studentProfileCommandService);
        }

        @Test
        @DisplayName("shouldLoginExistingGoogleUser: updates providerId if missing and returns tokens")
        void shouldLoginExistingGoogleUser() {
            String idToken = "valid-google-id-token";
            GoogleUserInfo googleUserInfo = GoogleUserInfo.builder()
                    .email("existing@example.com")
                    .googleId("google-sub-99999")
                    .firstName("Existing")
                    .lastName("User")
                    .pictureUrl("https://example.com/existing.jpg")
                    .build();

            User existingUser = new User();
            existingUser.setId(5L);
            existingUser.setEmail("existing@example.com");
            existingUser.setProviderId(null);
            Role studentRole = new Role();
            studentRole.setId(1L);
            studentRole.setName(RoleEnum.STUDENT.name());
            existingUser.setRoles(Set.of(studentRole));

            when(googleTokenVerifier.verifyToken(idToken)).thenReturn(googleUserInfo);
            when(userRepository.findByEmail("existing@example.com")).thenReturn(existingUser);
            when(jwtUtil.generateJwtTokenFromUser(existingUser)).thenReturn("existing-jwt-token");
            when(tokenHashUtil.hash(anyString())).thenReturn("hashed-refresh-token");
            when(jwtUtil.getRefreshExpirationMs()).thenReturn(604800000L);

            LoginResult result = authService.loginWithGoogle(idToken);

            assertNotNull(result);
            assertEquals("existing-jwt-token", result.jwtToken());
            assertEquals("google-sub-99999", existingUser.getProviderId());
            verify(userRepository).update(existingUser);
            verifyNoInteractions(userCommandService);
        }
    }
}
