package com.payroll.aj.common.enums;

/**
 * Classifies a salary component as either an earning or a deduction.
 *
 * <p>Earnings are added to the employee's gross pay; deductions
 * are subtracted to arrive at net pay.</p>
 *
 * @see com.payroll.aj.payroll.entity.SalaryComponent
 */
public enum ComponentType {

    EARNING("Earning"),
    DEDUCTION("Deduction");

    private final String displayName;

    ComponentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
