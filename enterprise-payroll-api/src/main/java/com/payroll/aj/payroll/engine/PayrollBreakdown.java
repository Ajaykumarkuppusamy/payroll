package com.payroll.aj.payroll.engine;

import java.math.BigDecimal;
import java.util.List;

/**
 * Complete payroll computation result for a single employee.
 *
 * <p>{@code grossPay = baseSalary + sum(earnings)}<br>
 * {@code netPay = grossPay − totalDeductions}</p>
 *
 * <p>All monetary values are {@code BigDecimal} with scale 2 and
 * {@link java.math.RoundingMode#HALF_UP}.</p>
 *
 * @param grossPay        base salary plus all earnings
 * @param totalDeductions sum of all deduction components
 * @param netPay          gross pay minus total deductions
 * @param earnings        itemised earning components
 * @param deductions      itemised deduction components
 */
public record PayrollBreakdown(
        BigDecimal grossPay,
        BigDecimal totalDeductions,
        BigDecimal netPay,
        List<ComponentLineItem> earnings,
        List<ComponentLineItem> deductions
) {
}
