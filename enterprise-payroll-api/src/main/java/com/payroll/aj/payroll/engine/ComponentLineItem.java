package com.payroll.aj.payroll.engine;

import com.payroll.aj.common.enums.ComponentType;

import java.math.BigDecimal;

/**
 * A single line item in a payroll breakdown representing one
 * resolved salary component.
 *
 * @param componentCode the component's code (e.g., {@code EAR-IND-HRA01})
 * @param componentName the component's display name (e.g., "House Rent Allowance")
 * @param type          earning or deduction
 * @param amount        the computed monetary value (scale 2, HALF_UP)
 */
public record ComponentLineItem(
        String componentCode,
        String componentName,
        ComponentType type,
        BigDecimal amount
) {
}
