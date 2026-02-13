package com.financal.mgt.Financal.Management.service.auth;


import com.financal.mgt.Financal.Management.dto.request.auth.ApiResponse;
import com.financal.mgt.Financal.Management.dto.response.auth.*;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    ApiResponse<?> signup(SignupRequest request);

    ApiResponse<?> refreshToken(HttpServletRequest request);

    ApiResponse<?> verifyOtp(OtpVerificationRequest request, HttpServletRequest httpServletRequest);
    ApiResponse<?> login(LoginRequest request);
    ApiResponse<?> setupBusiness(BusinessSetupRequest request, HttpServletRequest httpServletRequest);
    ApiResponse<?> resendOtp(ResendOtpRequest request, HttpServletRequest httpServletRequest);
    ApiResponse<UserResponseDTO> getUserByEmail(HttpServletRequest httpServletRequest);

}
