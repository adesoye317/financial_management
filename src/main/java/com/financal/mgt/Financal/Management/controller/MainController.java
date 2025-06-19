package com.financal.mgt.Financal.Management.controller;

import com.financal.mgt.Financal.Management.dto.request.CustomerSignUpRequest;
import com.financal.mgt.Financal.Management.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class MainController {


    private final CustomerService customerService;


    public MainController(CustomerService customerService) {
        this.customerService = customerService;
    }


    @PostMapping("/signup")
    public Object signUp(@Valid @RequestBody CustomerSignUpRequest request) {
        return customerService.signUp(request);
    }

}
