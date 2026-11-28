package com.ddd.domain.repository;

import com.ddd.domain.model.Otp;

import java.util.Optional;

public interface OtpRepository {

    Otp save(Otp otp);

    Optional<Otp> findLatestValidOtp(String email);

    Optional<Otp> findLatestOtp(String email);

    void markOtpUsed(Long otpId);

}
