package com.payroll.aj.common.enums;

/**
 * Lifecycle states of an {@link com.payroll.aj.employee.entity.Employee}.
 *
 * <p>Only {@code ACTIVE} employees are eligible for payroll calculation.
 * The payroll engine filters on this status during batch execution.</p>
 */
public enum EmploymentStatus {

    ACTIVE("Active"),
    ON_LEAVE("On Leave"),
    PROBATION("Probation"),
    TERMINATED("Terminated"),
    RESIGNED("Resigned");

    private final String displayName;

    EmploymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
