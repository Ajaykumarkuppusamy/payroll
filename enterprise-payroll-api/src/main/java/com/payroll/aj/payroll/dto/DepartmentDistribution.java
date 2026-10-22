package com.payroll.aj.payroll.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Breakdown of employee count per department for dashboard visualisations.
 */
public record DepartmentDistribution(
        @JsonProperty("department_name") String departmentName,
        long count
) {}
