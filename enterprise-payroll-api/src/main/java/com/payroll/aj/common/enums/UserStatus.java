package com.payroll.aj.common.enums;

/**
 * Account states for system operators ({@link com.payroll.aj.identity.entity.AppUser}).
 *
 * <p>Only {@code ACTIVE} users can authenticate and access the platform.
 * {@code LOCKED} accounts require administrative intervention.</p>
 */
public enum UserStatus {

    ACTIVE("Active"),
    INACTIVE("Inactive"),
    LOCKED("Locked"),
    PENDING_ACTIVATION("Pending Activation");

    private final String displayName;

    UserStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
