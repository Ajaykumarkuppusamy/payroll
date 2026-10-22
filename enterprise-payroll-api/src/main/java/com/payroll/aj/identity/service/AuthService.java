package com.payroll.aj.identity.service;

import com.payroll.aj.security.dto.AuthResponse;
import com.payroll.aj.security.dto.LoginRequest;
import com.payroll.aj.security.jwt.JwtService;
import com.payroll.aj.security.userdetails.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Authentication service orchestrating the login flow.
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li>Delegates credential verification to Spring Security's
 *       {@link AuthenticationManager}, which invokes
 *       {@link com.payroll.aj.security.userdetails.AppUserDetailsService}
 *       and the {@link org.springframework.security.crypto.password.PasswordEncoder}.</li>
 *   <li>On success, generates a stateless JWT embedding the user's
 *       identity and RBAC scope via {@link JwtService}.</li>
 *   <li>Returns an {@link AuthResponse} record containing the token
 *       and the full scope metadata for the frontend.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Authenticates the operator and issues a scoped JWT.
     *
     * @param request the login credentials
     * @return a response containing the JWT and RBAC scope metadata
     * @throws org.springframework.security.core.AuthenticationException
     *         if credentials are invalid or the account is disabled
     */
    public AuthResponse authenticate(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.login(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs() / 1000,
                principal.getUserId(),
                principal.getEmail(),
                principal.getRole().name(),
                List.copyOf(principal.getCountryIds()),
                List.copyOf(principal.getCompanyIds()));
    }
}
