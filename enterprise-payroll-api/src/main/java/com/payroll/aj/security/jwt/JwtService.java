package com.payroll.aj.security.jwt;

import com.payroll.aj.security.userdetails.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

/**
 * Stateless JWT service using JJWT 0.12.x.
 *
 * <p>Tokens embed the following custom claims for fully stateless
 * RBAC enforcement (no database lookup required after login):</p>
 * <ul>
 *   <li>{@code user_id} — surrogate key of the authenticated operator</li>
 *   <li>{@code tenant_id} — the operator's SaaS tenant</li>
 *   <li>{@code email} — operator email for display</li>
 *   <li>{@code role} — {@link com.payroll.aj.common.enums.UserRole} name</li>
 *   <li>{@code country_ids} — allowed country IDs</li>
 *   <li>{@code company_ids} — allowed company IDs</li>
 * </ul>
 *
 * <p>The signing key is a Base64-encoded HMAC-SHA key injected via
 * {@code app.jwt.secret}. Expiration defaults to 24 hours and is
 * configurable via {@code app.jwt.expiration-ms}.</p>
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a signed JWT embedding the user's identity and RBAC scope.
     *
     * @param principal the authenticated operator's principal
     * @return a compact JWS string
     */
    public String generateToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("user_id", principal.getUserId())
                .claim("tenant_id", principal.getTenantId())
                .claim("email", principal.getEmail())
                .claim("role", principal.getRole().name())
                .claim("country_ids", List.copyOf(principal.getCountryIds()))
                .claim("company_ids", List.copyOf(principal.getCompanyIds()))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Parses and verifies a signed JWT, returning the full claims payload.
     *
     * @param token the compact JWS string
     * @return the verified claims
     * @throws JwtException if the token is malformed, expired, or tampered
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validates a token's signature and expiration without throwing.
     *
     * @param token the compact JWS string
     * @return {@code true} if the token is valid and not expired
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Returns the configured token validity in milliseconds.
     * Used by {@link com.payroll.aj.identity.service.AuthService}
     * to populate the {@code expires_in} response field.
     */
    public long getExpirationMs() {
        return expirationMs;
    }
}
