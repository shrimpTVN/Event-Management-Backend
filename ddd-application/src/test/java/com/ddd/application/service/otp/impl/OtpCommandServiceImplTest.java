package com.ddd.application.service.otp.impl;

import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.exception.DuplicateResourceException;
import com.ddd.domain.model.Otp;
import com.ddd.domain.repository.OtpRepository;
import com.ddd.domain.repository.UserRepository;
import com.ddd.infrastructure.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpCommandServiceImplTest {

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OtpCommandServiceImpl otpCommandService;

    @Test
    void sendOtpRejectsAlreadyRegisteredEmailBeforeSendingMail() {
        when(userRepository.existsByEmail("existing@student.ctu.edu.vn")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> otpCommandService.sendOtp("Existing@student.ctu.edu.vn"));

        assertEquals("AUTH.003", ex.getErrorCode());
        verifyNoInteractions(otpRepository);
        verifyNoInteractions(emailService);
    }

    @Test
    void sendOtpRejectsRequestsWithinCooldownWindow() {
        when(userRepository.existsByEmail("user@student.ctu.edu.vn")).thenReturn(false);
        Otp recent = new Otp("user@student.ctu.edu.vn", "123456", Instant.now().plusSeconds(300), false);
        recent.setCreatedAt(Instant.now().minusSeconds(10));
        when(otpRepository.findLatestOtp("user@student.ctu.edu.vn")).thenReturn(Optional.of(recent));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> otpCommandService.sendOtp("user@student.ctu.edu.vn"));

        assertEquals("AUTH.016", ex.getErrorCode());
        verifyNoInteractions(emailService);
        verify(otpRepository, never()).save(any());
    }

    @Test
    void verifyOtpInvalidatesOtpAfterFifthFailedAttempt() {
        Otp otp = new Otp("user@student.ctu.edu.vn", "654321", Instant.now().plusSeconds(300), false);
        otp.setId(42L);
        when(otpRepository.findLatestValidOtp("user@student.ctu.edu.vn")).thenReturn(Optional.of(otp));

        for (int attempt = 1; attempt <= 4; attempt++) {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> otpCommandService.verifyOtp("user@student.ctu.edu.vn", "000000"));
            assertTrue(ex.getMessage().contains("Remaining attempts: " + (5 - attempt)));
            verify(otpRepository, never()).markOtpUsed(any());
        }

        BusinessException fifth = assertThrows(BusinessException.class,
                () -> otpCommandService.verifyOtp("user@student.ctu.edu.vn", "000000"));
        assertTrue(fifth.getMessage().contains("Too many invalid OTP attempts"));
        verify(otpRepository).markOtpUsed(42L);
    }

    @Test
    void sendOtpGeneratesFreshOtpWithResetAttemptLimitAfterCooldown() {
        when(userRepository.existsByEmail("user@student.ctu.edu.vn")).thenReturn(false);
        Otp expiredCooldown = new Otp("user@student.ctu.edu.vn", "111111", Instant.now().plusSeconds(300), true);
        expiredCooldown.setCreatedAt(Instant.now().minusSeconds(45));
        when(otpRepository.findLatestOtp("user@student.ctu.edu.vn")).thenReturn(Optional.of(expiredCooldown));

        Otp savedNew = new Otp("user@student.ctu.edu.vn", "222222", Instant.now().plusSeconds(300), false);
        savedNew.setId(99L);
        when(otpRepository.save(any(Otp.class))).thenReturn(savedNew);

        otpCommandService.sendOtp("user@student.ctu.edu.vn");

        verify(emailService).sendOtpEmail(eq("user@student.ctu.edu.vn"), anyString());
    }
}
