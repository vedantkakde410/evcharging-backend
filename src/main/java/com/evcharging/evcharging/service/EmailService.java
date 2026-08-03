package com.evcharging.evcharging.service;

import com.evcharging.evcharging.exception.EmailSendException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Real SMTP delivery via JavaMailSender - see application.properties for the
// MailHog-by-default dev configuration and AUTHENTICATION_DESIGN.md section
// 6 for why this must fail loudly rather than pretend to succeed.
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender, @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendOtpEmail(String to, String otpCode, OtpPurpose purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(to);
            helper.setFrom(fromAddress);
            helper.setSubject(purpose.subject());
            helper.setText(buildHtml(otpCode, purpose), true);

            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send OTP email to {}: {}", to, e.getMessage(), e);
            throw new EmailSendException(e);
        }
    }

    private String buildHtml(String otpCode, OtpPurpose purpose) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto;">
                  <h2 style="color: #1a1a1a;">%s</h2>
                  <p style="color: #444; font-size: 15px;">
                    Use the code below to continue. It expires in 5 minutes.
                  </p>
                  <div style="font-size: 32px; font-weight: bold; letter-spacing: 8px;
                              background: #f4f4f5; padding: 16px 24px; border-radius: 8px;
                              text-align: center; margin: 24px 0;">
                    %s
                  </div>
                  <p style="color: #888; font-size: 13px;">
                    If you didn't request this, you can safely ignore this email.
                  </p>
                </div>
                """.formatted(purpose.heading(), otpCode);
    }
}
