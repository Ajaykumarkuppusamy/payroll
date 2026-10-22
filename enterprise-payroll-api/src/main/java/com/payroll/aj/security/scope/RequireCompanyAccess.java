package com.payroll.aj.security.scope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative RBAC annotation enforcing company-level data boundaries.
 *
 * <p>When placed on a service or controller method, the
 * {@link DataScopeAspect} verifies that the authenticated user's
 * {@code companyIds} set contains the company ID resolved from the
 * named method parameter. {@code SUPER_ADMIN} users bypass this check.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * @RequireCompanyAccess("companyId")
 * public CompanyDto getCompany(Long companyId) { ... }
 * }</pre>
 *
 * @see RequireCountryAccess
 * @see DataScopeAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireCompanyAccess {

    /**
     * Name of the method parameter holding the company ID to verify.
     * Defaults to {@code "companyId"}.
     */
    String value() default "companyId";
}
