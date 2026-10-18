package com.ddd.infrastructure.service;

public interface EmailService {

    void sendOtpEmail(String toEmail, String otpCode);

}
