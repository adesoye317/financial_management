package com.financal.mgt.Financal.Management.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Sends an OTP email using an HTML template (memory-safe).
     *
     * @param email recipient email address
     * @param otp   one-time password
     */
    public void sendOtpEmail(String email, String otp) {
        try {
            // Load HTML template from classpath safely
            String htmlContent;
            ClassPathResource resource = new ClassPathResource("templates/EmailOTP.html");

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                htmlContent = reader.lines().collect(Collectors.joining("\n"));
            }

            // Replace placeholders in the template
            htmlContent = htmlContent
                    .replace("{{OTP}}", otp)
                    .replace("{{username}}", email)
                    .replace("{{email}}", email);

            // Create a MIME email (for HTML)
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Verify your email for Xpenskey");
            helper.setText(htmlContent, true);

            // Send email
            mailSender.send(message);
            log.info("✅ OTP email sent successfully to {}", email);

        } catch (IOException e) {
            log.error("❌ Error reading email template: {}", e.getMessage(), e);
            throw new RuntimeException("Error reading email template", e);
        } catch (MessagingException e) {
            log.error("❌ Failed to send OTP email to {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Email sending failed", e);
        }
    }
}
