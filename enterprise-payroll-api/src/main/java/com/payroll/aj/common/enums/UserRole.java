package com.payroll.aj.common.enums;

/**
 * System operator roles for RBAC data-boundary enforcement.
 *
 * <ul>
 *   <li>{@code SUPER_ADMIN} — unrestricted tenant-wide access</li>
 *   <li>{@code COUNTRY_PAYROLL_MGR} — scoped to assigned countries</li>
 *   <li>{@code COMPANY_HR} — scoped to assigned companies</li>
 * </ul>
 *
 * @see com.payroll.aj.identity.entity.AppUser
 */
public enum UserRole {

    SUPER_ADMIN("Super Admin"),
    COUNTRY_PAYROLL_MGR("Country Payroll Manager"),
    COMPANY_HR("Company HR");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
