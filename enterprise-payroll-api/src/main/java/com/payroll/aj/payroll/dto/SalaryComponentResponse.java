package com.payroll.aj.payroll.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payroll.aj.common.enums.CalculationType;
import com.payroll.aj.common.enums.ComponentType;
import com.payroll.aj.payroll.entity.SalaryComponent;

import java.math.BigDecimal;

/**
 * DTO projection for {@link SalaryComponent}.
 */
public record SalaryComponentResponse(
        Long id,
        String code,
        String name,
        ComponentType type,
        @JsonProperty("calculation_type")
        CalculationType calculationType,
        @JsonProperty("formula_expression")
        String formulaExpression,
        @JsonProperty("default_value")
        BigDecimal defaultValue,
        @JsonProperty("is_taxable")
        Boolean isTaxable,
        @JsonProperty("is_statutory")
        Boolean isStatutory,
        @JsonProperty("is_active")
        Boolean isActive
) {
    public static SalaryComponentResponse from(SalaryComponent sc) {
        return new SalaryComponentResponse(
                sc.getId(),
                sc.getCode(),
                sc.getName(),
                sc.getType(),
                sc.getCalculationType(),
                sc.getFormulaExpression(),
                sc.getDefaultValue(),
                sc.getIsTaxable(),
                sc.getIsStatutory(),
                sc.getIsActive()
        );
    }
}
