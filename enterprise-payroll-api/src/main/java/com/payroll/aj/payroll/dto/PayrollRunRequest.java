package com.payroll.aj.payroll.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request payload to trigger a monthly payroll run.
 *
 * @param year  the pay-period year (2000–2100)
 * @param month the pay-period month (1–12)
 */
public record PayrollRunRequest(
        @Min(2000) @Max(2100)
        int year,

        @Min(1) @Max(12)
        int month
) {
}
