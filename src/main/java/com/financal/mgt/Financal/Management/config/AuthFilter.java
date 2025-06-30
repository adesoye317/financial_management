package com.financal.mgt.Financal.Management.config;

import com.financal.mgt.Financal.Management.service.impl.CustomerServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

@Component
public class AuthFilter extends GenericFilterBean {

    @Autowired
    private Environment env;
    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);


    private boolean isExcludedPath(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String requestMethod = request.getMethod();
        System.out.println("Request URI: " + requestURI + ", Method: " + requestMethod);

        String[] excludedPaths = {"/xpenseny/api/customers/signup", "xpenseny/actuator", "/xpenseny/api/customers/send", "/xpenseny/api/customers/verify"}; // Add more paths as needed;

        for (String path : excludedPaths) {
            if (requestURI.equals(path) || requestURI.matches(path)) {
                return true;
            }
        }
        return false;
    }
    @Override
    public void doFilter(jakarta.servlet.ServletRequest servletRequest, jakarta.servlet.ServletResponse servletResponse, jakarta.servlet.FilterChain filterChain) throws IOException, jakarta.servlet.ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        String requestURI = httpRequest.getRequestURI();

        String requestId = "";
        requestId = httpRequest.getHeader("x-requestId");
        if (null == requestId || requestId.isEmpty()){
            requestId = UUID.randomUUID().toString();
        }

        MDC.put("requestId", requestId);


        boolean isExcluded = isExcludedPath(httpRequest);



        if (isExcluded) {
            // If the request URI matches the excluded context, bypass authentication
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        String authHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        String publicKeyString = env.getProperty("PUBLIC_KEY_BASE64");
        log.info("THE PUBLICKEY::{}", publicKeyString);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // Remove "Bearer " prefix
            try {
                log.info("THE TOKEN::{}", token);

                byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);


                // Create PublicKey object from the decoded bytes
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey publicKey = keyFactory.generatePublic(keySpec);

                // Parse and validate the token
                Jws<Claims> claimsJws = Jwts.parser().setSigningKey(publicKey).parseClaimsJws(token);
                Claims claims = claimsJws.getBody();
                System.out.println(true);

                String email = claims.get("email", String.class);
                String userId = claims.get("userId", String.class);
                httpRequest.setAttribute("email", email);
                httpRequest.setAttribute("userId", userId);

                // Add CORS headers for allowed origins
                httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");
                httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Origin, X-Requested-With, Content-Type, Accept, Authorization, x-affiliate-code");
                httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");

                // Security headers
                httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
                httpResponse.setHeader("X-Content-Type-Options", "nosniff");
                httpResponse.setHeader("X-Frame-Options", "DENY");
                httpResponse.setHeader("Strict-Transport-Security", "max-age=2592000; includeSubDomains; preload");
                httpResponse.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
                httpResponse.setHeader("Pragma", "no-cache");
                httpResponse.setHeader("Content-Security-Policy", "default-src 'none'");
                httpResponse.setHeader("Permissions-Policy", "geolocation=(self), microphone=(), camera=()");
                httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                filterChain.doFilter(servletRequest, servletResponse); // Continue with the request
            } catch (Exception e) {
                // Send a JSON response with a 401 status and "Invalid Token" message
                httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
                httpResponse.setContentType("application/json");
                PrintWriter out = httpResponse.getWriter();
                out.println("{\"statusCode\": 401, \"message\": \"Invalid Token\"}");
            }finally {
                MDC.clear();
            }
        } else {
            // Send a JSON response with a 401 status and "Authorization token must be provided" message
            httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            httpResponse.setContentType("application/json");
            PrintWriter out = httpResponse.getWriter();
            out.println("{\"statusCode\": 401, \"message\": \"Authorization token must be provided in the 'Authorization' header with the 'Bearer' prefix\"}");
        }
    }

    @Override
    public void destroy() {
        log.debug("destroy() method is invoked");
    }

    private Key decodePublicKey(String publicKeyBase64) {
        byte[] decodedBytes = Base64.getDecoder().decode(publicKeyBase64);
        return Keys.hmacShaKeyFor(decodedBytes);
    }
}
