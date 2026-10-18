package com.ddd.application.service.otp.impl;

import com.ddd.application.service.otp.OtpCommandService;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.model.Otp;
import com.ddd.domain.repository.OtpRepository;
import com.ddd.infrastructure.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpCommandServiceImpl implements OtpCommandService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final int OTP_EXPIRATION_MINUTES = 5;

    @Override
    @Transactional
    public void sendOtp(String email) {
        log.info("Generating and sending OTP for email: {}", email);

        String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        Instant expiredAt = Instant.now().plus(Duration.ofMinutes(OTP_EXPIRATION_MINUTES));

        Otp otp = new Otp();
        otp.setEmail(email);
        otp.setOtpCode(otpCode);
        otp.setExpiredAt(expiredAt);
        otp.setIsUsed(false);

        otpRepository.save(otp);
        emailService.sendOtpEmail(email, otpCode);

        log.info("OTP sent successfully to email: {}", email);
    }

    @Override
    @Transactional
    public void verifyOtp(String email, String otpCode) {
        log.info("Verifying OTP for email: {}", email);

        Otp otp = otpRepository.findLatestValidOtp(email)
                .orElseThrow(() -> new BusinessException("OTP not found or already used."));

        if (otp.getExpiredAt().isBefore(Instant.now())) {
            throw new BusinessException("OTP has expired. Please resend a new OTP.");
        }

        if (!otp.getOtpCode().equals(otpCode)) {
            throw new BusinessException("Invalid OTP code.");
        }

        otp.setIsUsed(true);
        otpRepository.save(otp);

        log.info("OTP verified successfully for email: {}", email);
    }

}
