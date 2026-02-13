package com.financal.mgt.Financal.Management.service.auth;

import com.financal.mgt.Financal.Management.dto.request.auth.ApiResponse;
import com.financal.mgt.Financal.Management.dto.response.auth.*;
import com.financal.mgt.Financal.Management.model.auth.TokenResponse;
import com.financal.mgt.Financal.Management.model.auth.User;
import com.financal.mgt.Financal.Management.repository.auth.UserRepository;
import com.financal.mgt.Financal.Management.security.JwtUtil;
import com.financal.mgt.Financal.Management.service.audit.AuditService;
import com.financal.mgt.Financal.Management.util.EmailService;
import com.financal.mgt.Financal.Management.util.Hash;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final AuditService auditService;

    // SecureRandom instead of Random for OTP generation
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 30;

    @Override
    @Transactional
    public ApiResponse<?> signup(SignupRequest request) {
        try {
            String email = normalizeEmail(request.getEmail());

            if (userRepository.existsByEmail(email)) {
                // Don't reveal if email exists — prevents enumeration
                auditService.log(null, "SIGNUP_DUPLICATE",
                        "Signup attempted with existing email", null);
                return new ApiResponse<>(400, "Unable to complete signup. Please try again.", null);
            }

            String otp = generateOtp();

            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .verified(false)
                    .otp(Hash.hash(otp))
                    .otpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                    .profileSetupStatus("USER_REGISTERED")
                    .loginAttempts(0)
                    .build();

            userRepository.save(user);

            // Send OTP asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    emailService.sendOtpEmail(email, otp);
                } catch (Exception e) {
                    log.error("Failed to send OTP email to {}: {}", email, e.getMessage());
                }
            });

            String token = jwtUtil.generateToken(user.getEmail(), user.getUserId());

            auditService.log(user.getUserId(), "SIGNUP",
                    "User registered successfully", null);

            log.info("User signed up: {}", email);

            return new ApiResponse<>(200, "Signup successful. Verify email with OTP.",
                    new TokenResponse(null, token));

        } catch (Exception e) {
            log.error("Signup failed: {}", e.getMessage(), e);
            return new ApiResponse<>(400,
                    "Unable to sign up at the moment. Please try again later.", null);
        }
    }

    @Override
    public ApiResponse<?> login(LoginRequest request) {
        try {
            String email = normalizeEmail(request.getEmail());

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                auditService.log(null, "LOGIN_FAILED",
                        "Login attempt with non-existent email", getClientIp(null));
                // Same message to prevent email enumeration
                return new ApiResponse<>(401, "Invalid credentials", null);
            }

            User user = userOpt.get();

            // Check account lockout
            if (isAccountLocked(user)) {
                auditService.log(user.getUserId(), "LOGIN_LOCKED",
                        "Login attempt on locked account", null);
                return new ApiResponse<>(423,
                        "Account is temporarily locked. Try again after "
                                + LOCKOUT_MINUTES + " minutes.", null);
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                handleFailedLogin(user);
                auditService.log(user.getUserId(), "LOGIN_FAILED",
                        "Invalid password. Attempts: " + user.getLoginAttempts(), null);
                return new ApiResponse<>(401, "Invalid credentials", null);
            }

            if (!user.isVerified()) {
                auditService.log(user.getUserId(), "LOGIN_UNVERIFIED",
                        "Login attempt with unverified email", null);
                return new ApiResponse<>(403, "Email not verified", null);
            }

            // Reset login attempts on success
            user.setLoginAttempts(0);
            user.setLockedUntil(null);
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            String token = jwtUtil.generateToken(user.getEmail(), user.getUserId());

            auditService.log(user.getUserId(), "LOGIN_SUCCESS",
                    "User logged in successfully", null);

            log.info("User logged in: {}", email);

            return new ApiResponse<>(200, "Login successful",
                    new TokenResponse(user, token));

        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage(), e);
            return new ApiResponse<>(400,
                    "Unable to login at the moment. Please try again later.", null);
        }
    }

    @Override
    public ApiResponse<?> verifyOtp(OtpVerificationRequest request, HttpServletRequest httpServletRequest) {
        try {
            String email = getEmail(httpServletRequest);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check OTP expiry
            if (user.getOtpExpiry() == null || LocalDateTime.now().isAfter(user.getOtpExpiry())) {
                auditService.log(user.getUserId(), "OTP_EXPIRED",
                        "OTP verification attempted with expired OTP", null);
                return new ApiResponse<>(400, "OTP has expired. Please request a new one.", null);
            }

            String hashedOTP = Hash.hash(request.getOtp());

            if (!user.getOtp().equals(hashedOTP)) {
                auditService.log(user.getUserId(), "OTP_INVALID",
                        "Invalid OTP entered", null);
                return new ApiResponse<>(400, "Invalid OTP", null);
            }

            user.setVerified(true);
            user.setProfileSetupStatus("USER_EMAIL_VERIFIED");
            user.setOtp(null);
            user.setOtpExpiry(null);
            userRepository.save(user);

            String token = jwtUtil.generateToken(email, user.getUserId());

            auditService.log(user.getUserId(), "EMAIL_VERIFIED",
                    "Email verified successfully", null);

            log.info("Email verified for user: {}", email);

            return new ApiResponse<>(200, "Email verified successfully",
                    new TokenResponse(null, token));

        } catch (Exception e) {
            log.error("Verify OTP failed: {}", e.getMessage(), e);
            return new ApiResponse<>(400,
                    "Unable to verify OTP at the moment. Please try again later.", null);
        }
    }

    @Override
    public ApiResponse<?> resendOtp(ResendOtpRequest request, HttpServletRequest httpServletRequest) {
        try {

            String email = getEmail(httpServletRequest);
             email = normalizeEmail(email);

            Optional<User> existingUserOpt = userRepository.findByEmail(email);

            if (existingUserOpt.isEmpty()) {
                // Don't reveal if email exists
                auditService.log(null, "RESEND_OTP_NOT_FOUND",
                        "OTP resend attempted for non-existent email", null);
                return new ApiResponse<>(200,
                        "If the email exists, an OTP has been sent.", null);
            }

            User user = existingUserOpt.get();

            // Rate limit: prevent OTP spam (minimum 1 minute between resends)
            if (user.getOtpExpiry() != null) {
                long minutesSinceLastOtp = Duration.between(
                        user.getOtpExpiry().minusMinutes(OTP_EXPIRY_MINUTES),
                        LocalDateTime.now()).toMinutes();
                if (minutesSinceLastOtp < 1) {
                    return new ApiResponse<>(429,
                            "Please wait before requesting another OTP.", null);
                }
            }

            String otp = generateOtp();

            user.setOtp(Hash.hash(otp));
            user.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
            userRepository.save(user);

            String finalEmail = email;
            CompletableFuture.runAsync(() -> {
                try {
                    emailService.sendOtpEmail(finalEmail, otp);
                } catch (Exception e) {
                    log.error("Failed to send OTP email: {}", e.getMessage());
                }
            });

            auditService.log(user.getUserId(), "OTP_RESENT",
                    "OTP resent to " + email, null);

            return new ApiResponse<>(200,
                    "If the email exists, an OTP has been sent.", null);

        } catch (Exception e) {
            log.error("Error in resendOtp: {}", e.getMessage(), e);
            return new ApiResponse<>(400,
                    "Unable to process request. Please try again later.", null);
        }
    }

    @Override
    public ApiResponse<?> refreshToken(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");

            if (token == null || !token.startsWith("Bearer ")) {
                return new ApiResponse<>(401, "Invalid authorization header", null);
            }

            String email = jwtUtil.extractEmailFromHeader(token);

            Optional<User> existing = userRepository.findByEmail(email);
            if (existing.isEmpty()) {
                return new ApiResponse<>(401, "Invalid token", null);
            }

            String jwtToken = jwtUtil.generateRefreshToken(email);

            auditService.log(existing.get().getUserId(), "TOKEN_REFRESHED",
                    "Token refreshed", null);

            return new ApiResponse<>(200, "Refresh Token.", new TokenResponse(null, jwtToken));

        } catch (Exception e) {
            log.error("Refresh token failed: {}", e.getMessage(), e);
            return new ApiResponse<>(400,
                    "Unable to generate refresh token. Please try again later.", null);
        }
    }

    @Override
    public ApiResponse<?> setupBusiness(BusinessSetupRequest request, HttpServletRequest httpServletRequest) {
        try {
            String email = getEmail(httpServletRequest);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.isVerified()) {
                return new ApiResponse<>(403, "Email not verified", null);
            }

            user.setCurrency(request.getCurrency());
            user.setGoals(request.getGoals());
            user.setAddress(request.getAddress());
            user.setAccountType(request.getAccountType());
            user.setLanguage(request.getLanguage());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setProfileSetupStatus("USER_BUSINESS_SETUP");
            userRepository.save(user);

            auditService.log(user.getUserId(), "BUSINESS_SETUP",
                    "Business profile setup completed", null);

            log.info("Business setup completed for user: {}", email);

            return new ApiResponse<>(200, "Business setup completed!", user);

        } catch (Exception e) {
            log.error("Business setup failed: {}", e.getMessage(), e);
            return new ApiResponse<>(400,
                    "Unable to set up business at the moment. Please try again later.", null);
        }
    }

    @Override
    public ApiResponse<UserResponseDTO> getUserByEmail(HttpServletRequest httpServletRequest) {
        try {
            String email = getEmail(httpServletRequest);

            if (email == null || email.trim().isEmpty()) {
                return new ApiResponse<>(400, "Email is required", null);
            }

            email = normalizeEmail(email);

            Optional<User> userOptional = userRepository.findByEmail(email);

            if (userOptional.isEmpty()) {
                return new ApiResponse<>(404, "User not found", null);
            }

            User user = userOptional.get();
            UserResponseDTO userDTO = convertToDTO(user);

            return new ApiResponse<>(200, "User fetched successfully", userDTO);

        } catch (Exception e) {
            log.error("Failed to fetch user by email: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Failed to retrieve user", null);
        }
    }

    public ApiResponse<Boolean> checkUserExists(String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return new ApiResponse<>(400, "Email is required", false);
            }

            boolean exists = userRepository.existsByEmail(normalizeEmail(email));

            return new ApiResponse<>(200,
                    exists ? "User exists" : "User does not exist", exists);

        } catch (Exception e) {
            log.error("Failed to check user existence: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Failed to check user existence", false);
        }
    }

    // ==================== HELPERS ====================

    public String getEmail(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid authorization header");
        }
        String token = authHeader.substring(7);
        return jwtUtil.extractEmail(token);
    }

    private boolean isAccountLocked(User user) {
        if (user.getLockedUntil() == null) return false;
        if (LocalDateTime.now().isAfter(user.getLockedUntil())) {
            // Lockout expired, reset
            user.setLockedUntil(null);
            user.setLoginAttempts(0);
            userRepository.save(user);
            return false;
        }
        return true;
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getLoginAttempts() + 1;
        user.setLoginAttempts(attempts);

        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
            log.warn("Account locked for user: {} after {} failed attempts",
                    user.getEmail(), attempts);
        }

        userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }

    private UserResponseDTO convertToDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getUserId())
                .email(user.getEmail())
                .verified(user.isVerified())
                .profileSetupStatus(user.getProfileSetupStatus())
                .language(user.getLanguage())
                .currency(user.getCurrency())
                .firstName(user.getFirstName())
                .LastName(user.getLastName())
                .phoneNumber(Optional.ofNullable(user.getPhoneNumber()).orElse(""))
                .goals(user.getGoals())
                .accountType(user.getAccountType())
                .address(user.getAddress())
                .build();
    }

    private String generateOtp() {
        return String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
    }
}