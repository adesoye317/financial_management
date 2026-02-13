package com.financal.mgt.Financal.Management.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private Environment env;
    private final JwtUtil jwtUtil;

    private static final AntPathMatcher matcher = new AntPathMatcher();
    private static final String BEARER_PREFIX = "Bearer ";

    // Public endpoints that don't require JWT authentication
    private static final String[] PUBLIC_ENDPOINTS = {
            "/xpenskey/api/auth/login",
            "/xpenskey/api/auth/signup",
            "/xpenskey/api/auth/verify-otp",
            "/xpenskey/api/auth/refresh-token",
            "/api/auth/doc/**",
            "/xpenskey/actuator",
            "/xpenskey/actuator/**",
            "/error"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // Don't apply this filter to public endpoints
        for (String path : PUBLIC_ENDPOINTS) {
            if (matcher.match(path, uri)) {
                log.debug("✅ Filter bypassed for public endpoint: {}", uri);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Always apply security headers first
        addSecurityHeaders(response);

        // Validate Authorization header
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("⚠️ Missing or invalid Authorization header for {}", uri);
            sendUnauthorized(response, "Missing or invalid token");
            return;
        }

        try {
            String token = authHeader.substring(7);

            log.debug("🔍 Validating JWT token for {}", uri);

            // ✅ Validate token using your existing JwtUtil
            if (!jwtUtil.validateToken(token)) {
                log.warn("⚠️ Invalid or expired JWT token for {}", uri);
                sendUnauthorized(response, "Invalid or expired token");
                return;
            }

            // ✅ Extract email from validated token
            String email = jwtUtil.extractEmail(token);

            if (email == null || email.isEmpty()) {
                log.warn("⚠️ No email found in JWT token for {}", uri);
                sendUnauthorized(response, "Invalid token claims");
                return;
            }

            log.info("✅ Valid token for user: {} accessing {}", email, uri);

            // ✅ Set authentication in Spring Security context
            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,  // Principal (user identifier)
                                null,   // Credentials (not needed after authentication)
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("✅ Authentication set in SecurityContext for user: {}", email);
            }

        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            log.warn("⚠️ JWT token expired for {}: {}", uri, ex.getMessage());
            sendUnauthorized(response, "Token expired");
            return;
        } catch (io.jsonwebtoken.MalformedJwtException ex) {
            log.warn("⚠️ Malformed JWT token for {}: {}", uri, ex.getMessage());
            sendUnauthorized(response, "Malformed token");
            return;
        } catch (io.jsonwebtoken.SignatureException ex) {
            log.warn("⚠️ Invalid JWT signature for {}: {}", uri, ex.getMessage());
            sendUnauthorized(response, "Invalid token signature");
            return;
        } catch (Exception ex) {
            log.error("❌ JWT validation failed for {}: {}", uri, ex.getMessage(), ex);
            sendUnauthorized(response, "Invalid token");
            return;
        }

        // ✅ Continue to the next filter in the chain
        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"statusCode\":401, \"message\":\"" + message + "\"}");
    }

    private void addSecurityHeaders(HttpServletResponse response) {
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }
}
