package com.financal.mgt.Financal.Management.service;

import com.financal.mgt.Financal.Management.dto.request.CustomerLoginRequest;
import com.financal.mgt.Financal.Management.dto.request.CustomerSignUpRequest;

public interface CustomerService {

    Object signUp(CustomerSignUpRequest request);

    Object login(CustomerLoginRequest request);
}
