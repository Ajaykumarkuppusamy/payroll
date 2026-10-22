package com.payroll.aj.security.scope;

import com.payroll.aj.common.enums.UserRole;
import com.payroll.aj.security.userdetails.UserPrincipal;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * AOP aspect enforcing RBAC data boundaries per ARCHITECTURE_SPEC §3.
 *
 * <p>Intercepts methods annotated with {@link RequireCountryAccess}
 * or {@link RequireCompanyAccess} and verifies that the authenticated
 * {@link UserPrincipal}'s scope includes the requested entity ID.
 * Users with role {@code SUPER_ADMIN} bypass all boundary checks.</p>
 *
 * <h3>Parameter resolution</h3>
 * <p>The aspect resolves the target ID from a named method parameter.
 * This relies on the {@code -parameters} compiler flag, which Spring
 * Boot's parent POM enables by default since 3.0.</p>
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * @RequireCompanyAccess("companyId")
 * public PayrollResult runPayroll(Long companyId, int year, int month) {
 *     // only reachable if user has access to companyId
 * }
 * }</pre>
 */
@Aspect
@Component
public class DataScopeAspect {

    /**
     * Enforces country-level access before method execution.
     */
    @Before("@annotation(access)")
    public void enforceCountryAccess(JoinPoint joinPoint, RequireCountryAccess access) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        if (principal.getRole() == UserRole.SUPER_ADMIN) {
            return;
        }

        Long countryId = extractParameter(joinPoint, access.value(), Long.class);
        if (!principal.getCountryIds().contains(countryId)) {
            throw new AccessDeniedException(
                    "User does not have access to country ID: " + countryId);
        }
    }

    /**
     * Enforces company-level access before method execution.
     */
    @Before("@annotation(access)")
    public void enforceCompanyAccess(JoinPoint joinPoint, RequireCompanyAccess access) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        if (principal.getRole() == UserRole.SUPER_ADMIN) {
            return;
        }

        Long companyId = extractParameter(joinPoint, access.value(), Long.class);
        if (!principal.getCompanyIds().contains(companyId)) {
            throw new AccessDeniedException(
                    "User does not have access to company ID: " + companyId);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────

    /**
     * Extracts the authenticated {@link UserPrincipal} from the
     * {@link SecurityContextHolder}. Uses Java 21 pattern matching
     * for instanceof.
     */
    private UserPrincipal getAuthenticatedPrincipal() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException(
                "No authenticated user in security context");
    }

    /**
     * Resolves a named method parameter from the join point.
     *
     * @param joinPoint the AOP join point
     * @param paramName the parameter name to look up
     * @param type      the expected parameter type
     * @return the resolved parameter value
     * @throws IllegalStateException if the parameter is not found
     */
    private <T> T extractParameter(JoinPoint joinPoint, String paramName, Class<T> type) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(paramName) && type.isInstance(args[i])) {
                return type.cast(args[i]);
            }
        }
        throw new IllegalStateException(
                "Could not resolve parameter '" + paramName + "' of type "
                        + type.getSimpleName() + " on method "
                        + signature.toShortString());
    }
}
