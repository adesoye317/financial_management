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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);
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
            response.setToken(generateJWTToken(customer, request.getDeviceToken()));
            response.setData(customer);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error occurred during signup", e);
            response.setMessage("Unable to sign you up at the moment. Try again");
            response.setStatusCode(400);
            return ResponseEntity.badRequest().body(response);
        }
    }

    private String generateJWTToken(Customer user, String deviceToken) {
        try {
            String tokenValidityHours = env.getProperty("TOKEN_VALIDITY_HOURS");
            String privateKeyString = env.getProperty("API_SECRET_KEY");

            if (privateKeyString == null || tokenValidityHours == null) {
                throw new IllegalArgumentException("Missing TOKEN_VALIDITY_HOURS or API_SECRET_KEY");
            }

            long timestamp = System.currentTimeMillis();
            long tokenValidityInMillis = Long.parseLong(tokenValidityHours) * 60L * 60L * 1000L;

            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyString);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);

            return Jwts.builder()
                    .claim("userId", user.getUserId())
                    .claim("email", user.getEmail())
                    .claim("fcmToken", deviceToken)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + tokenValidityInMillis))
                    .signWith(privateKey, SignatureAlgorithm.RS256) // << NOTE: new style
                    .compact();


        } catch (Exception e) {
            log.error("JWT generation failed", e);
            throw new IllegalStateException("JWT token creation failed", e);
        }
    }
}
