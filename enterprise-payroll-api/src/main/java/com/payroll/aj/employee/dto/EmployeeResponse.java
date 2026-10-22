package com.payroll.aj.employee.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payroll.aj.employee.entity.Employee;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read-only projection of an {@link Employee} for the HR Directory endpoint.
 *
 * <p>Exposes only non-sensitive HR fields. Bank account details, tax IDs,
 * and IFSC codes are intentionally excluded and served through a separate
 * employee-detail endpoint (future phase).</p>
 *
 * <p>Field names use {@code @JsonProperty} to produce snake_case JSON keys
 * consistent with the project's API serialisation contract.</p>
 *
 * @param id                 surrogate PK
 * @param employeeCode       human-readable identifier (e.g. {@code EMP-001})
 * @param firstName          given name
 * @param lastName           family name
 * @param email              corporate email address
 * @param baseSalary         gross base salary (NUMERIC 19,2)
 * @param employmentStatus   lifecycle state ({@code ACTIVE}, {@code ON_LEAVE}, etc.)
 * @param hireDate           ISO-8601 date of hire
 * @param departmentName     name of the employee's department (eagerly joined)
 */
public record EmployeeResponse(
        Long id,

        @JsonProperty("employee_code")
        String employeeCode,

        @JsonProperty("first_name")
        String firstName,

        @JsonProperty("last_name")
        String lastName,

        String email,

        @JsonProperty("base_salary")
        BigDecimal baseSalary,

        @JsonProperty("employment_status")
        String employmentStatus,

        @JsonProperty("hire_date")
        LocalDate hireDate,

        @JsonProperty("department_name")
        String departmentName
) {

    /**
     * Factory method mapping from the JPA entity to the response DTO.
     *
     * <p>Requires the {@code department} association to be initialised
     * (use {@code JOIN FETCH} in the repository query to avoid N+1).</p>
     *
     * @param employee the source entity (department must be loaded)
     * @return the mapped {@link EmployeeResponse}
     */
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getBaseSalary(),
                employee.getEmploymentStatus().name(),
                employee.getHireDate(),
                employee.getDepartment().getName()
        );
    }
}
