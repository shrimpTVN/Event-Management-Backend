package com.ddd.application.service.otp;

public interface OtpCommandService {

    void sendOtp(String email);

    void verifyOtp(String email, String otpCode);

}
