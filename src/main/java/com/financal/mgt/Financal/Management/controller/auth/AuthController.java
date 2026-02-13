package com.financal.mgt.Financal.Management.controller.auth;



import com.financal.mgt.Financal.Management.dto.request.auth.ApiResponse;
import com.financal.mgt.Financal.Management.dto.response.auth.*;
import com.financal.mgt.Financal.Management.service.auth.AuthServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServiceImpl authService;

    @PostMapping("/signup")
    public ApiResponse<?> signup(@RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/verify-otp")
    public ApiResponse<?> verifyOtp(@RequestBody OtpVerificationRequest request, HttpServletRequest httpServletRequest) {
        return authService.verifyOtp(request, httpServletRequest);
    }

    @PostMapping("/resend-otp")
    public ApiResponse<?> resendOtp(@RequestBody ResendOtpRequest request,  HttpServletRequest httpServletRequest) {
        return authService.resendOtp(request, httpServletRequest);
    }

    @PostMapping("/business-setup")
    public ApiResponse<?> businessSetup(@RequestBody BusinessSetupRequest request, HttpServletRequest httpServletRequest) {
        return authService.setupBusiness(request, httpServletRequest);
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }


    @PostMapping("/refresh-token")
    public ApiResponse<?> refreshToken(HttpServletRequest httpServletRequest) {
        return authService.refreshToken(httpServletRequest);
    }


    /**
     * Get user by email
     */
    @GetMapping("/get-user")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserByEmail(HttpServletRequest httpServletRequest) {
        ApiResponse<UserResponseDTO> response = authService.getUserByEmail(httpServletRequest);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }




}

