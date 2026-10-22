package com.payroll.aj.security.scope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative RBAC annotation enforcing country-level data boundaries.
 *
 * <p>When placed on a service or controller method, the
 * {@link DataScopeAspect} verifies that the authenticated user's
 * {@code countryIds} set contains the country ID resolved from the
 * named method parameter. {@code SUPER_ADMIN} users bypass this check.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * @RequireCountryAccess("countryId")
 * public CountryDto getCountry(Long countryId) { ... }
 * }</pre>
 *
 * @see RequireCompanyAccess
 * @see DataScopeAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireCountryAccess {

    /**
     * Name of the method parameter holding the country ID to verify.
     * Defaults to {@code "countryId"}.
     */
    String value() default "countryId";
}
