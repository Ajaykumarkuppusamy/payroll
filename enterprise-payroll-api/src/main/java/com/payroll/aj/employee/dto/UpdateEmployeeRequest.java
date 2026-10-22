package com.payroll.aj.employee.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payroll.aj.common.enums.EmploymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for updating an existing employee.
 */
public record UpdateEmployeeRequest(
        @JsonProperty("department_id")
        @NotNull(message = "Department ID is required")
        Long departmentId,

        @JsonProperty("first_name")
        @NotBlank(message = "First name is required")
        @Size(max = 100)
        String firstName,

        @JsonProperty("last_name")
        @NotBlank(message = "Last name is required")
        @Size(max = 100)
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255)
        String email,

        @JsonProperty("base_salary")
        @NotNull(message = "Base salary is required")
        @DecimalMin(value = "0.01", message = "Base salary must be positive")
        BigDecimal baseSalary,

        @JsonProperty("employment_status")
        @NotNull(message = "Employment status is required")
        EmploymentStatus employmentStatus,

        @JsonProperty("hire_date")
        @NotNull(message = "Hire date is required")
        LocalDate hireDate,

        @JsonProperty("termination_date")
        LocalDate terminationDate,

        @JsonProperty("bank_account_number")
        String bankAccountNumber,

        @JsonProperty("bank_name")
        String bankName,

        @JsonProperty("bank_ifsc_code")
        String bankIfscCode,

        @JsonProperty("tax_id")
        String taxId
) {}
