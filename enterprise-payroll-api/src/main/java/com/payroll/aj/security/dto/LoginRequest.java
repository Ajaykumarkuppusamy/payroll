package com.payroll.aj.security.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Authentication request payload for {@code POST /api/v1/auth/login}.
 *
 * <p>The {@code login} field accepts either an email address or a
 * username, allowing flexible operator authentication.</p>
 *
 * @param login    email address or username of the system operator
 * @param password plaintext password to verify against the stored BCrypt hash
 */
public record LoginRequest(
        @NotBlank(message = "Login is required")
        String login,

        @NotBlank(message = "Password is required")
        String password
) {
}
