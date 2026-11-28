package com.ddd.application.service.otp.impl;

import com.ddd.application.service.otp.OtpCommandService;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.exception.DuplicateResourceException;
import com.ddd.domain.model.Otp;
import com.ddd.domain.repository.OtpRepository;
import com.ddd.domain.repository.UserRepository;
import com.ddd.infrastructure.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpCommandServiceImpl implements OtpCommandService {

    private final OtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();

    private static final int OTP_EXPIRATION_MINUTES = 5;
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final Duration SEND_COOLDOWN = Duration.ofSeconds(30);

    private static String attemptKey(Otp otp, String email) {
        if (otp.getId() != null) {
            return "id:" + otp.getId();
        }
        return "email:" + (email != null ? email.trim().toLowerCase() : "");
    }

    @Override
    @Transactional
    public void sendOtp(String email) {
        log.info("Generating and sending OTP for email: {}", email);

        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        if (Boolean.TRUE.equals(userRepository.existsByEmail(normalizedEmail))
                || (!normalizedEmail.equals(email) && Boolean.TRUE.equals(userRepository.existsByEmail(email)))) {
            throw DuplicateResourceException.withCode(
                    "AUTH.003",
                    "User with email " + normalizedEmail + " already exists."
            );
        }

        Instant now = Instant.now();
        otpRepository.findLatestOtp(normalizedEmail).ifPresent(latest -> {
            if (latest.getCreatedAt() != null
                    && Duration.between(latest.getCreatedAt(), now).compareTo(SEND_COOLDOWN) < 0) {
                throw BusinessException.withCode("AUTH.016", "Please wait 30 seconds before requesting a new OTP.");
            }
        });

        String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        Instant expiredAt = now.plus(Duration.ofMinutes(OTP_EXPIRATION_MINUTES));

        Otp otp = new Otp(normalizedEmail, otpCode, expiredAt, false);
        Otp saved = otpRepository.save(otp);
        failedAttempts.remove("email:" + normalizedEmail);
        if (saved != null && saved.getId() != null) {
            failedAttempts.remove("id:" + saved.getId());
        }
        emailService.sendOtpEmail(normalizedEmail, otpCode);

        log.info("OTP sent successfully to email: {}", normalizedEmail);
    }

    @Override
    @Transactional
    public void verifyOtp(String email, String otpCode) {
        log.info("Verifying OTP for email: {}", email);

        Otp otp = otpRepository.findLatestValidOtp(email)
                .orElseThrow(() -> BusinessException.withCode("AUTH.016", "OTP not found or already used."));

        if (Boolean.TRUE.equals(otp.getIsUsed())) {
            throw BusinessException.withCode("AUTH.016", "OTP has already been used.");
        }

        if (otp.getExpiredAt().isBefore(Instant.now())) {
            throw BusinessException.withCode("AUTH.016", "OTP has expired. Please resend a new OTP.");
        }

        String key = attemptKey(otp, email);
        if (!otp.getOtpCode().equals(otpCode)) {
            if (failedAttempts.size() > 10_000) {
                failedAttempts.clear();
            }
            int attempts = failedAttempts.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
            if (attempts >= MAX_VERIFY_ATTEMPTS) {
                otpRepository.markOtpUsed(otp.getId());
                failedAttempts.remove(key);
                throw BusinessException.withCode("AUTH.016", "Too many invalid OTP attempts. Please request a new OTP.");
            }
            int remaining = MAX_VERIFY_ATTEMPTS - attempts;
            throw BusinessException.withCode("AUTH.016", "Invalid OTP code. Remaining attempts: " + remaining + ".");
        }

        failedAttempts.remove(key);
        otp.setIsUsed(true);
        otpRepository.save(otp);

        log.info("OTP verified successfully for email: {}", email);
    }

}
