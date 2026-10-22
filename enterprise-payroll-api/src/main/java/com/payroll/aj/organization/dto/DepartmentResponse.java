package com.payroll.aj.organization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payroll.aj.organization.entity.Department;

/**
 * Projection of a {@link Department} for organization APIs.
 */
public record DepartmentResponse(
        Long id,
        String name,
        String code,
        @JsonProperty("is_active")
        Boolean isActive
) {
    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getCode(),
                department.getIsActive()
        );
    }
}
