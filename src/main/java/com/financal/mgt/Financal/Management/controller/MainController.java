package com.financal.mgt.Financal.Management.controller;

import com.financal.mgt.Financal.Management.dto.request.CustomerLoginRequest;
import com.financal.mgt.Financal.Management.dto.request.CustomerSignUpRequest;
import com.financal.mgt.Financal.Management.dto.request.VerifyEmailRequest;
import com.financal.mgt.Financal.Management.service.CustomerService;
import com.financal.mgt.Financal.Management.service.impl.EmailOtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class MainController {


    private final CustomerService customerService;
    private final EmailOtpService emailOtpService;


    public MainController(CustomerService customerService, EmailOtpService emailOtpService) {
        this.customerService = customerService;
        this.emailOtpService = emailOtpService;
    }


    @PostMapping("/signup")
    public Object signUp(@Valid @RequestBody CustomerSignUpRequest request) {
        return customerService.signUp(request);
    }

    @PostMapping("/login")
    public Object login(@Valid @RequestBody CustomerLoginRequest request) {
        return customerService.login(request);
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendOtp(HttpServletRequest httpServletRequest) {

        String email = (String) httpServletRequest.getAttribute("email");
        System.out.println(email);
        emailOtpService.sendOtp("anuoluwagoke@gmail.com");
        return ResponseEntity.ok("OTP sent to email.");
    }

    @PostMapping("/verify")
    public Object verifyOtp(@RequestBody VerifyEmailRequest otp, HttpServletRequest httpServletRequest) {
        String email = (String) httpServletRequest.getAttribute("email");
        return emailOtpService.verifyOtp("anuoluwagoke@gmail.com", otp.getOtp());

    }

}
