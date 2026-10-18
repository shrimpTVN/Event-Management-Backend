package com.ddd.application.service.otp.impl;

import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.model.Otp;
import com.ddd.domain.repository.OtpRepository;
import com.ddd.infrastructure.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpCommandServiceImplTest {

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OtpCommandServiceImpl otpCommandService;

    private final String testEmail = "test@example.com";

    @Test
    @DisplayName("sendOtp: should generate 6-digit OTP, save to repository and send email")
    void sendOtp_shouldSaveOtpAndSendEmail() {
        // Arrange
        when(otpRepository.save(any(Otp.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        otpCommandService.sendOtp(testEmail);

        // Assert
        ArgumentCaptor<Otp> otpCaptor = ArgumentCaptor.forClass(Otp.class);
        verify(otpRepository).save(otpCaptor.capture());
        Otp savedOtp = otpCaptor.getValue();

        assertNotNull(savedOtp);
        assertEquals(testEmail, savedOtp.getEmail());
        assertNotNull(savedOtp.getOtpCode());
        assertEquals(6, savedOtp.getOtpCode().length());
        assertFalse(savedOtp.getIsUsed());
        assertTrue(savedOtp.getExpiredAt().isAfter(Instant.now()));

        verify(emailService).sendOtpEmail(eq(testEmail), eq(savedOtp.getOtpCode()));
    }

    @Test
    @DisplayName("verifyOtp: should succeed and mark OTP as used when valid")
    void verifyOtp_shouldSucceedWhenValid() {
        // Arrange
        String code = "123456";
        Otp otp = new Otp();
        otp.setEmail(testEmail);
        otp.setOtpCode(code);
        otp.setExpiredAt(Instant.now().plus(Duration.ofMinutes(5)));
        otp.setIsUsed(false);

        when(otpRepository.findLatestValidOtp(testEmail)).thenReturn(Optional.of(otp));

        // Act
        otpCommandService.verifyOtp(testEmail, code);

        // Assert
        assertTrue(otp.getIsUsed());
        verify(otpRepository).save(otp);
    }

    @Test
    @DisplayName("verifyOtp: should throw BusinessException when OTP not found")
    void verifyOtp_shouldThrowWhenNotFound() {
        // Arrange
        when(otpRepository.findLatestValidOtp(testEmail)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                otpCommandService.verifyOtp(testEmail, "123456")
        );
        assertTrue(ex.getMessage().contains("không tồn tại"));
        verify(otpRepository, never()).save(any());
    }

    @Test
    @DisplayName("verifyOtp: should throw BusinessException when OTP is expired")
    void verifyOtp_shouldThrowWhenExpired() {
        // Arrange
        Otp otp = new Otp();
        otp.setEmail(testEmail);
        otp.setOtpCode("123456");
        otp.setExpiredAt(Instant.now().minus(Duration.ofMinutes(1)));
        otp.setIsUsed(false);

        when(otpRepository.findLatestValidOtp(testEmail)).thenReturn(Optional.of(otp));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                otpCommandService.verifyOtp(testEmail, "123456")
        );
        assertTrue(ex.getMessage().contains("hết hạn"));
        verify(otpRepository, never()).save(any());
    }

    @Test
    @DisplayName("verifyOtp: should throw BusinessException when OTP code is incorrect")
    void verifyOtp_shouldThrowWhenCodeMismatch() {
        // Arrange
        Otp otp = new Otp();
        otp.setEmail(testEmail);
        otp.setOtpCode("123456");
        otp.setExpiredAt(Instant.now().plus(Duration.ofMinutes(5)));
        otp.setIsUsed(false);

        when(otpRepository.findLatestValidOtp(testEmail)).thenReturn(Optional.of(otp));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                otpCommandService.verifyOtp(testEmail, "654321")
        );
        assertTrue(ex.getMessage().contains("không chính xác"));
        verify(otpRepository, never()).save(any());
    }

}
