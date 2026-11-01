package com.ddd.infrastructure.service.impl;

import com.ddd.infrastructure.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Override
    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail);
            }
            helper.setTo(toEmail);
            helper.setSubject("Your OTP passcode");

            String htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                        <h2 style="color: #1a73e8; text-align: center;">Your OTP passcode</h2>
                        <p>Hi,</p>
                        <p>You requested an OTP passcode to verify your account. Your OTP passcode is:</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <span style="display: inline-block; font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #1a73e8; background-color: #f1f3f4; padding: 12px 24px; border-radius: 8px;">
                                %s
                            </span>
                        </div>
                        <p style="color: #d93025; font-size: 14px;"><strong>Lưu ý:</strong> Mã này có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>
                        <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;" />
                        <p style="color: #70757a; font-size: 12px; text-align: center;">Đây là email tự động, vui lòng không trả lời thư này.</p>
                    </div>
                    """.formatted(otpCode);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Sent OTP email successfully to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Can't send OTP email, please try again later.", e);  
        }
    }
}
