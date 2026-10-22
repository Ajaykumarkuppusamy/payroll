package com.payroll.aj.security.userdetails;

import com.payroll.aj.common.enums.UserRole;
import com.payroll.aj.common.enums.UserStatus;
import com.payroll.aj.identity.entity.AppUser;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom {@link UserDetails} implementation carrying RBAC scope.
 *
 * <p>This principal is created in two contexts:</p>
 * <ol>
 *   <li><strong>Login flow</strong> — constructed from a hydrated
 *       {@link AppUser} entity plus the access-scoping ID sets
 *       queried from {@code user_country_access} and
 *       {@code user_company_access}.</li>
 *   <li><strong>Stateless JWT filter</strong> — reconstructed from
 *       JWT custom claims via {@link #fromClaims(Claims)}, so no
 *       database hit is required on subsequent requests.</li>
 * </ol>
 *
 * <p>The {@code countryIds} and {@code companyIds} sets are used by
 * {@link com.payroll.aj.security.scope.DataScopeAspect} to enforce
 * data boundaries at the service layer.</p>
 */
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final Long tenantId;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final UserStatus status;
    private final Set<Long> countryIds;
    private final Set<Long> companyIds;

    /**
     * Constructs a principal from a fully loaded {@link AppUser} entity
     * and the user's RBAC scope sets. Used during authentication.
     */
    public UserPrincipal(AppUser user, Set<Long> countryIds, Set<Long> companyIds) {
        this.userId = user.getId();
        this.tenantId = user.getTenant().getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.status = user.getStatus();
        this.countryIds = Collections.unmodifiableSet(countryIds);
        this.companyIds = Collections.unmodifiableSet(companyIds);
    }

    /**
     * Private all-args constructor used by {@link #fromClaims(Claims)}.
     */
    private UserPrincipal(Long userId, Long tenantId, String username, String email,
                          String passwordHash, UserRole role, UserStatus status,
                          Set<Long> countryIds, Set<Long> companyIds) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.countryIds = countryIds;
        this.companyIds = companyIds;
    }

    /**
     * Reconstructs a {@code UserPrincipal} from JWT custom claims
     * without any database access. Used by the stateless
     * {@link com.payroll.aj.security.jwt.JwtAuthenticationFilter}.
     *
     * <p>JJWT deserialises numeric arrays as {@code List<Number>};
     * we convert each element via {@link Number#longValue()}.</p>
     *
     * @param claims the verified JWT payload
     * @return a fully populated principal
     */
    @SuppressWarnings("unchecked")
    public static UserPrincipal fromClaims(Claims claims) {
        Long userId = claims.get("user_id", Long.class);
        Long tenantId = claims.get("tenant_id", Long.class);
        String username = claims.getSubject();
        String email = claims.get("email", String.class);
        UserRole role = UserRole.valueOf(claims.get("role", String.class));

        List<Number> rawCountryIds = claims.get("country_ids", List.class);
        List<Number> rawCompanyIds = claims.get("company_ids", List.class);

        Set<Long> countryIds = rawCountryIds == null
                ? Set.of()
                : rawCountryIds.stream()
                        .map(Number::longValue)
                        .collect(Collectors.toUnmodifiableSet());

        Set<Long> companyIds = rawCompanyIds == null
                ? Set.of()
                : rawCompanyIds.stream()
                        .map(Number::longValue)
                        .collect(Collectors.toUnmodifiableSet());

        return new UserPrincipal(userId, tenantId, username, email,
                null, role, UserStatus.ACTIVE, countryIds, companyIds);
    }

    // ── Spring Security UserDetails contract ──────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return status != UserStatus.INACTIVE;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }

    // ── Domain-specific accessors ─────────────────────────────────

    public Long getUserId() {
        return userId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public Set<Long> getCountryIds() {
        return countryIds;
    }

    public Set<Long> getCompanyIds() {
        return companyIds;
    }
}
