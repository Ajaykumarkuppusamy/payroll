package com.payroll.aj.identity.controller;

import com.payroll.aj.identity.service.AuthService;
import com.payroll.aj.security.dto.AuthResponse;
import com.payroll.aj.security.dto.LoginRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication endpoint.
 *
 * <p>Permitted without a JWT via the
 * {@link com.payroll.aj.security.config.SecurityConfig} filter chain
 * ({@code /api/v1/auth/**} is public).</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a system operator and returns a scoped JWT.
     *
     * <p><strong>POST</strong> {@code /api/v1/auth/login}</p>
     *
     * @param request the login credentials (username/email + password)
     * @return 200 OK with {@link AuthResponse} on success;
     *         401 Unauthorized on invalid credentials
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }
}
