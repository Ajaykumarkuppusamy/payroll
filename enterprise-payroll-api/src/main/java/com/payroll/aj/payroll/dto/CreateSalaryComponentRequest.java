package com.payroll.aj.payroll.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payroll.aj.common.enums.CalculationType;
import com.payroll.aj.common.enums.ComponentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload for creating a new salary component template.
 */
public record CreateSalaryComponentRequest(
        @NotBlank(message = "Component code is required")
        @Size(max = 50)
        String code,

        @NotBlank(message = "Component name is required")
        @Size(max = 255)
        String name,

        @NotNull(message = "Component type is required")
        ComponentType type,

        @JsonProperty("calculation_type")
        @NotNull(message = "Calculation type is required")
        CalculationType calculationType,

        @JsonProperty("formula_expression")
        String formulaExpression,

        @JsonProperty("default_value")
        BigDecimal defaultValue,

        @JsonProperty("is_taxable")
        Boolean isTaxable,

        @JsonProperty("is_statutory")
        Boolean isStatutory
) {}
