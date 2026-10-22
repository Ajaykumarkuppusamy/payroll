package com.payroll.aj.payroll.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Single component line-item detail within a payroll record.
 */
public record PayrollRecordDetailResponse(
        @JsonProperty("component_code") String componentCode,
        @JsonProperty("component_name") String componentName,
        @JsonProperty("component_type") String componentType,
        BigDecimal amount
) {
}
