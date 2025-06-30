package com.financal.mgt.Financal.Management.service.impl;

import com.financal.mgt.Financal.Management.config.SpringMongoConfig;
import com.financal.mgt.Financal.Management.dto.response.FinalResponse;
import com.financal.mgt.Financal.Management.model.OtpRequest;
import com.financal.mgt.Financal.Management.repository.OtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class EmailOtpService {

    private static final Logger log = LoggerFactory.getLogger(EmailOtpService.class);

    @Value("${otp.expiry.minutes:5}")
    private long otpExpiryMinutes;

    private final JavaMailSender mailSender;
    private final OtpRepository otpRepository;

    public EmailOtpService(JavaMailSender mailSender, OtpRepository otpRepository) {
        this.mailSender = mailSender;
        this.otpRepository = otpRepository;
    }



    public void sendOtp(String email) {
        String otp = generateOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setExpiry(expiryTime);
        otpRequest.setOtp(otp);
        otpRequest.setUserId(email);

        otpRepository.save(otpRequest);

        sendEmail(email, otp);
        log.info("OTP sent to {}", email);
    }

    public Object verifyOtp(String email, String otp) {

        FinalResponse response = new FinalResponse();
        try {

            boolean valid = otpRepository.findById(email)
                    .filter(record -> record.getOtp().equals(otp))
                    .filter(record -> record.getExpiry().isAfter(LocalDateTime.now()))
                    .map(record -> {
                        otpRepository.deleteById(email);
                        return true;
                    }).orElse(false);

            if (valid) {
                response.setMessage("OTP verified successfully.");
                response.setStatusCode(200);
                return ResponseEntity.ok(response);
            } else {
                response.setMessage("Invalid or expired OTP.");
                response.setStatusCode(400);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            log.info("THE ERROR::{}", e.getMessage());
            response.setMessage("Invalid or expired OTP.");
            response.setStatusCode(400);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

    }

    private String generateOtp() {
        int otp = 100_000 + new SecureRandom().nextInt(900_000); // 6-digit OTP
        return String.valueOf(otp);
    }

    private void sendEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your OTP Code");
        message.setText("Your OTP is: " + otp + ". It expires in " + otpExpiryMinutes + " minutes.");
        mailSender.send(message);
    }
}

