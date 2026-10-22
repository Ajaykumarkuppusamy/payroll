package com.payroll.aj.common.enums;

/**
 * Determines how a salary component's monetary value is computed.
 *
 * <ul>
 *   <li>{@code FLAT_AMOUNT} — a fixed monetary value
 *       (uses {@code default_value} or {@code override_amount})</li>
 *   <li>{@code PERCENTAGE_OF_BASE} — a percentage of the employee's
 *       {@code base_salary} (the percentage is stored in
 *       {@code default_value}, e.g. {@code 40.00} for 40%)</li>
 *   <li>{@code CUSTOM_FORMULA} — evaluated via a SpEL expression
 *       stored in {@code formula_expression} against a
 *       {@code Map<String, BigDecimal>} context</li>
 * </ul>
 *
 * @see com.payroll.aj.payroll.entity.SalaryComponent
 * @see com.payroll.aj.payroll.engine.SpelFormulaEvaluatorService
 */
public enum CalculationType {

    FLAT_AMOUNT("Flat Amount"),
    PERCENTAGE_OF_BASE("Percentage of Base"),
    CUSTOM_FORMULA("Custom Formula");

    private final String displayName;

    CalculationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
