package com.payroll.aj.payroll.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payroll.aj.employee.dto.EmployeeResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated analytics and metrics payload for the Company Dashboard.
 */
public record DashboardResponse(
        @JsonProperty("company_id") Long companyId,
        @JsonProperty("total_employees") long totalEmployees,
        @JsonProperty("active_employees") long activeEmployees,
        @JsonProperty("on_leave_employees") long onLeaveEmployees,
        @JsonProperty("probation_employees") long probationEmployees,
        @JsonProperty("monthly_payroll_estimate") BigDecimal monthlyPayrollEstimate,
        @JsonProperty("avg_salary") BigDecimal avgSalary,
        @JsonProperty("last_batch") PayrollBatchResponse lastBatch,
        @JsonProperty("department_distribution") List<DepartmentDistribution> departmentDistribution,
        @JsonProperty("recent_employees") List<EmployeeResponse> recentEmployees
) {}
