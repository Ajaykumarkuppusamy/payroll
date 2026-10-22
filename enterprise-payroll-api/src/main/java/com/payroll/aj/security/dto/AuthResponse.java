package com.payroll.aj.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * JWT authentication response returned after successful login.
 *
 * <p>Contains the signed token, its metadata, and the caller's full
 * RBAC scope (allowed country and company IDs) so the frontend can
 * pre-filter navigation and UI elements without additional API calls.</p>
 *
 * <p>Field names use {@code @JsonProperty} to produce snake_case JSON
 * keys while keeping Java-idiomatic camelCase in source.</p>
 *
 * @param jwtToken          signed JWT string
 * @param tokenType         always {@code "Bearer"}
 * @param expiresIn         token validity in seconds
 * @param userId            authenticated user's surrogate key
 * @param email             authenticated user's email
 * @param role              the {@link com.payroll.aj.common.enums.UserRole} name
 * @param allowedCountryIds country IDs the user may access
 * @param allowedCompanyIds company IDs the user may access
 */
public record AuthResponse(
        @JsonProperty("jwt_token")
        String jwtToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("expires_in")
        long expiresIn,

        @JsonProperty("user_id")
        Long userId,

        String email,

        String role,

        @JsonProperty("allowed_country_ids")
        List<Long> allowedCountryIds,

        @JsonProperty("allowed_company_ids")
        List<Long> allowedCompanyIds
) {
}
