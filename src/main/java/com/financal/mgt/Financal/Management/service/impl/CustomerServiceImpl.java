package com.financal.mgt.Financal.Management.service.impl;



import com.financal.mgt.Financal.Management.dto.request.CustomerSignUpRequest;
import com.financal.mgt.Financal.Management.dto.response.FinalResponse;
import com.financal.mgt.Financal.Management.model.Customer;
import com.financal.mgt.Financal.Management.repository.CustomerRepository;
import com.financal.mgt.Financal.Management.service.CustomerService;
import com.financal.mgt.Financal.Management.util.Hash;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final Environment env;

    public CustomerServiceImpl(CustomerRepository customerRepository, Environment env) {
        this.customerRepository = customerRepository;
        this.env = env;
    }

    @Override
    public Object signUp(CustomerSignUpRequest request) {
        FinalResponse response = new FinalResponse();
        try {
            if (customerRepository.existsByEmail(request.getEmail())) {

                response.setMessage("Email already registered");
                response.setStatusCode(400);
                return ResponseEntity.badRequest().body(response);
            }

            if (customerRepository.existsByPhoneNumber(request.getPhoneNumber())) {

                response.setMessage("Phone number already registered");
                response.setStatusCode(400);
                return ResponseEntity.badRequest().body(response);
            }

            Customer customer = new Customer();
            customer.setEmail(request.getEmail());
            customer.setPhoneNumber(request.getPhoneNumber());
            customer.setPasswordHash(Hash.hash(request.getPassword()));
            customer.setUserId(UUID.randomUUID().toString());

            customerRepository.save(customer);

            response.setMessage("Customer registered successfully");
            response.setStatusCode(200);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            response.setMessage("Please I am unable to sign you up at the moment. Try again");
            response.setStatusCode(400);
            return ResponseEntity.badRequest().body(response);
        }



    }


    private String generateJWTToken(Customer user, String deviceToken) {

        log.info("THE USER DETAILS IN JWT::{}", user);
        long timestamp = System.currentTimeMillis();

        String tokenValidityHours = env.getProperty("TOKEN_VALIDITY_HOURS");

        String privateKeyString = env.getProperty("API_SECRET_KEY");
        String fcmToken = "";
        if (deviceToken != null) {
            fcmToken = deviceToken;
        }

        if (privateKeyString == null || tokenValidityHours == null) {
            // Handle the case where properties are not found or are invalid
            throw new IllegalArgumentException("Private key or TOKEN_VALIDITY_HOURS not found or invalid");
        }

        try {
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyString);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            long tokenValidityInHours = Long.parseLong(tokenValidityHours);
            long tokenValidityInMillis = tokenValidityInHours * 60L * 1000L;

            return Jwts.builder()
                    .signWith(SignatureAlgorithm.RS256, privateKey)
                    .setIssuedAt(new Date(timestamp))
                    .setExpiration(new Date(timestamp + tokenValidityInMillis))
                    .claim("userId", user.getUserId())
                    .claim("email", user.getEmail())
                    .claim("fcmToken", fcmToken)
                    .compact();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to generate JWT token", e);
        }
    }
}
